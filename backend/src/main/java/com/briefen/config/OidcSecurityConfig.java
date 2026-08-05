package com.briefen.config;

import com.briefen.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.briefen.security.LazyClientRegistrationRepository;
import com.briefen.security.OidcAuthenticationFailureHandler;
import com.briefen.security.OidcAuthenticationSuccessHandler;
import com.briefen.security.StatelessOAuth2AuthorizedClientRepository;
import com.briefen.service.AuthSessionService;
import com.briefen.service.OidcUserResolver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Wires the OpenID Connect / SSO beans. Loaded only when {@code briefen.oidc.issuer}
 * is set, so a default deployment has zero OIDC surface area.
 *
 * <p>The actual {@code oauth2Login} filter is enabled in {@link SecurityConfig},
 * which injects the {@link ClientRegistrationRepository} bean via {@code ObjectProvider}
 * and only activates SSO when it is present.
 */
@Configuration
@Conditional(OidcEnabledCondition.class)
public class OidcSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(OidcSecurityConfig.class);

    private final OidcProperties props;
    private final String configuredSecret;
    private final boolean secureCookies;

    public OidcSecurityConfig(OidcProperties props,
                              @Value("${briefen.session.secret:}") String configuredSecret,
                              @Value("${briefen.cookies.secure:false}") boolean secureCookies) {
        this.props = props;
        this.configuredSecret = configuredSecret;
        this.secureCookies = secureCookies;
    }

    /** Fail fast at startup on an incomplete SSO configuration. */
    @PostConstruct
    void validate() {
        if (props.getClientId() == null || props.getClientId().isBlank()) {
            throw new IllegalStateException(
                    "BRIEFEN_OIDC_ISSUER is set but BRIEFEN_OIDC_CLIENT_ID is missing.");
        }
        if (props.getClientSecret() == null || props.getClientSecret().isBlank()) {
            throw new IllegalStateException(
                    "BRIEFEN_OIDC_ISSUER is set but BRIEFEN_OIDC_CLIENT_SECRET is missing.");
        }
        log.info("OIDC enabled (issuer={}, provider='{}', signup={})",
                props.getIssuer(), props.getProviderName(), props.isAllowSignup());
        if (!props.isPasswordLoginEnabled()) {
            log.info("Password login is DISABLED — SSO is the only sign-in method.");
        }
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new LazyClientRegistrationRepository(props);
    }

    @Bean
    AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository(hmacSecret(), secureCookies);
    }

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new StatelessOAuth2AuthorizedClientRepository();
    }

    @Bean
    OidcAuthenticationSuccessHandler oidcAuthenticationSuccessHandler(OidcUserResolver resolver,
                                                                      AuthSessionService authSessionService) {
        return new OidcAuthenticationSuccessHandler(props, resolver, authSessionService);
    }

    @Bean
    OidcAuthenticationFailureHandler oidcAuthenticationFailureHandler() {
        return new OidcAuthenticationFailureHandler();
    }

    private byte[] hmacSecret() {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        // No configured secret: generate a per-instance random key. The authz cookie
        // only lives for the ~5-minute handshake, so an ephemeral secret is acceptable
        // (a restart mid-login just makes the user click "Sign in" again).
        log.info("briefen.session.secret not set — using an ephemeral per-instance secret for the OIDC handshake cookie");
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return secret;
    }
}
