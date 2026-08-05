package com.briefen.security;

import com.briefen.config.OidcProperties;
import com.briefen.exception.AuthReasonException;
import com.briefen.model.User;
import com.briefen.service.AuthSessionService;
import com.briefen.service.OidcUserResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * On a successful OIDC login: resolves (links or provisions) the Briefen user,
 * mints an opaque bearer-token session, and hands it to the SPA by redirecting to
 * the app root with the token in the URL <b>fragment</b> (never the query string,
 * so it is not sent to the server, logged, or leaked via {@code Referer}).
 *
 * <p>Controlled failures ({@link AuthReasonException}) redirect to
 * {@code /?auth_error=<reason>} so the login screen can show a friendly message.
 */
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OidcAuthenticationSuccessHandler.class);

    private final OidcProperties props;
    private final OidcUserResolver resolver;
    private final AuthSessionService authSessionService;

    public OidcAuthenticationSuccessHandler(OidcProperties props,
                                            OidcUserResolver resolver,
                                            AuthSessionService authSessionService) {
        this.props = props;
        this.resolver = resolver;
        this.authSessionService = authSessionService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String base = contextRoot(request);
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            log.warn("OIDC success handler received a non-OIDC principal: {}", authentication.getPrincipal());
            response.sendRedirect(base + "?auth_error=server_error");
            return;
        }

        boolean mobile = isMobileLogin(request);
        clearMobileMarker(response);

        try {
            OidcUserResolver.Claims claims = extractClaims(oidcUser);
            User user = resolver.resolve(claims);
            String token = authSessionService.issueToken(user, "oidc");
            String params = buildParams(token, user);
            log.info("OIDC login succeeded for user '{}' ({})", user.getUsername(), mobile ? "mobile" : "web");
            if (mobile) {
                // Custom-scheme deep link into the app; query params are captured by the
                // native app (not sent to any server), so they don't leak.
                response.sendRedirect(props.getMobileScheme() + "://auth?" + params);
            } else {
                // Web: fragment keeps the token out of the query string, logs, and Referer.
                response.sendRedirect(base + "#" + params);
            }
        } catch (AuthReasonException e) {
            log.info("OIDC login rejected: {}", e.getReason());
            response.sendRedirect(base + "?auth_error=" + enc(e.getReason()));
        } catch (RuntimeException e) {
            log.error("OIDC login failed unexpectedly", e);
            response.sendRedirect(base + "?auth_error=server_error");
        }
    }

    private OidcUserResolver.Claims extractClaims(OidcUser oidcUser) {
        String username = oidcUser.getClaimAsString(props.getUsernameClaim());
        if (username == null || username.isBlank()) {
            username = oidcUser.getPreferredUsername();
        }
        String email = oidcUser.getEmail();
        if ((username == null || username.isBlank()) && email != null) {
            username = email;
        }
        List<String> groups = oidcUser.getClaimAsStringList(props.getGroupsClaim());
        String issuer = oidcUser.getIssuer() != null ? oidcUser.getIssuer().toString() : null;
        boolean emailVerified = Boolean.TRUE.equals(oidcUser.getEmailVerified());

        return new OidcUserResolver.Claims(
                issuer, oidcUser.getSubject(), username, email, emailVerified, groups);
    }

    private String buildParams(String token, User user) {
        return "sso_token=" + enc(token)
                + "&uid=" + enc(user.getId())
                + "&role=" + enc(user.getRole())
                + "&username=" + enc(user.getUsername());
    }

    private static boolean isMobileLogin(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (Cookie c : request.getCookies()) {
            if (MobileClientMarkerFilter.COOKIE_NAME.equals(c.getName())) {
                return MobileClientMarkerFilter.MOBILE.equals(c.getValue());
            }
        }
        return false;
    }

    private void clearMobileMarker(HttpServletResponse response) {
        ResponseCookie cleared = ResponseCookie.from(MobileClientMarkerFilter.COOKIE_NAME, "")
                .path("/").httpOnly(true).sameSite("Lax").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    }

    /** Context-path root the SPA is served from, e.g. "/" or "/briefen/". */
    private static String contextRoot(HttpServletRequest request) {
        String ctx = request.getContextPath();
        if (ctx == null || ctx.isEmpty() || "/".equals(ctx)) {
            return "/";
        }
        return ctx.endsWith("/") ? ctx : ctx + "/";
    }

    private static String enc(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}
