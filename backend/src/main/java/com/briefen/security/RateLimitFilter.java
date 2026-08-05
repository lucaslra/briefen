package com.briefen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-IP rate limiting for abuse-sensitive, unauthenticated auth endpoints:
 * the OIDC handshake (whose callback triggers an outbound token exchange to the
 * IdP — an unthrottled DoS/abuse vector) and first-run setup (brute-force window).
 *
 * <p>Runs ahead of Spring Security so rejected requests never reach the OAuth2
 * machinery. Client IP comes from {@code getRemoteAddr()}, which already reflects
 * {@code X-Forwarded-For} when {@code SERVER_FORWARD_HEADERS_STRATEGY=FRAMEWORK}.
 * Disable with {@code BRIEFEN_RATE_LIMIT_ENABLED=false}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final boolean enabled;
    private final int maxRequests;
    private final long windowMillis;

    public RateLimitFilter(RateLimiter rateLimiter,
                           @Value("${briefen.rate-limit.enabled:true}") boolean enabled,
                           @Value("${briefen.rate-limit.max-requests:30}") int maxRequests,
                           @Value("${briefen.rate-limit.window:60s}") Duration window) {
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
    }

    /** Returns the rate-limit bucket for a path, or null when the path is not limited. */
    private static String bucketFor(String uri) {
        if (uri.contains("/oauth2/authorization/") || uri.contains("/login/oauth2/code/")) {
            return "oidc";
        }
        if (uri.contains("/api/setup")) {
            return "setup";
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || bucketFor(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String bucket = bucketFor(request.getRequestURI());
        String key = request.getRemoteAddr() + ":" + bucket;
        if (!rateLimiter.tryAcquire(key, maxRequests, windowMillis)) {
            response.setStatus(429); // 429 Too Many Requests (not a servlet SC_ constant)
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again shortly.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
