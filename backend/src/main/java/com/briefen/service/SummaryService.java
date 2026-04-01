package com.briefen.service;

import com.briefen.config.OllamaProperties;
import com.briefen.exception.InvalidUrlException;
import com.briefen.exception.SummarizationException;
import com.briefen.exception.SummaryNotFoundException;
import com.briefen.model.Summary;
import com.briefen.model.UserSettings;
import com.briefen.repository.SummaryRepository;
import com.briefen.repository.UserSettingsRepository;
import com.briefen.validation.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
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
    private final MongoTemplate mongoTemplate;

    public SummaryService(UrlValidator urlValidator,
                          ArticleFetcherService articleFetcher,
                          OllamaSummarizerService ollamaSummarizer,
                          OpenAiSummarizerService openAiSummarizer,
                          SummaryRepository repository,
                          UserSettingsRepository settingsRepository,
                          OllamaProperties ollamaProperties,
                          MongoTemplate mongoTemplate) {
        this.urlValidator = urlValidator;
        this.articleFetcher = articleFetcher;
        this.ollamaSummarizer = ollamaSummarizer;
        this.openAiSummarizer = openAiSummarizer;
        this.repository = repository;
        this.settingsRepository = settingsRepository;
        this.ollamaProperties = ollamaProperties;
        this.mongoTemplate = mongoTemplate;
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
        boolean isNew = summary.getId() == null;
        summary.setUrl(normalizedUrl);
        summary.setTitle(article.title());
        summary.setSummary(summaryText);
        summary.setModelUsed(effectiveModel);
        summary.setCreatedAt(Instant.now());
        if (isNew) {
            summary.setIsRead(false);
            summary.setSavedAt(summary.getCreatedAt());
        }

        return repository.save(summary);
    }

    /**
     * Summarize from raw pasted text — no URL fetching, no caching.
     */
    public Summary summarizeText(String text, String title, String lengthHint, String model, String sourceUrl) {
        if (text == null || text.isBlank()) {
            throw new InvalidUrlException("Article text must not be blank");
        }

        boolean isLengthAdjustment = lengthHint != null && !lengthHint.isBlank();

        // Check cache by sourceUrl before generating (skip for length adjustments)
        String effectiveUrl = (sourceUrl != null && !sourceUrl.isBlank()) ? sourceUrl.trim() : null;
        if (!isLengthAdjustment && effectiveUrl != null) {
            Optional<Summary> cached = repository.findByUrl(effectiveUrl);
            if (cached.isPresent()) {
                log.info("Returning cached summary for source URL: {}", effectiveUrl);
                return cached.get();
            }
        }

        String effectiveModel = (model != null && !model.isBlank()) ? model : ollamaProperties.model();
        String summaryText = dispatchSummarize(text, lengthHint, effectiveModel);

        // Extract title from LLM response if not provided
        String effectiveTitle = (title != null && !title.isBlank()) ? title.trim() : null;
        if (effectiveTitle == null) {
            var parsed = extractTitle(summaryText);
            effectiveTitle = parsed.title();
            summaryText = parsed.body();
        }

        // Upsert when a sourceUrl is provided (avoids duplicate key on the unique URL index)
        Summary result;
        boolean isNew;
        if (effectiveUrl != null) {
            var existing = repository.findByUrl(effectiveUrl);
            result = existing.orElseGet(Summary::new);
            isNew = existing.isEmpty();
            result.setUrl(effectiveUrl);
        } else {
            result = new Summary();
            isNew = true;
        }
        result.setTitle(effectiveTitle);
        result.setSummary(summaryText);
        result.setModelUsed(effectiveModel);
        result.setCreatedAt(java.time.Instant.now());
        if (isNew) {
            result.setIsRead(false);
            result.setSavedAt(result.getCreatedAt());
        }
        result = repository.save(result);
        log.info("Generated and saved summary from pasted text ({} chars, title='{}')", text.length(), effectiveTitle);
        return result;
    }

    private record TitleAndBody(String title, String body) {}

    /**
     * Extracts a markdown H1 title from the beginning of the summary text.
     * If found, returns the title and the remaining body separately.
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

    public Page<Summary> getSummaries(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public Page<Summary> getSummaries(int page, int size, String filter) {
        return getSummaries(page, size, filter, null);
    }

    public Page<Summary> getSummaries(int page, int size, String filter, String search) {
        PageRequest pageable = PageRequest.of(page, size);
        Query query = buildFilterQuery(filter, search);

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Summary.class);
        query.with(pageable);
        List<Summary> items = mongoTemplate.find(query, Summary.class);
        return new PageImpl<>(items, pageable, total);
    }

    public List<Summary> getAllSummaries(String filter, String search) {
        Query query = buildFilterQuery(filter, search);
        return mongoTemplate.find(query, Summary.class);
    }

    private Query buildFilterQuery(String filter, String search) {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "createdAt"));

        switch (filter) {
            case "unread" -> query.addCriteria(Criteria.where("isRead").ne(true));
            case "read" -> query.addCriteria(Criteria.where("isRead").is(true));
            default -> {} // "all" — no filter
        }

        if (search != null && !search.isBlank()) {
            String escapedSearch = search.replaceAll("([\\\\.*+?^${}()|\\[\\]])", "\\\\$1");
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(escapedSearch, "i"),
                    Criteria.where("summary").regex(escapedSearch, "i"),
                    Criteria.where("notes").regex(escapedSearch, "i")
            );
            query.addCriteria(searchCriteria);
        }

        return query;
    }

    public Summary updateReadStatus(String id, boolean isRead) {
        Summary summary = repository.findById(id)
                .orElseThrow(() -> new SummaryNotFoundException(id));
        summary.setIsRead(isRead);
        return repository.save(summary);
    }

    public Summary updateNotes(String id, String notes) {
        Summary summary = repository.findById(id)
                .orElseThrow(() -> new SummaryNotFoundException(id));
        summary.setNotes((notes != null && !notes.isEmpty()) ? notes : null);
        return repository.save(summary);
    }

    public void deleteSummary(String id) {
        if (!repository.existsById(id)) {
            throw new SummaryNotFoundException(id);
        }
        repository.deleteById(id);
    }

    public long markAllAsRead() {
        Query query = new Query(Criteria.where("isRead").ne(true));
        Update update = new Update().set("isRead", true);
        return mongoTemplate.updateMulti(query, update, Summary.class).getModifiedCount();
    }

    public long markAllAsUnread() {
        Query query = new Query(Criteria.where("isRead").is(true));
        Update update = new Update().set("isRead", false);
        return mongoTemplate.updateMulti(query, update, Summary.class).getModifiedCount();
    }

    public long getUnreadCount() {
        Query query = new Query(Criteria.where("isRead").ne(true));
        return mongoTemplate.count(query, Summary.class);
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
