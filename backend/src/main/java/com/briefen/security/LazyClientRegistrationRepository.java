package com.briefen.security;

import com.briefen.config.OidcProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;

/**
 * A {@link ClientRegistrationRepository} that performs OIDC discovery lazily on
 * first use rather than at application startup.
 *
 * <p>This avoids the well-known failure mode where a transient identity-provider
 * outage at boot crash-loops the whole app and locks out password users too.
 * If discovery fails, {@link #findByRegistrationId(String)} returns {@code null}
 * (SSO temporarily unavailable) and is retried on the next attempt; the app keeps
 * running and password login is unaffected.
 */
public class LazyClientRegistrationRepository implements ClientRegistrationRepository {

    private static final Logger log = LoggerFactory.getLogger(LazyClientRegistrationRepository.class);

    public static final String REGISTRATION_ID = "briefen";
    private static final String DEFAULT_REDIRECT = "{baseUrl}/login/oauth2/code/" + REGISTRATION_ID;

    private final OidcProperties props;
    private volatile ClientRegistration cached;

    public LazyClientRegistrationRepository(OidcProperties props) {
        this.props = props;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!REGISTRATION_ID.equals(registrationId)) {
            return null;
        }
        ClientRegistration current = cached;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cached != null) {
                return cached;
            }
            try {
                cached = build();
                log.info("OIDC discovery succeeded for issuer {}", props.getIssuer());
                return cached;
            } catch (RuntimeException e) {
                log.warn("OIDC discovery failed for issuer {} — SSO temporarily unavailable, password login unaffected: {}",
                        props.getIssuer(), e.getMessage());
                return null;
            }
        }
    }

    private ClientRegistration build() {
        String redirect = (props.getRedirectUri() != null && !props.getRedirectUri().isBlank())
                ? props.getRedirectUri()
                : DEFAULT_REDIRECT;
        return ClientRegistrations.fromIssuerLocation(props.getIssuer())
                .registrationId(REGISTRATION_ID)
                .clientId(props.getClientId())
                .clientSecret(props.getClientSecret())
                .scope(props.resolvedScopes())
                .redirectUri(redirect)
                .build();
    }
}
