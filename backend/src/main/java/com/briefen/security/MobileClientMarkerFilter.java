package com.briefen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Remembers that an SSO login was initiated by the mobile app.
 *
 * <p>When the mobile client opens {@code /oauth2/authorization/briefen?client=mobile},
 * this filter drops a short-lived, {@code SameSite=Lax} cookie. After the provider
 * round-trip, {@link OidcAuthenticationSuccessHandler} reads it to decide whether to
 * hand the session token back via the app's custom URL scheme
 * ({@code briefen://auth?...}) instead of the web fragment.
 */
public class MobileClientMarkerFilter extends OncePerRequestFilter {

    static final String COOKIE_NAME = "briefen_login_client";
    static final String MOBILE = "mobile";
    private static final int MAX_AGE_SECONDS = 300;

    private final boolean secureCookies;

    public MobileClientMarkerFilter(boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().contains("/oauth2/authorization/")
                && MOBILE.equals(request.getParameter("client"))) {
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, MOBILE)
                    .path("/")
                    .httpOnly(true)
                    .secure(secureCookies)
                    .sameSite("Lax")
                    .maxAge(MAX_AGE_SECONDS)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        filterChain.doFilter(request, response);
    }
}
