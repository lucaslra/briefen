package com.briefen.controller;

import com.briefen.dto.ArticleQueueResponse;
import com.briefen.dto.ArticleSubmitRequest;
import com.briefen.exception.InvalidUrlException;
import com.briefen.security.BriefenUserDetails;
import com.briefen.service.SummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Accepts article URLs from external clients (e.g. the Briefen browser extension)
 * and queues them for background summarization.
 *
 * POST /api/articles → 202 Accepted immediately; summarization runs on a virtual thread.
 */
@RestController
@RequestMapping("/api/articles")
public class ArticlesController {

    private static final Logger log = LoggerFactory.getLogger(ArticlesController.class);

    private final SummaryService summaryService;

    public ArticlesController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping
    public ResponseEntity<ArticleQueueResponse> submitArticle(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @RequestBody ArticleSubmitRequest request) {
        if (request.url() == null || request.url().isBlank()) {
            throw new InvalidUrlException("A URL must be provided.");
        }

        String id = UUID.randomUUID().toString();
        String url = request.url();
        // Capture userId before spawning virtual thread — SecurityContext does not propagate
        String userId = userDetails.userId();

        Thread.ofVirtual().start(() -> {
            try {
                summaryService.summarize(userId, url, false, null, null);
            } catch (Exception e) {
                log.warn("Background summarization failed for {}: {}", url, e.getMessage());
            }
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ArticleQueueResponse(id, "QUEUED", "Article received and queued for summarization."));
    }
}
