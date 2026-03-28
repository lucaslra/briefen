package com.briefly.controller;

import com.briefly.dto.SummarizeRequest;
import com.briefly.dto.SummarizeResponse;
import com.briefly.model.Summary;
import com.briefly.service.SummaryService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody SummarizeRequest request,
            @RequestParam(defaultValue = "false") boolean refresh) {
        Summary summary = summaryService.summarize(request.url(), refresh, request.lengthHint());
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
