package com.briefen.service;

import com.briefen.exception.ArticleExtractionException;
import com.briefen.exception.ArticleFetchException;
import com.briefen.validation.UrlValidator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ArticleFetcherServiceTest {

    private WireMockServer wireMock;
    private ArticleFetcherService articleFetcherService;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        // Use a no-op UrlValidator mock so the DNS rebinding check does not block
        // WireMock's localhost test server (127.0.0.1 is a loopback address).
        articleFetcherService = new ArticleFetcherService(Duration.ofSeconds(5), mock(UrlValidator.class));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldExtractTitleAndTextFromSimpleHtmlArticle() {
        // Arrange
        String html = """
                <html>
                  <head><title>Page Title</title></head>
                  <body>
                    <article>
                      <h1>My Article Heading</h1>
                      <p>%s</p>
                    </article>
                  </body>
                </html>
                """.formatted("Spring Boot is a framework for building production-ready Java applications. ".repeat(5));

        wireMock.stubFor(get(urlEqualTo("/article"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        // Act
        ArticleFetcherService.ArticleContent content = articleFetcherService.fetch("http://localhost:" + wireMock.port() + "/article");

        // Assert
        assertThat(content.title()).isEqualTo("My Article Heading");
        assertThat(content.text()).contains("Spring Boot");
    }

    @Test
    void shouldFallBackToMainElementWhenNoArticle() {
        // Arrange
        String html = """
                <html>
                  <head><title>Page Title</title></head>
                  <body>
                    <main>
                      <h1>Main Content Heading</h1>
                      <p>%s</p>
                    </main>
                  </body>
                </html>
                """.formatted("This is a very long paragraph about Java and Spring Boot development practices. ".repeat(5));

        wireMock.stubFor(get(urlEqualTo("/main-content"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        // Act
        ArticleFetcherService.ArticleContent content = articleFetcherService.fetch("http://localhost:" + wireMock.port() + "/main-content");

        // Assert
        assertThat(content.title()).isEqualTo("Main Content Heading");
        assertThat(content.text()).contains("Java and Spring Boot");
    }

    @Test
    void shouldThrowExtractionExceptionWhenContentTooShort() {
        // Arrange — only 10 chars of actual content, well below the 100-char minimum
        String html = """
                <html>
                  <body>
                    <article><p>Short.</p></article>
                  </body>
                </html>
                """;

        wireMock.stubFor(get(urlEqualTo("/short"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        // Act & Assert
        assertThatThrownBy(() -> articleFetcherService.fetch("http://localhost:" + wireMock.port() + "/short"))
                .isInstanceOf(ArticleExtractionException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void shouldThrowArticleFetchExceptionOnHttpError() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo("/not-found"))
                .willReturn(aResponse().withStatus(404)));

        // Act & Assert
        assertThatThrownBy(() -> articleFetcherService.fetch("http://localhost:" + wireMock.port() + "/not-found"))
                .isInstanceOf(ArticleFetchException.class);
    }

    @Test
    void shouldThrowArticleFetchExceptionOn403() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo("/blocked"))
                .willReturn(aResponse().withStatus(403)));

        // Act & Assert
        assertThatThrownBy(() -> articleFetcherService.fetch("http://localhost:" + wireMock.port() + "/blocked"))
                .isInstanceOf(ArticleFetchException.class)
                .hasMessageContaining("bot protection");
    }
}
