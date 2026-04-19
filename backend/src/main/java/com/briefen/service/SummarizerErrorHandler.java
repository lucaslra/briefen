package com.briefen.service;

import com.briefen.exception.SummarizationException;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpTimeoutException;

/**
 * Shared error-handling logic for all LLM summarizer services.
 * Prevents duplication of timeout detection, HTTP error mapping, and fallback handling
 * across OllamaSummarizerService, OpenAiSummarizerService, and AnthropicSummarizerService.
 */
final class SummarizerErrorHandler {

    private SummarizerErrorHandler() {}

    /**
     * Maps a {@link ResourceAccessException} (network/timeout) to a {@link SummarizationException}.
     * Call this from every summarizer's {@code catch (ResourceAccessException e)} block.
     */
    static SummarizationException handleResourceAccessException(ResourceAccessException e, String provider, Logger log) {
        if (isTimeoutCause(e)) {
            log.error("{} request timed out", provider, e);
            return new SummarizationException(provider + " request timed out. The article may be too long.", e, true);
        }
        log.error("Failed to reach {}", provider, e);
        return new SummarizationException("Could not connect to " + provider + ". Check your network settings and server logs.", e, false);
    }

    /**
     * Maps a {@link HttpClientErrorException} (4xx) to a {@link SummarizationException}.
     * Handles 401 (bad key) and 429 (rate limit) with user-friendly messages.
     * Call this from cloud provider summarizers (OpenAI, Anthropic).
     */
    static SummarizationException handleHttpClientErrorException(HttpClientErrorException e, String provider, Logger log) {
        log.error("{} API error: {} {}", provider, e.getStatusCode(), e.getResponseBodyAsString());
        if (e.getStatusCode().value() == 401) {
            return new SummarizationException(
                    "Invalid " + provider + " API key. Please check your key in Settings.", false);
        }
        if (e.getStatusCode().value() == 429) {
            return new SummarizationException(
                    provider + " rate limit exceeded. Please wait a moment and try again.", e, true);
        }
        return new SummarizationException(
                provider + " request failed with status " + e.getStatusCode().value() + ". Check server logs for details.", e, false);
    }

    /**
     * Maps a {@link HttpServerErrorException} (5xx) to a {@link SummarizationException}.
     * Handles 504 as a timeout. Used by OllamaSummarizerService.
     */
    static SummarizationException handleHttpServerErrorException(HttpServerErrorException e, String provider, Logger log) {
        if (e.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT) {
            log.error("{} gateway timed out (504)", provider, e);
            return new SummarizationException(
                    "Summarization timed out. The article may be too long or the model is overloaded.", e, true);
        }
        log.error("{} server error: {}", provider, e.getStatusCode(), e);
        return new SummarizationException(provider + " returned a server error: " + e.getStatusCode(), e, false);
    }

    /**
     * Fallback for any unexpected exception not caught by more specific handlers.
     */
    static SummarizationException handleUnexpectedException(Exception e, String provider, Logger log) {
        log.error("Unexpected error during {} summarization", provider, e);
        return new SummarizationException(provider + " summarization failed: " + e.getMessage(), e, false);
    }

    /**
     * Returns true if the exception (or any of its causes) is a network timeout.
     */
    static boolean isTimeoutCause(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException || cause instanceof java.net.SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
