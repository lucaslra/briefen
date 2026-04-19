package com.briefen.service;

import com.briefen.config.BriefenProperties;
import com.briefen.config.OllamaProperties;
import com.briefen.exception.InvalidUrlException;
import com.briefen.exception.SummarizationException;
import com.briefen.exception.SummaryNotFoundException;
import com.briefen.model.Summary;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.persistence.SummaryPersistence;
import com.briefen.validation.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);


    private final UrlValidator urlValidator;
    private final ArticleFetcherService articleFetcher;
    private final OllamaSummarizerService ollamaSummarizer;
    private final OpenAiSummarizerService openAiSummarizer;
    private final AnthropicSummarizerService anthropicSummarizer;
    private final SummaryPersistence summaryPersistence;
    private final SettingsPersistence settingsPersistence;
    private final OllamaProperties ollamaProperties;
    private final BriefenProperties briefenProperties;
    private final WebhookService webhookService;

    public SummaryService(UrlValidator urlValidator,
                          ArticleFetcherService articleFetcher,
                          OllamaSummarizerService ollamaSummarizer,
                          OpenAiSummarizerService openAiSummarizer,
                          AnthropicSummarizerService anthropicSummarizer,
                          SummaryPersistence summaryPersistence,
                          SettingsPersistence settingsPersistence,
                          OllamaProperties ollamaProperties,
                          BriefenProperties briefenProperties,
                          WebhookService webhookService) {
        this.urlValidator = urlValidator;
        this.articleFetcher = articleFetcher;
        this.ollamaSummarizer = ollamaSummarizer;
        this.openAiSummarizer = openAiSummarizer;
        this.anthropicSummarizer = anthropicSummarizer;
        this.summaryPersistence = summaryPersistence;
        this.settingsPersistence = settingsPersistence;
        this.ollamaProperties = ollamaProperties;
        this.briefenProperties = briefenProperties;
        this.webhookService = webhookService;
    }

    /**
     * Summarize from a URL — fetches the article, summarizes, and caches the result.
     */
    public Summary summarize(String userId, String url, boolean refresh, String lengthHint, String model) {
        URI validatedUri = urlValidator.validate(url);
        String normalizedUrl = validatedUri.toString();
        boolean isLengthAdjustment = lengthHint != null && !lengthHint.isBlank();

        // Only use cache for default-length summaries
        if (!refresh && !isLengthAdjustment) {
            Optional<Summary> cached = summaryPersistence.findByUrl(userId, normalizedUrl);
            if (cached.isPresent()) {
                log.info("Returning cached summary for {}", normalizedUrl);
                return cached.get();
            }
        }

        ArticleFetcherService.ArticleContent article = articleFetcher.fetch(normalizedUrl);
        String effectiveModel = (model != null && !model.isBlank()) ? model : ollamaProperties.model();
        String rawSummaryText = dispatchSummarize(userId, article.text(), lengthHint, effectiveModel);
        TagExtractor.Result extracted = TagExtractor.extract(rawSummaryText);
        String summaryText = extracted.summary();

        // Length-adjusted summaries are transient — don't persist them
        if (isLengthAdjustment) {
            Summary transient_ = new Summary(normalizedUrl, article.title(), summaryText, effectiveModel);
            transient_.setUserId(userId);
            transient_.setArticleText(article.text());
            if (!extracted.tags().isEmpty()) {
                transient_.setTags(extracted.tags());
            }
            log.info("Generated {} summary for {} (not persisted)", lengthHint, normalizedUrl);
            return transient_;
        }

        // Upsert: update existing or create new
        Summary summary = summaryPersistence.findByUrl(userId, normalizedUrl)
                .orElseGet(Summary::new);
        boolean isNew = summary.getId() == null;
        summary.setUserId(userId);
        summary.setUrl(normalizedUrl);
        summary.setTitle(article.title());
        summary.setSummary(summaryText);
        summary.setModelUsed(effectiveModel);
        summary.setArticleText(article.text());
        summary.setCreatedAt(Instant.now());
        if (isNew) {
            summary.setIsRead(false);
            summary.setSavedAt(summary.getCreatedAt());
        }
        // Auto-tag new summaries; preserve user-edited tags on refresh
        if (isNew && !extracted.tags().isEmpty()) {
            summary.setTags(extracted.tags());
        }

        Summary saved = summaryPersistence.save(summary);
        webhookService.send(saved, userId);
        return saved;
    }

    /**
     * Summarize from raw pasted text — no URL fetching, no caching.
     */
    public Summary summarizeText(String userId, String text, String title, String lengthHint, String model, String sourceUrl) {
        if (text == null || text.isBlank()) {
            throw new InvalidUrlException("Article text must not be blank");
        }

        boolean isLengthAdjustment = lengthHint != null && !lengthHint.isBlank();

        // Check cache by sourceUrl before generating (skip for length adjustments)
        String effectiveUrl = (sourceUrl != null && !sourceUrl.isBlank()) ? sourceUrl.trim() : null;
        if (!isLengthAdjustment && effectiveUrl != null) {
            Optional<Summary> cached = summaryPersistence.findByUrl(userId, effectiveUrl);
            if (cached.isPresent()) {
                log.info("Returning cached summary for source URL: {}", effectiveUrl);
                return cached.get();
            }
        }

        String effectiveModel = (model != null && !model.isBlank()) ? model : ollamaProperties.model();
        String rawSummaryText = dispatchSummarize(userId, text, lengthHint, effectiveModel);
        TagExtractor.Result extracted = TagExtractor.extract(rawSummaryText);
        String summaryText = extracted.summary();

        // Extract title from LLM response if not provided
        String effectiveTitle = (title != null && !title.isBlank()) ? title.trim() : null;
        if (effectiveTitle == null) {
            var parsed = extractTitle(summaryText);
            effectiveTitle = parsed.title();
            summaryText = parsed.body();
        }

        // Upsert when a sourceUrl is provided (avoids duplicate key on the unique URL+userId index)
        Summary result;
        boolean isNew;
        if (effectiveUrl != null) {
            var existing = summaryPersistence.findByUrl(userId, effectiveUrl);
            result = existing.orElseGet(Summary::new);
            isNew = existing.isEmpty();
            result.setUrl(effectiveUrl);
        } else {
            result = new Summary();
            isNew = true;
        }
        result.setUserId(userId);
        result.setTitle(effectiveTitle);
        result.setSummary(summaryText);
        result.setModelUsed(effectiveModel);
        result.setArticleText(text);
        result.setCreatedAt(java.time.Instant.now());
        if (isNew) {
            result.setIsRead(false);
            result.setSavedAt(result.getCreatedAt());
        }
        // Auto-tag new summaries; preserve user-edited tags on refresh
        if (isNew && !extracted.tags().isEmpty()) {
            result.setTags(extracted.tags());
        }
        result = summaryPersistence.save(result);
        webhookService.send(result, userId);
        log.info("Generated and saved summary from pasted text ({} chars, title='{}')", text.length(), effectiveTitle);
        return result;
    }

    private record TitleAndBody(String title, String body) {}

    /**
     * Extracts a markdown H1 title from the beginning of the summary text.
     */
    private TitleAndBody extractTitle(String text) {
        if (text != null && text.startsWith("# ")) {
            int newline = text.indexOf('\n');
            if (newline > 0) {
                String title = text.substring(2, newline).trim();
                String body = text.substring(newline + 1).stripLeading();
                if (!title.isEmpty()) {
                    return new TitleAndBody(title, body);
                }
            }
        }
        return new TitleAndBody("Pasted Article", text);
    }

    public Page<Summary> getSummaries(String userId, int page, int size) {
        return summaryPersistence.findAll(userId, page, size);
    }

    public Page<Summary> getSummaries(String userId, int page, int size, String filter) {
        return getSummaries(userId, page, size, filter, null);
    }

    public Page<Summary> getSummaries(String userId, int page, int size, String filter, String search) {
        return summaryPersistence.findAll(userId, page, size, filter, search);
    }

    public Page<Summary> getSummaries(String userId, int page, int size, String filter, String search, String tag) {
        return summaryPersistence.findAll(userId, page, size, filter, search, tag);
    }

    public List<Summary> getAllSummaries(String userId, String filter, String search) {
        return summaryPersistence.findAll(userId, filter, search);
    }

    public List<Summary> getAllSummaries(String userId, String filter, String search, String tag) {
        return summaryPersistence.findAll(userId, filter, search, tag);
    }

    public Summary getSummary(String userId, String id) {
        return summaryPersistence.findById(userId, id)
                .orElseThrow(() -> new SummaryNotFoundException(id));
    }

    public String getArticleText(String userId, String id) {
        Summary summary = summaryPersistence.findById(userId, id)
                .orElseThrow(() -> new SummaryNotFoundException(id));
        return summary.getArticleText();
    }

    public Summary updateReadStatus(String userId, String id, boolean isRead) {
        Summary summary = summaryPersistence.findById(userId, id)
                .orElseThrow(() -> new SummaryNotFoundException(id));
        summary.setIsRead(isRead);
        return summaryPersistence.save(summary);
    }

    public Summary updateNotes(String userId, String id, String notes) {
        Summary summary = summaryPersistence.findById(userId, id)
                .orElseThrow(() -> new SummaryNotFoundException(id));
        summary.setNotes((notes != null && !notes.isEmpty()) ? notes : null);
        return summaryPersistence.save(summary);
    }

    static final int MAX_TAG_LENGTH = 50;
    static final int MAX_TAG_COUNT = 20;

    public Summary updateTags(String userId, String id, java.util.List<String> tags) {
        Summary summary = summaryPersistence.findById(userId, id)
                .orElseThrow(() -> new SummaryNotFoundException(id));
        summary.setTags(tags != null ? tags.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(t -> !t.isEmpty() && t.length() <= MAX_TAG_LENGTH)
                .distinct()
                .limit(MAX_TAG_COUNT)
                .toList() : List.of());
        return summaryPersistence.save(summary);
    }

    public void deleteSummary(String userId, String id) {
        if (!summaryPersistence.existsById(userId, id)) {
            throw new SummaryNotFoundException(id);
        }
        summaryPersistence.deleteById(userId, id);
    }

    public long markAllAsRead(String userId) {
        return summaryPersistence.markAllAsRead(userId);
    }

    public long markAllAsUnread(String userId) {
        return summaryPersistence.markAllAsUnread(userId);
    }

    public long getUnreadCount(String userId) {
        return summaryPersistence.countUnread(userId);
    }

    /**
     * Routes to the correct summarizer based on the model name.
     * Loads the user's custom prompt (or falls back to the deployment-wide default).
     */
    private String dispatchSummarize(String userId, String articleText, String lengthHint, String model) {
        String customPrompt = loadCustomPrompt(userId);
        String deploymentPrompt = briefenProperties.defaultPrompt();

        if (isOpenAiModel(model)) {
            String apiKey = loadOpenAiKey(userId);
            return openAiSummarizer.summarize(articleText, lengthHint, model, apiKey, customPrompt, deploymentPrompt);
        }
        if (isAnthropicModel(model)) {
            String apiKey = loadAnthropicKey(userId);
            return anthropicSummarizer.summarize(articleText, lengthHint, model, apiKey, customPrompt, deploymentPrompt);
        }
        return ollamaSummarizer.summarize(articleText, lengthHint, model, customPrompt, deploymentPrompt);
    }

    private boolean isOpenAiModel(String model) {
        return model != null && (model.startsWith("gpt-")
                || model.startsWith("o3-")
                || model.startsWith("o4-")
                || model.startsWith("o1-"));
    }

    private boolean isAnthropicModel(String model) {
        return model != null && model.startsWith("claude-");
    }

    private String loadCustomPrompt(String userId) {
        return settingsPersistence.findByUserId(userId)
                .map(UserSettings::getCustomPrompt)
                .filter(p -> p != null && !p.isBlank())
                .orElse(null);
    }

    private String loadOpenAiKey(String userId) {
        return settingsPersistence.findByUserId(userId)
                .map(UserSettings::getOpenaiApiKey)
                .filter(key -> key != null && !key.isBlank())
                .orElseThrow(() -> new SummarizationException(
                        "OpenAI API key not configured. Please add your key in Settings.", false));
    }

    private String loadAnthropicKey(String userId) {
        return settingsPersistence.findByUserId(userId)
                .map(UserSettings::getAnthropicApiKey)
                .filter(key -> key != null && !key.isBlank())
                .orElseThrow(() -> new SummarizationException(
                        "Anthropic API key not configured. Please add your key in Settings.", false));
    }
}
