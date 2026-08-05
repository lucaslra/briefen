package com.briefen.controller;

import com.briefen.config.OidcProperties;
import com.briefen.dto.ApiTokenResponse;
import com.briefen.dto.AuthConfigResponse;
import com.briefen.dto.CreateApiTokenRequest;
import com.briefen.dto.CreatedApiTokenResponse;
import com.briefen.security.BriefenUserDetails;
import com.briefen.service.AuthSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Authentication endpoints that are not part of the first-run setup flow:
 * the public sign-in-method descriptor and logout (bearer-session revocation).
 * The OAuth2 redirect/callback endpoints are handled by Spring Security.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService authSessionService;
    private final OidcProperties oidcProperties;

    public AuthController(AuthSessionService authSessionService, OidcProperties oidcProperties) {
        this.authSessionService = authSessionService;
        this.oidcProperties = oidcProperties;
    }

    /**
     * Public: describes which sign-in methods are available so the login screen
     * can render the password form and/or the "Sign in with SSO" button.
     */
    @GetMapping("/config")
    public AuthConfigResponse config() {
        return AuthConfigResponse.from(oidcProperties);
    }

    /**
     * Revokes the bearer-token session used to make this request. Basic-auth
     * callers have no server-side session, so this is a harmless no-op for them.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String rawToken = authorization.substring(BEARER_PREFIX.length()).trim();
            authSessionService.revokeByRawToken(rawToken);
        }
    }

    /** Maximum personal access tokens a single user may hold. */
    private static final int MAX_API_TOKENS = 20;

    /** Lists the caller's personal access tokens (no token material). */
    @GetMapping("/tokens")
    public List<ApiTokenResponse> listTokens(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        return authSessionService.listApiTokens(userDetails.userId()).stream()
                .map(ApiTokenResponse::from)
                .toList();
    }

    /**
     * Creates a personal access token for headless / browser-extension use.
     * The plaintext token is returned exactly once.
     */
    @PostMapping("/tokens")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedApiTokenResponse createToken(@AuthenticationPrincipal BriefenUserDetails userDetails,
                                               @Valid @RequestBody CreateApiTokenRequest request) {
        if (authSessionService.listApiTokens(userDetails.userId()).size() >= MAX_API_TOKENS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Token limit reached (" + MAX_API_TOKENS + "). Revoke an existing token first.");
        }
        var issued = authSessionService.issueApiToken(userDetails.userId(), request.name());
        return CreatedApiTokenResponse.from(issued.session(), issued.plaintext());
    }

    /** Revokes one of the caller's personal access tokens. */
    @DeleteMapping("/tokens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeToken(@AuthenticationPrincipal BriefenUserDetails userDetails,
                            @PathVariable String id) {
        if (!authSessionService.revokeApiToken(userDetails.userId(), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token not found");
        }
    }
}
