package com.briefen.security;

import com.briefen.model.User;
import com.briefen.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests bearing an opaque Briefen session token
 * ({@code Authorization: Bearer bfn_…}), the credential minted by the OIDC flow.
 *
 * <p>Placed before {@code BasicAuthenticationFilter} so bearer tokens win when
 * present. When no Bearer header is present (or the token is invalid) this
 * filter does nothing and lets the HTTP Basic filter handle the request —
 * keeping password auth working unchanged (hybrid auth).
 */
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService authSessionService;

    public BearerTokenAuthenticationFilter(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String rawToken = extractBearerToken(request);
        if (rawToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> user = authSessionService.resolveByRawToken(rawToken);
            user.ifPresent(u -> authenticate(u, request));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(User user, HttpServletRequest request) {
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());
        var principal = new BriefenUserDetails(
                user.getId(), user.getUsername(), user.getPasswordHash(), List.of(authority));
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
