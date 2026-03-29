package com.briefen.service;

import com.briefen.config.OllamaProperties;
import com.briefen.exception.InvalidUrlException;
import com.briefen.exception.SummarizationException;
import com.briefen.model.Summary;
import com.briefen.model.UserSettings;
import com.briefen.repository.SummaryRepository;
import com.briefen.repository.UserSettingsRepository;
import com.briefen.validation.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private static final Set<String> OPENAI_MODELS = Set.of(
            "gpt-4o-mini", "gpt-4o", "gpt-4.1-nano", "gpt-4.1-mini",
            "gpt-4.5-preview", "gpt-5-mini",
            "o3-mini", "o4-mini"
    );

    private final UrlValidator urlValidator;
    private final ArticleFetcherService articleFetcher;
    private final OllamaSummarizerService ollamaSummarizer;
    private final OpenAiSummarizerService openAiSummarizer;
    private final SummaryRepository repository;
    private final UserSettingsRepository settingsRepository;
    private final OllamaProperties ollamaProperties;

    public SummaryService(UrlValidator urlValidator,
                          ArticleFetcherService articleFetcher,
                          OllamaSummarizerService ollamaSummarizer,
                          OpenAiSummarizerService openAiSummarizer,
                          SummaryRepository repository,
                          UserSettingsRepository settingsRepository,
                          OllamaProperties ollamaProperties) {
        this.urlValidator = urlValidator;
        this.articleFetcher = articleFetcher;
        this.ollamaSummarizer = ollamaSummarizer;
        this.openAiSummarizer = openAiSummarizer;
        this.repository = repository;
        this.settingsRepository = settingsRepository;
        this.ollamaProperties = ollamaProperties;
    }

    /**
     * Summarize from a URL — fetches the article, summarizes, and caches the result.
     */
    public Summary summarize(String url, boolean refresh, String lengthHint, String model) {
        URI validatedUri = urlValidator.validate(url);
        String normalizedUrl = validatedUri.toString();
        boolean isLengthAdjustment = lengthHint != null && !lengthHint.isBlank();

        // Only use cache for default-length summaries
        if (!refresh && !isLengthAdjustment) {
            Optional<Summary> cached = repository.findByUrl(normalizedUrl);
            if (cached.isPresent()) {
                log.info("Returning cached summary for {}", normalizedUrl);
                return cached.get();
            }
        }

        ArticleFetcherService.ArticleContent article = articleFetcher.fetch(normalizedUrl);
        String effectiveModel = (model != null && !model.isBlank()) ? model : ollamaProperties.model();
        String summaryText = dispatchSummarize(article.text(), lengthHint, effectiveModel);

        // Length-adjusted summaries are transient — don't persist them
        if (isLengthAdjustment) {
            Summary transient_ = new Summary(normalizedUrl, article.title(), summaryText, effectiveModel);
            log.info("Generated {} summary for {} (not persisted)", lengthHint, normalizedUrl);
            return transient_;
        }

        // Upsert: update existing or create new
        Summary summary = repository.findByUrl(normalizedUrl)
                .orElseGet(Summary::new);
        summary.setUrl(normalizedUrl);
        summary.setTitle(article.title());
        summary.setSummary(summaryText);
        summary.setModelUsed(effectiveModel);
        summary.setCreatedAt(Instant.now());

        return repository.save(summary);
    }

    /**
     * Summarize from raw pasted text — no URL fetching, no caching.
     */
    public Summary summarizeText(String text, String title, String lengthHint, String model) {
        if (text == null || text.isBlank()) {
            throw new InvalidUrlException("Article text must not be blank");
        }

        String effectiveTitle = (title != null && !title.isBlank()) ? title.trim() : "Pasted Article";
        String effectiveModel = (model != null && !model.isBlank()) ? model : ollamaProperties.model();
        String summaryText = dispatchSummarize(text, lengthHint, effectiveModel);

        // Pasted-text summaries are always transient (no URL to key on)
        Summary result = new Summary(null, effectiveTitle, summaryText, effectiveModel);
        log.info("Generated summary from pasted text ({} chars, title='{}')", text.length(), effectiveTitle);
        return result;
    }

    public Page<Summary> getSummaries(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    /**
     * Routes to the correct summarizer based on the model name.
     */
    private String dispatchSummarize(String articleText, String lengthHint, String model) {
        if (isOpenAiModel(model)) {
            String apiKey = loadOpenAiKey();
            return openAiSummarizer.summarize(articleText, lengthHint, model, apiKey);
        }
        return ollamaSummarizer.summarize(articleText, lengthHint, model);
    }

    private boolean isOpenAiModel(String model) {
        return model != null && (OPENAI_MODELS.contains(model)
                || model.startsWith("gpt-")
                || model.startsWith("o3-")
                || model.startsWith("o4-"));
    }

    private String loadOpenAiKey() {
        return settingsRepository.findById(UserSettings.DEFAULT_ID)
                .map(UserSettings::getOpenaiApiKey)
                .filter(key -> key != null && !key.isBlank())
                .orElseThrow(() -> new SummarizationException(
                        "OpenAI API key not configured. Please add your key in Settings.", false));
    }
}
