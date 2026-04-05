package com.briefen.service;

import com.briefen.config.OllamaProperties;
import com.briefen.exception.SummarizationException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaSummarizerServiceTest {

    private WireMockServer wireMock;
    private OllamaSummarizerService service;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        OllamaProperties properties = new OllamaProperties(
                "http://localhost:" + wireMock.port(),
                "gemma3:4b",
                Duration.ofSeconds(30)
        );

        // Force HTTP/1.1 — WireMock's default listener does not perform an H2 upgrade
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();

        service = new OllamaSummarizerService(restClient, properties);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldReturnSummaryFromOllamaResponse() {
        // Arrange
        String responseBody = """
                {"model":"gemma3:4b","response":"This is the generated summary text.","done":true}
                """;

        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act
        String summary = service.summarize("Some article text here.", null, "gemma3:4b");

        // Assert
        assertThat(summary).isEqualTo("This is the generated summary text.");
    }

    @Test
    void shouldThrowSummarizationExceptionOnConnectionError() {
        // Arrange — stop the server to force a connection error
        wireMock.stop();

        // Act & Assert
        assertThatThrownBy(() -> service.summarize("Article text.", null, "gemma3:4b"))
                .isInstanceOf(SummarizationException.class)
                .satisfies(ex -> assertThat(((SummarizationException) ex).isTimeout()).isFalse());
    }

    @Test
    void shouldSendFullArticleTextWithoutTruncation() {
        // Arrange — build a string that previously would have been truncated (> 50000 chars)
        String longText = "A".repeat(70_000);

        String responseBody = """
                {"model":"gemma3:4b","response":"Summary of long article.","done":true}
                """;
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act
        String summary = service.summarize(longText, null, "gemma3:4b");

        // Assert — full text was sent, no truncation marker present
        assertThat(summary).isEqualTo("Summary of long article.");
        wireMock.verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withRequestBody(notContaining("[Article truncated for length]")));
    }

    @Test
    void shouldThrowSummarizationExceptionWhenResponseHasNoResponseKey() {
        // Arrange — Ollama returns a valid 200 but without the "response" field
        String responseBody = """
                {"model":"gemma3:4b","done":true}
                """;
        wireMock.stubFor(post(urlEqualTo("/api/generate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize("Article text.", null, "gemma3:4b"))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("empty or invalid");
    }
}
