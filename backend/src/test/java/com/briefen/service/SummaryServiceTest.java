package com.briefen.service;

import com.briefen.config.OllamaProperties;
import com.briefen.exception.InvalidUrlException;
import com.briefen.model.Summary;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.persistence.SummaryPersistence;
import com.briefen.validation.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock private UrlValidator urlValidator;
    @Mock private ArticleFetcherService articleFetcher;
    @Mock private OllamaSummarizerService ollamaSummarizer;
    @Mock private OpenAiSummarizerService openAiSummarizer;
    @Mock private AnthropicSummarizerService anthropicSummarizer;
    @Mock private SummaryPersistence summaryPersistence;
    @Mock private SettingsPersistence settingsPersistence;

    private OllamaProperties ollamaProperties;
    private SummaryService summaryService;

    private static final String ARTICLE_URL = "https://example.com/article";
    private static final String NORMALIZED_URL = "https://example.com/article";
    private static final String ARTICLE_TEXT = "This is the article body content. ".repeat(20);
    private static final String ARTICLE_TITLE = "Example Article Title";
    private static final String SUMMARY_TEXT = "This is a generated summary.";

    @BeforeEach
    void setUp() {
        ollamaProperties = new OllamaProperties("http://localhost:11434", "gemma3:4b", java.time.Duration.ofSeconds(300));
        summaryService = new SummaryService(
                urlValidator, articleFetcher, ollamaSummarizer, openAiSummarizer,
                anthropicSummarizer, summaryPersistence, settingsPersistence, ollamaProperties
        );
    }

    // ---- URL-based summarization: caching behavior ----

    @Test
    void shouldReturnCachedSummaryWhenArticleAlreadyExists() {
        // Arrange
        Summary cached = buildSummary("cached-id", NORMALIZED_URL, "Cached Title", "Cached summary");
        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(summaryPersistence.findByUrl(NORMALIZED_URL)).thenReturn(Optional.of(cached));

        // Act
        Summary result = summaryService.summarize(ARTICLE_URL, false, null, null);

        // Assert
        assertThat(result.getId()).isEqualTo("cached-id");
        assertThat(result.getSummary()).isEqualTo("Cached summary");
        verifyNoInteractions(articleFetcher, ollamaSummarizer, openAiSummarizer);
    }

    @Test
    void shouldBypassCacheWhenRefreshIsTrue() {
        // Arrange
        Summary existing = buildSummary("existing-id", NORMALIZED_URL, ARTICLE_TITLE, "Old summary");
        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(articleFetcher.fetch(NORMALIZED_URL)).thenReturn(new ArticleFetcherService.ArticleContent(ARTICLE_TITLE, ARTICLE_TEXT));
        when(ollamaSummarizer.summarize(anyString(), isNull(), anyString())).thenReturn(SUMMARY_TEXT);
        when(summaryPersistence.findByUrl(NORMALIZED_URL)).thenReturn(Optional.of(existing));
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        summaryService.summarize(ARTICLE_URL, true, null, null);

        // Assert
        verify(articleFetcher).fetch(NORMALIZED_URL);
        verify(ollamaSummarizer).summarize(anyString(), isNull(), anyString());
        verify(summaryPersistence).save(any());
    }

    @Test
    void shouldBypassCacheWhenLengthHintProvided() {
        // Arrange
        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(articleFetcher.fetch(NORMALIZED_URL)).thenReturn(new ArticleFetcherService.ArticleContent(ARTICLE_TITLE, ARTICLE_TEXT));
        when(ollamaSummarizer.summarize(anyString(), eq("shorter"), anyString())).thenReturn(SUMMARY_TEXT);

        // Act
        Summary result = summaryService.summarize(ARTICLE_URL, false, "shorter", null);

        // Assert — cache was NOT checked; fetcher and summarizer were called
        verify(summaryPersistence, never()).findByUrl(any());
        verify(articleFetcher).fetch(NORMALIZED_URL);
        assertThat(result.getSummary()).isEqualTo(SUMMARY_TEXT);
        // Length-adjusted summaries are transient — not saved
        verify(summaryPersistence, never()).save(any());
    }

    @Test
    void shouldFetchAndSummarizeNewArticle() {
        // Arrange
        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(summaryPersistence.findByUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(articleFetcher.fetch(NORMALIZED_URL)).thenReturn(new ArticleFetcherService.ArticleContent(ARTICLE_TITLE, ARTICLE_TEXT));
        when(ollamaSummarizer.summarize(anyString(), isNull(), anyString())).thenReturn(SUMMARY_TEXT);
        when(summaryPersistence.save(any())).thenAnswer(inv -> {
            Summary s = inv.getArgument(0);
            s.setId("new-id");
            return s;
        });

        // Act
        Summary result = summaryService.summarize(ARTICLE_URL, false, null, null);

        // Assert
        assertThat(result.getTitle()).isEqualTo(ARTICLE_TITLE);
        assertThat(result.getSummary()).isEqualTo(SUMMARY_TEXT);
        verify(summaryPersistence).save(any());
    }

    // ---- Model routing ----

    @Test
    void shouldRouteToOllamaForDefaultModel() {
        // Arrange — model=null → falls back to ollamaProperties.model()
        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(summaryPersistence.findByUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(articleFetcher.fetch(NORMALIZED_URL)).thenReturn(new ArticleFetcherService.ArticleContent(ARTICLE_TITLE, ARTICLE_TEXT));
        when(ollamaSummarizer.summarize(anyString(), isNull(), eq("gemma3:4b"))).thenReturn(SUMMARY_TEXT);
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        summaryService.summarize(ARTICLE_URL, false, null, null);

        // Assert
        verify(ollamaSummarizer).summarize(anyString(), isNull(), eq("gemma3:4b"));
        verifyNoInteractions(openAiSummarizer);
    }

    @Test
    void shouldRouteToOpenAiForGptModel() {
        // Arrange
        String apiKey = "sk-test-key";
        UserSettings settings = new UserSettings();
        settings.setOpenaiApiKey(apiKey);

        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(summaryPersistence.findByUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(articleFetcher.fetch(NORMALIZED_URL)).thenReturn(new ArticleFetcherService.ArticleContent(ARTICLE_TITLE, ARTICLE_TEXT));
        when(settingsPersistence.findDefault()).thenReturn(Optional.of(settings));
        when(openAiSummarizer.summarize(anyString(), isNull(), eq("gpt-4o"), eq(apiKey))).thenReturn(SUMMARY_TEXT);
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        summaryService.summarize(ARTICLE_URL, false, null, "gpt-4o");

        // Assert
        verify(openAiSummarizer).summarize(anyString(), isNull(), eq("gpt-4o"), eq(apiKey));
        verifyNoInteractions(ollamaSummarizer);
    }

    @Test
    void shouldRouteToOpenAiForO3Model() {
        // Arrange
        String apiKey = "sk-o3-key";
        UserSettings settings = new UserSettings();
        settings.setOpenaiApiKey(apiKey);

        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(summaryPersistence.findByUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(articleFetcher.fetch(NORMALIZED_URL)).thenReturn(new ArticleFetcherService.ArticleContent(ARTICLE_TITLE, ARTICLE_TEXT));
        when(settingsPersistence.findDefault()).thenReturn(Optional.of(settings));
        when(openAiSummarizer.summarize(anyString(), isNull(), eq("o3-mini"), eq(apiKey))).thenReturn(SUMMARY_TEXT);
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        summaryService.summarize(ARTICLE_URL, false, null, "o3-mini");

        // Assert
        verify(openAiSummarizer).summarize(anyString(), isNull(), eq("o3-mini"), eq(apiKey));
        verifyNoInteractions(ollamaSummarizer);
    }

    @Test
    void shouldRouteToOpenAiForO4Model() {
        // Arrange
        String apiKey = "sk-o4-key";
        UserSettings settings = new UserSettings();
        settings.setOpenaiApiKey(apiKey);

        when(urlValidator.validate(ARTICLE_URL)).thenReturn(URI.create(NORMALIZED_URL));
        when(summaryPersistence.findByUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(articleFetcher.fetch(NORMALIZED_URL)).thenReturn(new ArticleFetcherService.ArticleContent(ARTICLE_TITLE, ARTICLE_TEXT));
        when(settingsPersistence.findDefault()).thenReturn(Optional.of(settings));
        when(openAiSummarizer.summarize(anyString(), isNull(), eq("o4-mini"), eq(apiKey))).thenReturn(SUMMARY_TEXT);
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        summaryService.summarize(ARTICLE_URL, false, null, "o4-mini");

        // Assert
        verify(openAiSummarizer).summarize(anyString(), isNull(), eq("o4-mini"), eq(apiKey));
        verifyNoInteractions(ollamaSummarizer);
    }

    // ---- Text summarization ----

    @Test
    void shouldExtractTitleFromMarkdownHeadingInSummarizeText() {
        // Arrange
        String text = "Some article body content. ".repeat(10);
        String summaryWithHeading = "# My Extracted Title\n\nThis is the summary body.";
        when(ollamaSummarizer.summarize(anyString(), isNull(), anyString())).thenReturn(summaryWithHeading);
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Summary result = summaryService.summarizeText(text, null, null, null, null);

        // Assert
        assertThat(result.getTitle()).isEqualTo("My Extracted Title");
        assertThat(result.getSummary()).isEqualTo("This is the summary body.");
    }

    @Test
    void shouldUseFallbackTitleWhenNoHeadingInSummarizeText() {
        // Arrange
        String text = "Some article body content. ".repeat(10);
        String summaryWithoutHeading = "This is a summary without a markdown heading.";
        when(ollamaSummarizer.summarize(anyString(), isNull(), anyString())).thenReturn(summaryWithoutHeading);
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Summary result = summaryService.summarizeText(text, null, null, null, null);

        // Assert
        assertThat(result.getTitle()).isEqualTo("Pasted Article");
        assertThat(result.getSummary()).isEqualTo(summaryWithoutHeading);
    }

    @Test
    void shouldReturnCachedSummaryForSourceUrlInSummarizeText() {
        // Arrange
        String sourceUrl = "https://example.com/original";
        Summary cached = buildSummary("cached-text-id", sourceUrl, "Cached Text Title", "Cached text summary");
        when(summaryPersistence.findByUrl(sourceUrl)).thenReturn(Optional.of(cached));

        String text = "Some article text. ".repeat(10);

        // Act
        Summary result = summaryService.summarizeText(text, null, null, null, sourceUrl);

        // Assert
        assertThat(result.getId()).isEqualTo("cached-text-id");
        verifyNoInteractions(ollamaSummarizer, openAiSummarizer);
    }

    @Test
    void shouldSummarizeTextWithoutSourceUrl() {
        // Arrange — no sourceUrl → always summarizes, no cache lookup
        String text = "Some article text. ".repeat(10);
        when(ollamaSummarizer.summarize(anyString(), isNull(), anyString())).thenReturn(SUMMARY_TEXT);
        when(summaryPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        summaryService.summarizeText(text, "My Title", null, null, null);

        // Assert
        verify(summaryPersistence, never()).findByUrl(any());
        verify(ollamaSummarizer).summarize(anyString(), isNull(), anyString());
    }

    @Test
    void shouldThrowWhenTextIsBlank() {
        assertThatThrownBy(() -> summaryService.summarizeText("  ", null, null, null, null))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("text must not be blank");
    }

    // ---- Helpers ----

    private Summary buildSummary(String id, String url, String title, String summaryText) {
        Summary s = new Summary();
        s.setId(id);
        s.setUrl(url);
        s.setTitle(title);
        s.setSummary(summaryText);
        s.setModelUsed("gemma3:4b");
        s.setCreatedAt(Instant.now());
        s.setIsRead(false);
        s.setSavedAt(Instant.now());
        return s;
    }
}
