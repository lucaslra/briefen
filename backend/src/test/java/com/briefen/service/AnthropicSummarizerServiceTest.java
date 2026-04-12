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

class AnthropicSummarizerServiceTest {

    private WireMockServer wireMock;
    private AnthropicSummarizerService service;

    private static final String ARTICLE_TEXT = "This is an article about Java and Spring Boot. ".repeat(10);
    private static final String API_KEY = "sk-ant-test-api-key";

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

        service = new AnthropicSummarizerService(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldReturnSummaryFromAnthropicResponse() {
        // Arrange
        String responseBody = """
                {
                  "id": "msg-test",
                  "type": "message",
                  "role": "assistant",
                  "content": [{
                    "type": "text",
                    "text": "# Test Title\\n\\nThis is the Anthropic summary."
                  }],
                  "stop_reason": "end_turn"
                }
                """;

        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act
        String result = service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", API_KEY, null, null);

        // Assert
        assertThat(result).contains("This is the Anthropic summary.");
    }

    @Test
    void shouldThrowOnUnauthorizedResponse() {
        // Arrange
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"Invalid API key\"}}")));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", "bad-key", null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("Invalid Anthropic API key")
                .satisfies(ex -> assertThat(((SummarizationException) ex).isTimeout()).isFalse());
    }

    @Test
    void shouldThrowWithTimeoutFlagOnRateLimitResponse() {
        // Arrange
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"Rate limit exceeded\"}}")));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", API_KEY, null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("rate limit")
                .satisfies(ex -> assertThat(((SummarizationException) ex).isTimeout()).isTrue());
    }

    @Test
    void shouldPassApiKeyAndVersionHeaders() {
        // Arrange
        String responseBody = """
                {
                  "content": [{
                    "type": "text",
                    "text": "Summary content here."
                  }],
                  "stop_reason": "end_turn"
                }
                """;

        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act
        service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", API_KEY, null, null);

        // Assert — x-api-key and anthropic-version headers were sent
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo(API_KEY))
                .withHeader("anthropic-version", equalTo("2023-06-01")));
    }

    @Test
    void shouldThrowWhenContentBlocksAreEmpty() {
        // Arrange
        String responseBody = """
                {
                  "content": [],
                  "stop_reason": "end_turn"
                }
                """;
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", API_KEY, null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("no content");
    }

    @Test
    void shouldThrowWhenResponseHasNoContentKey() {
        // Arrange
        String responseBody = """
                {
                  "id": "msg-test",
                  "type": "message"
                }
                """;
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", API_KEY, null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("empty or invalid response");
    }

    @Test
    void shouldThrowWhenTextBlockIsEmpty() {
        // Arrange
        String responseBody = """
                {
                  "content": [{
                    "type": "text",
                    "text": "   "
                  }],
                  "stop_reason": "max_tokens"
                }
                """;
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", API_KEY, null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("empty summary");
    }

    @Test
    void shouldThrowOnGenericClientError() {
        // Arrange
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"Bad request\"}}")));

        // Act & Assert
        assertThatThrownBy(() -> service.summarize(ARTICLE_TEXT, null, "claude-sonnet-4-20250514", API_KEY, null, null))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("status 400")
                .satisfies(ex -> assertThat(((SummarizationException) ex).isTimeout()).isFalse());
    }
}
