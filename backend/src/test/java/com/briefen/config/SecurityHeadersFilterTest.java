package com.briefen.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void shouldSetContentSecurityPolicyHeader() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        String csp = response.getHeader("Content-Security-Policy");
        assertThat(csp).contains("default-src 'self'");
        assertThat(csp).contains("script-src 'self'");
        assertThat(csp).contains("frame-ancestors 'none'");
        assertThat(csp).contains("form-action 'self'");
    }

    @Test
    void shouldSetXFrameOptionsDeny() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void shouldSetXContentTypeOptionsNosniff() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void shouldSetReferrerPolicyHeader() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    void shouldSetPermissionsPolicyHeader() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        String policy = response.getHeader("Permissions-Policy");
        assertThat(policy).contains("camera=()");
        assertThat(policy).contains("microphone=()");
        assertThat(policy).contains("geolocation=()");
    }

    @Test
    void shouldContinueFilterChain() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSetAllFiveHeadersInOnePass() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("Content-Security-Policy")).isNotBlank();
        assertThat(response.getHeader("X-Frame-Options")).isNotBlank();
        assertThat(response.getHeader("X-Content-Type-Options")).isNotBlank();
        assertThat(response.getHeader("Referrer-Policy")).isNotBlank();
        assertThat(response.getHeader("Permissions-Policy")).isNotBlank();
    }
}
