package com.briefen.controller;

import com.briefen.exception.ArticleExtractionException;
import com.briefen.exception.ArticleFetchException;
import com.briefen.exception.InvalidUrlException;
import com.briefen.exception.SummarizationException;
import com.briefen.exception.SummaryNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFound_returns404() {
        var response = handler.handleNotFound(new SummaryNotFoundException("abc"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void invalidUrl_returns400() {
        var response = handler.handleInvalidUrl(new InvalidUrlException("bad url"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("bad url");
    }

    @Test
    void articleExtraction_returns400() {
        var response = handler.handleExtractionError(new ArticleExtractionException("cannot parse"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    void articleFetch_returns502() {
        var response = handler.handleFetchError(new ArticleFetchException("connection refused"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(502);
    }

    @Test
    void summarizationTimeout_returns504() {
        var response = handler.handleSummarizationError(new SummarizationException("timed out", true));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(504);
    }

    @Test
    void summarizationError_returns502() {
        var response = handler.handleSummarizationError(new SummarizationException("model error", false));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(502);
    }

    @Test
    void illegalArgument_returns400() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("bad input");
    }

    @Test
    void accessDenied_returns403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("no access"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Access denied.");
    }

    @Test
    void generic_returns500WithSafeMessage() {
        var response = handler.handleGeneric(new RuntimeException("internal details"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("An unexpected error occurred.");
        assertThat(response.getBody().error()).doesNotContain("internal details");
    }
}
