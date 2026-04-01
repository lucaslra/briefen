package com.briefen.controller;

import com.briefen.dto.ReadStatusRequest;
import com.briefen.dto.SummarizeRequest;
import com.briefen.dto.SummarizeResponse;
import com.briefen.dto.UpdateNotesRequest;
import com.briefen.exception.InvalidUrlException;
import com.briefen.model.Summary;
import com.briefen.service.SummaryService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SummarizeController {

    private final SummaryService summaryService;

    public SummarizeController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping("/summarize")
    public SummarizeResponse summarize(
            @RequestBody SummarizeRequest request,
            @RequestParam(defaultValue = "false") boolean refresh) {

        boolean hasUrl = request.url() != null && !request.url().isBlank();
        boolean hasText = request.text() != null && !request.text().isBlank();

        if (!hasUrl && !hasText) {
            throw new InvalidUrlException("Either a URL or article text must be provided.");
        }

        Summary summary;
        if (hasText) {
            summary = summaryService.summarizeText(request.text(), request.title(), request.lengthHint(), request.model(), request.sourceUrl());
        } else {
            summary = summaryService.summarize(request.url(), refresh, request.lengthHint(), request.model());
        }

        return SummarizeResponse.from(summary);
    }

    @GetMapping("/summaries")
    public Page<SummarizeResponse> summaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String search) {
        return summaryService.getSummaries(page, size, filter, search)
                .map(SummarizeResponse::from);
    }

    @GetMapping("/summaries/export")
    public ResponseEntity<StreamingResponseBody> exportSummaries(
            @RequestParam(defaultValue = "md") String format,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String search) {

        if (!"md".equals(format)) {
            return ResponseEntity.badRequest().build();
        }

        List<Summary> summaries = summaryService.getAllSummaries(filter, search);

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
    public Map<String, Long> unreadCount() {
        return Map.of("count", summaryService.getUnreadCount());
    }

    @PatchMapping("/summaries/{id}/read-status")
    public SummarizeResponse updateReadStatus(
            @PathVariable String id,
            @RequestBody ReadStatusRequest request) {
        if (request.isRead() == null) {
            throw new InvalidUrlException("isRead field is required");
        }
        return SummarizeResponse.from(summaryService.updateReadStatus(id, request.isRead()));
    }

    @PatchMapping("/summaries/read-status/bulk")
    public Map<String, Long> markAllAsRead() {
        return Map.of("updated", summaryService.markAllAsRead());
    }

    @PatchMapping("/summaries/unread-status/bulk")
    public Map<String, Long> markAllAsUnread() {
        return Map.of("updated", summaryService.markAllAsUnread());
    }

    @PatchMapping("/summaries/{id}/notes")
    public SummarizeResponse updateNotes(
            @PathVariable String id,
            @RequestBody UpdateNotesRequest request) {
        return SummarizeResponse.from(summaryService.updateNotes(id, request.notes()));
    }

    @DeleteMapping("/summaries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSummary(@PathVariable String id) {
        summaryService.deleteSummary(id);
    }
}
