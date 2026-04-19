package com.briefen.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadeckControllerTest {

    // ---- sanitizeUrlForLog ----

    @Test
    void sanitizeUrlForLog_returnsSchemeAndHostOnly() {
        assertThat(ReadeckController.sanitizeUrlForLog("https://readeck.example.com/api/bookmarks?search=secret"))
                .isEqualTo("https://readeck.example.com");
    }

    @Test
    void sanitizeUrlForLog_stripsPathAndQuery() {
        assertThat(ReadeckController.sanitizeUrlForLog("http://192.168.1.10:8080/api/bookmarks/abc123?token=xyz"))
                .isEqualTo("http://192.168.1.10");
    }

    @Test
    void sanitizeUrlForLog_returnsMalformedForInvalidUrl() {
        assertThat(ReadeckController.sanitizeUrlForLog("not a url at all \n injected"))
                .isEqualTo("<malformed-url>");
    }

    @Test
    void sanitizeUrlForLog_returnsMalformedForNull() {
        assertThat(ReadeckController.sanitizeUrlForLog(null))
                .isEqualTo("<malformed-url>");
    }

    @Test
    void sanitizeUrlForLog_returnsMalformedForUrlWithNoHost() {
        assertThat(ReadeckController.sanitizeUrlForLog("file:///etc/passwd"))
                .isEqualTo("<malformed-url>");
    }

    // ---- validateReadeckUrl ----

    @ParameterizedTest
    @ValueSource(strings = {"http://readeck.local", "https://readeck.example.com", "http://192.168.1.10:8080"})
    void validateReadeckUrl_acceptsHttpAndHttps(String url) {
        // Should not throw
        ReadeckController.validateReadeckUrl(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://readeck.local", "file:///etc/passwd", "gopher://example.com", "javascript:alert(1)"})
    void validateReadeckUrl_rejectsNonHttpSchemes(String url) {
        assertThatThrownBy(() -> ReadeckController.validateReadeckUrl(url))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void validateReadeckUrl_rejectsInvalidUrl() {
        assertThatThrownBy(() -> ReadeckController.validateReadeckUrl("not a url"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
