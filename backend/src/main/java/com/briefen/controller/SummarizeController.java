package com.briefen.controller;

import com.briefen.dto.SummarizeRequest;
import com.briefen.dto.SummarizeResponse;
import com.briefen.exception.InvalidUrlException;
import com.briefen.model.Summary;
import com.briefen.service.SummaryService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "10") int size) {
        return summaryService.getSummaries(page, size)
                .map(SummarizeResponse::from);
    }
}
