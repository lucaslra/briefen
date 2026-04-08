package com.briefen.service;

import com.briefen.exception.ArticleExtractionException;
import com.briefen.exception.ArticleFetchException;
import com.briefen.validation.UrlValidator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
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

    @Test
    void shouldExtractTextFromPdf() throws Exception {
        // Arrange — build a minimal PDF in memory using PDFBox
        String expectedText = "Criteria for modularization. ".repeat(10);
        byte[] pdfBytes = buildPdf("Criteria for Modularization", expectedText);

        wireMock.stubFor(get(urlEqualTo("/paper.pdf"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/pdf")
                        .withBody(pdfBytes)));

        // Act
        ArticleFetcherService.ArticleContent content =
                articleFetcherService.fetch("http://localhost:" + wireMock.port() + "/paper.pdf");

        // Assert
        assertThat(content.title()).isEqualTo("Criteria for Modularization");
        assertThat(content.text()).contains("modularization");
    }

    @Test
    void shouldUseFilenameAsTitleWhenPdfHasNoMetadataTitle() throws Exception {
        // Arrange — PDF with no title in document information
        byte[] pdfBytes = buildPdf(null, "Some PDF content about software engineering. ".repeat(5));

        wireMock.stubFor(get(urlEqualTo("/criteria_for_modularization.pdf"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/pdf")
                        .withBody(pdfBytes)));

        // Act
        ArticleFetcherService.ArticleContent content = articleFetcherService
                .fetch("http://localhost:" + wireMock.port() + "/criteria_for_modularization.pdf");

        // Assert — filename with underscores replaced by spaces, .pdf stripped
        assertThat(content.title()).isEqualTo("criteria for modularization");
    }

    @Test
    void shouldThrowExtractionExceptionForEmptyPdf() throws Exception {
        // Arrange — valid PDF structure but no text content
        byte[] pdfBytes = buildPdf("Empty", "");

        wireMock.stubFor(get(urlEqualTo("/empty.pdf"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/pdf")
                        .withBody(pdfBytes)));

        // Act & Assert
        assertThatThrownBy(() -> articleFetcherService.fetch("http://localhost:" + wireMock.port() + "/empty.pdf"))
                .isInstanceOf(ArticleExtractionException.class)
                .hasMessageContaining("too short");
    }

    /** Creates a minimal single-page PDF with the given title (metadata) and body text. */
    private byte[] buildPdf(String title, String bodyText) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (title != null) {
                doc.getDocumentInformation().setTitle(title);
            }
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            if (bodyText != null && !bodyText.isBlank()) {
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 700);
                    // PDFBox can't write very long strings in one call — split into lines
                    int chunkSize = 80;
                    for (int i = 0; i < bodyText.length(); i += chunkSize) {
                        cs.showText(bodyText.substring(i, Math.min(i + chunkSize, bodyText.length())));
                        cs.newLineAtOffset(0, -15);
                    }
                    cs.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
