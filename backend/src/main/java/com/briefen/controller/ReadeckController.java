package com.briefen.controller;

import com.briefen.security.BriefenUserDetails;
import com.briefen.service.ReadeckService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Proxies requests to a user-configured Readeck instance.
 * The frontend never sees the Readeck API key — it stays server-side.
 * All business logic lives in {@link ReadeckService}.
 */
@RestController
@RequestMapping("/api/readeck")
@Validated
public class ReadeckController {

    private final ReadeckService readeckService;

    public ReadeckController(ReadeckService readeckService) {
        this.readeckService = readeckService;
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        return readeckService.getStatus(userDetails.userId());
    }

    @GetMapping("/bookmarks")
    public String listBookmarks(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return readeckService.listBookmarks(userDetails.userId(), page, limit, search, status);
    }

    @GetMapping("/bookmarks/{id}")
    public String getBookmark(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id) {
        return readeckService.getBookmark(userDetails.userId(), id);
    }

    @GetMapping("/bookmarks/{id}/article")
    public Map<String, String> getArticleContent(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id) {
        return readeckService.getArticleContent(userDetails.userId(), id);
    }
}
