package com.briefly.service;

import com.briefly.exception.ArticleExtractionException;
import com.briefly.exception.ArticleFetchException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

@Service
public class ArticleFetcherService {

    private static final Logger log = LoggerFactory.getLogger(ArticleFetcherService.class);
    private static final int MIN_CONTENT_LENGTH = 100;

    private final int fetchTimeoutMs;

    public ArticleFetcherService(@Value("${article.fetch-timeout:10s}") Duration fetchTimeout) {
        this.fetchTimeoutMs = (int) fetchTimeout.toMillis();
    }

    public ArticleContent fetch(String url) {
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .timeout(fetchTimeoutMs)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .followRedirects(true)
                    .get();
        } catch (HttpStatusException e) {
            log.error("HTTP {} fetching article from {}: {}", e.getStatusCode(), url, e.getMessage());
            if (e.getStatusCode() == 403) {
                throw new ArticleFetchException(
                        "This website blocked the request (HTTP 403). It may use bot protection (e.g. Cloudflare) that prevents automated access.", e);
            }
            throw new ArticleFetchException("Failed to fetch article: HTTP " + e.getStatusCode(), e);
        } catch (IOException e) {
            log.error("Failed to fetch article from {}: {}", url, e.getMessage());
            throw new ArticleFetchException("Failed to fetch article: " + e.getMessage(), e);
        }

        String title = extractTitle(doc);
        String text = extractText(doc);

        if (text.length() < MIN_CONTENT_LENGTH) {
            throw new ArticleExtractionException(
                    "Extracted content too short (%d chars). This URL may not contain an article."
                            .formatted(text.length()));
        }

        log.info("Fetched article '{}' from {} ({} chars)", title, url, text.length());
        return new ArticleContent(title, text);
    }

    private String extractTitle(Document doc) {
        Element h1 = doc.selectFirst("article h1, main h1, h1");
        if (h1 != null && !h1.text().isBlank()) {
            return h1.text().strip();
        }
        String title = doc.title();
        return (title != null && !title.isBlank()) ? title.strip() : "Untitled";
    }

    private String extractText(Document doc) {
        // Remove non-content elements
        doc.select("script, style, nav, footer, header, aside, iframe, form, .ad, .ads, .advertisement, .sidebar, .menu, .navigation").remove();

        // Try to find the main article content
        Element content = doc.selectFirst("article");
        if (content == null) content = doc.selectFirst("main");
        if (content == null) content = doc.selectFirst("[role=main]");
        if (content == null) content = doc.body();

        if (content == null) {
            return "";
        }

        return content.text().strip().replaceAll("\\s+", " ");
    }

    public record ArticleContent(String title, String text) {}
}
