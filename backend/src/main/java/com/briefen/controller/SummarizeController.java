package com.briefen.controller;

import com.briefen.dto.ReadStatusRequest;
import com.briefen.dto.SummarizeRequest;
import com.briefen.dto.SummarizeResponse;
import com.briefen.dto.UpdateNotesRequest;
import com.briefen.dto.UpdateTagsRequest;
import com.briefen.exception.InvalidUrlException;
import com.briefen.model.Summary;
import com.briefen.security.BriefenUserDetails;
import com.briefen.service.SummaryService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class SummarizeController {

    private final SummaryService summaryService;

    public SummarizeController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping("/summarize")
    public SummarizeResponse summarize(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @Valid @RequestBody SummarizeRequest request,
            @RequestParam(defaultValue = "false") boolean refresh) {

        String userId = userDetails.userId();
        boolean hasUrl = request.url() != null && !request.url().isBlank();
        boolean hasText = request.text() != null && !request.text().isBlank();

        if (!hasUrl && !hasText) {
            throw new InvalidUrlException("Either a URL or article text must be provided.");
        }

        Summary summary;
        if (hasText) {
            summary = summaryService.summarizeText(userId, request.text(), request.title(), request.lengthHint(), request.model(), request.sourceUrl());
        } else {
            summary = summaryService.summarize(userId, request.url(), refresh, request.lengthHint(), request.model());
        }

        return SummarizeResponse.from(summary);
    }

    @GetMapping("/summaries")
    public Page<SummarizeResponse> summaries(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag) {
        return summaryService.getSummaries(userDetails.userId(), page, size, filter, search, tag)
                .map(SummarizeResponse::from);
    }

    @GetMapping("/summaries/export")
    public ResponseEntity<StreamingResponseBody> exportSummaries(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @RequestParam(defaultValue = "md") String format,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag) {

        if (!"md".equals(format)) {
            return ResponseEntity.badRequest().build();
        }

        List<Summary> summaries = summaryService.getAllSummaries(userDetails.userId(), filter, search, tag);

        StreamingResponseBody body = outputStream -> {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            for (int i = 0; i < summaries.size(); i++) {
                Summary s = summaries.get(i);
                String title = (s.getTitle() != null && !s.getTitle().isBlank()) ? s.getTitle() : "Untitled";
                writer.write("## " + title + "\n\n");
                if (s.getUrl() != null && !s.getUrl().isBlank()) {
                    writer.write("*Source: " + s.getUrl() + "*\n\n");
                }
                if (s.getSummary() != null) {
                    writer.write(s.getSummary() + "\n");
                }
                if (i < summaries.size() - 1) {
                    writer.write("\n---\n\n");
                }
            }
            writer.flush();
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "markdown", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "briefen-export.md");

        return ResponseEntity.ok().headers(headers).body(body);
    }

    @GetMapping("/summaries/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        return Map.of("count", summaryService.getUnreadCount(userDetails.userId()));
    }

    @GetMapping("/summaries/{id}/article-text")
    public Map<String, String> getArticleText(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id) {
        String articleText = summaryService.getArticleText(userDetails.userId(), id);
        return Map.of("articleText", articleText != null ? articleText : "");
    }

    @PatchMapping("/summaries/{id}/read-status")
    public SummarizeResponse updateReadStatus(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id,
            @RequestBody ReadStatusRequest request) {
        if (request.isRead() == null) {
            throw new InvalidUrlException("isRead field is required");
        }
        return SummarizeResponse.from(summaryService.updateReadStatus(userDetails.userId(), id, request.isRead()));
    }

    @PatchMapping("/summaries/read-status/bulk")
    public Map<String, Long> markAllAsRead(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        return Map.of("updated", summaryService.markAllAsRead(userDetails.userId()));
    }

    @PatchMapping("/summaries/unread-status/bulk")
    public Map<String, Long> markAllAsUnread(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        return Map.of("updated", summaryService.markAllAsUnread(userDetails.userId()));
    }

    @PatchMapping("/summaries/{id}/notes")
    public SummarizeResponse updateNotes(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id,
            @RequestBody UpdateNotesRequest request) {
        return SummarizeResponse.from(summaryService.updateNotes(userDetails.userId(), id, request.notes()));
    }

    @PatchMapping("/summaries/{id}/tags")
    public SummarizeResponse updateTags(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id,
            @RequestBody UpdateTagsRequest request) {
        return SummarizeResponse.from(summaryService.updateTags(userDetails.userId(), id, request.tags()));
    }

    @DeleteMapping("/summaries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSummary(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id) {
        summaryService.deleteSummary(userDetails.userId(), id);
    }
}
