package com.briefen.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * A no-op {@link OAuth2AuthorizedClientRepository}.
 *
 * <p>Briefen only needs the ID token once, at login, to establish identity — it
 * never calls downstream resource servers on the user's behalf, so there is no
 * reason to persist the {@link OAuth2AuthorizedClient} (access/refresh tokens).
 * Discarding it keeps the OIDC handshake free of any {@code HttpSession}, so the
 * whole login flow stays stateless.
 */
public class StatelessOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
                                                                     Authentication principal,
                                                                     HttpServletRequest request) {
        return null;
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient,
                                     Authentication principal,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        // Intentionally discarded — see class javadoc.
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId,
                                       Authentication principal,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        // Nothing stored, nothing to remove.
    }
}
