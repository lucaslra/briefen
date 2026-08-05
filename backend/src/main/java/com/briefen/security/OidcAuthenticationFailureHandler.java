package com.briefen.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redirects failed OIDC logins to {@code /?auth_error=<reason>} so the login
 * screen can show a friendly message, mapping provider/OAuth2 error codes to a
 * small set of stable reasons. Never leaks internal exception detail to the URL.
 */
public class OidcAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OidcAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String reason = reasonFor(exception);
        log.info("OIDC login failed: {} ({})", reason, exception.getMessage());
        String base = contextRoot(request);
        response.sendRedirect(base + "?auth_error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8));
    }

    private String reasonFor(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oae) {
            String code = oae.getError() != null ? oae.getError().getErrorCode() : null;
            if (code == null || code.isBlank()) {
                return "provider_error";
            }
            return switch (code) {
                case "invalid_token", "invalid_id_token" -> "invalid_token";
                case "access_denied" -> "access_denied";
                case "authorization_request_not_found" -> "missing_state";
                case "invalid_state" -> "state_mismatch";
                default -> "exchange_failed";
            };
        }
        return "server_error";
    }

    private static String contextRoot(HttpServletRequest request) {
        String ctx = request.getContextPath();
        if (ctx == null || ctx.isEmpty() || "/".equals(ctx)) {
            return "/";
        }
        return ctx.endsWith("/") ? ctx : ctx + "/";
    }
}
