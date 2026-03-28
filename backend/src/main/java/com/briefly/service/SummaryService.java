package com.briefly.service;

import com.briefly.config.OllamaProperties;
import com.briefly.model.Summary;
import com.briefly.repository.SummaryRepository;
import com.briefly.validation.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final UrlValidator urlValidator;
    private final ArticleFetcherService articleFetcher;
    private final OllamaSummarizerService summarizer;
    private final SummaryRepository repository;
    private final OllamaProperties ollamaProperties;

    public SummaryService(UrlValidator urlValidator,
                          ArticleFetcherService articleFetcher,
                          OllamaSummarizerService summarizer,
                          SummaryRepository repository,
                          OllamaProperties ollamaProperties) {
        this.urlValidator = urlValidator;
        this.articleFetcher = articleFetcher;
        this.summarizer = summarizer;
        this.repository = repository;
        this.ollamaProperties = ollamaProperties;
    }

    public Summary summarize(String url, boolean refresh, String lengthHint) {
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
        String summaryText = summarizer.summarize(article.text(), lengthHint);

        // Length-adjusted summaries are transient — don't persist them
        if (isLengthAdjustment) {
            Summary transient_ = new Summary(normalizedUrl, article.title(), summaryText, ollamaProperties.model());
            log.info("Generated {} summary for {} (not persisted)", lengthHint, normalizedUrl);
            return transient_;
        }

        // Upsert: update existing or create new
        Summary summary = repository.findByUrl(normalizedUrl)
                .orElseGet(Summary::new);
        summary.setUrl(normalizedUrl);
        summary.setTitle(article.title());
        summary.setSummary(summaryText);
        summary.setModelUsed(ollamaProperties.model());
        summary.setCreatedAt(Instant.now());

        return repository.save(summary);
    }

    // TODO: Rate limiting should be applied here
    public Page<Summary> getSummaries(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }
}
