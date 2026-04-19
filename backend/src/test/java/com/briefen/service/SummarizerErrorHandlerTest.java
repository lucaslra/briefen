package com.briefen.service;

import com.briefen.exception.SummarizationException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class SummarizerErrorHandlerTest {

    private static final Logger log = LoggerFactory.getLogger(SummarizerErrorHandlerTest.class);

    // ---- isTimeoutCause ----

    @Test
    void isTimeoutCause_trueForHttpTimeoutException() {
        var e = new ResourceAccessException("timeout", new HttpTimeoutException("timed out"));
        assertThat(SummarizerErrorHandler.isTimeoutCause(e)).isTrue();
    }

    @Test
    void isTimeoutCause_trueForSocketTimeoutException() {
        var e = new ResourceAccessException("timeout", new SocketTimeoutException("timed out"));
        assertThat(SummarizerErrorHandler.isTimeoutCause(e)).isTrue();
    }

    @Test
    void isTimeoutCause_falseForConnectionRefused() {
        var e = new ResourceAccessException("refused", new java.net.ConnectException("refused"));
        assertThat(SummarizerErrorHandler.isTimeoutCause(e)).isFalse();
    }

    // ---- handleResourceAccessException ----

    @Test
    void handleResourceAccessException_mapsTimeoutToTimeoutException() {
        var e = new ResourceAccessException("timeout", new SocketTimeoutException("timed out"));
        SummarizationException result = SummarizerErrorHandler.handleResourceAccessException(e, "TestProvider", log);
        assertThat(result.isTimeout()).isTrue();
        assertThat(result.getMessage()).contains("timed out");
    }

    @Test
    void handleResourceAccessException_mapsConnectionErrorToNonTimeout() {
        var e = new ResourceAccessException("refused", new java.net.ConnectException("refused"));
        SummarizationException result = SummarizerErrorHandler.handleResourceAccessException(e, "TestProvider", log);
        assertThat(result.isTimeout()).isFalse();
        assertThat(result.getMessage()).contains("Could not connect");
    }

    // ---- handleHttpClientErrorException ----

    @Test
    void handleHttpClientErrorException_maps401ToInvalidKey() {
        var e = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized",
                new org.springframework.http.HttpHeaders(), new byte[0], null);
        SummarizationException result = SummarizerErrorHandler.handleHttpClientErrorException(e, "OpenAI", log);
        assertThat(result.isTimeout()).isFalse();
        assertThat(result.getMessage()).containsIgnoringCase("API key");
    }

    @Test
    void handleHttpClientErrorException_maps429ToRateLimit() {
        var e = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                new org.springframework.http.HttpHeaders(), new byte[0], null);
        SummarizationException result = SummarizerErrorHandler.handleHttpClientErrorException(e, "Anthropic", log);
        assertThat(result.isTimeout()).isTrue();
        assertThat(result.getMessage()).containsIgnoringCase("rate limit");
    }

    @Test
    void handleHttpClientErrorException_mapsOtherStatusToGenericError() {
        var e = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                new org.springframework.http.HttpHeaders(), new byte[0], null);
        SummarizationException result = SummarizerErrorHandler.handleHttpClientErrorException(e, "OpenAI", log);
        assertThat(result.isTimeout()).isFalse();
        assertThat(result.getMessage()).contains("400");
    }

    // ---- handleHttpServerErrorException ----

    @Test
    void handleHttpServerErrorException_maps504ToTimeout() {
        var e = HttpServerErrorException.create(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout",
                new org.springframework.http.HttpHeaders(), new byte[0], null);
        SummarizationException result = SummarizerErrorHandler.handleHttpServerErrorException(e, "Ollama", log);
        assertThat(result.isTimeout()).isTrue();
    }

    @Test
    void handleHttpServerErrorException_maps500ToServerError() {
        var e = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                new org.springframework.http.HttpHeaders(), new byte[0], null);
        SummarizationException result = SummarizerErrorHandler.handleHttpServerErrorException(e, "Ollama", log);
        assertThat(result.isTimeout()).isFalse();
        assertThat(result.getMessage()).contains("server error");
    }

    // ---- handleUnexpectedException ----

    @Test
    void handleUnexpectedException_wrapsMessageAndProvider() {
        var e = new RuntimeException("something unexpected");
        SummarizationException result = SummarizerErrorHandler.handleUnexpectedException(e, "OpenAI", log);
        assertThat(result.isTimeout()).isFalse();
        assertThat(result.getMessage()).contains("OpenAI").contains("something unexpected");
    }
}
