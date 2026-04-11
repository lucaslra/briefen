package com.briefen.service;

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

class OpenAiSummarizerServiceTest {

    private WireMockServer wireMock;
    private OpenAiSummarizerService service;

    private static final String ARTICLE_TEXT = "This is an article about Java and Spring Boot. ".repeat(10);
    private static final String API_KEY = "sk-test-api-key";

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        // Force HTTP/1.1 — WireMock's default listener does not perform an H2 upgrade
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .requestFactory(factory)
                .build();

        service = new OpenAiSummarizerService(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldReturnSummaryFromOpenAiResponse() {
        // Arrange
        String responseBody = """
                {
                  "id": "chatcmpl-test",
                  "choices": [{
                    "message": {"role": "assistant", "content": "# Test Title\\n\\nThis is the OpenAI summary."},
                    "finish_reason": "stop"
                  }]
                }
                """;

        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act
        String result = service.summarize(ARTICLE_TEXT, null, "gpt-4o", API_KEY, null, null);

        // Assert
        assertThat(result).contains("This is the OpenAI summary.");
    }

    @Test
    void shouldThrowOnUnauthorizedResponse() {
        // Arrange
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"Invalid API key\"}}")));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "gpt-4o", "bad-key", null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("Invalid OpenAI API key")
                .satisfies(ex -> assertThat(((SummarizationException) ex).isTimeout()).isFalse());
    }

    @Test
    void shouldThrowWithTimeoutFlagOnRateLimitResponse() {
        // Arrange
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"Rate limit exceeded\"}}")));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "gpt-4o", API_KEY, null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("rate limit")
                .satisfies(ex -> assertThat(((SummarizationException) ex).isTimeout()).isTrue());
    }

    @Test
    void shouldPassAuthorizationHeader() {
        // Arrange
        String responseBody = """
                {
                  "choices": [{
                    "message": {"role": "assistant", "content": "Summary content here."},
                    "finish_reason": "stop"
                  }]
                }
                """;

        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act
        service.summarize(ARTICLE_TEXT, null, "gpt-4o", API_KEY, null, null);

        // Assert — Authorization header was sent
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer " + API_KEY)));
    }

    @Test
    void shouldThrowSummarizationExceptionWhenChoicesEmpty() {
        // Arrange
        String responseBody = """
                {
                  "choices": []
                }
                """;
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "gpt-4o", API_KEY, null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("no choices");
    }
}
