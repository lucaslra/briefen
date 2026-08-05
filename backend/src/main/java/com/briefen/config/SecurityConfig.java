package com.briefen.config;

import com.briefen.security.BearerTokenAuthenticationFilter;
import com.briefen.security.MobileClientMarkerFilter;
import com.briefen.security.OidcAuthenticationFailureHandler;
import com.briefen.security.OidcAuthenticationSuccessHandler;
import com.briefen.service.AuthSessionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Hybrid authentication:
 * <ul>
 *   <li><b>HTTP Basic</b> (password login) — always on, unless SSO-only mode is enabled.</li>
 *   <li><b>Bearer token</b> — opaque session tokens minted by the OIDC flow (and usable by API clients).</li>
 *   <li><b>OIDC / SSO</b> — enabled only when {@code briefen.oidc.issuer} is set (a {@link ClientRegistrationRepository} bean is present).</li>
 * </ul>
 *
 * Authentication is required for all endpoints except {@code /actuator/health},
 * {@code /api/setup/**}, {@code /api/auth/config}, and the OAuth2 login endpoints.
 * The initial admin account is created via the browser-based setup flow
 * ({@link com.briefen.service.SetupService}) or by the first SSO login.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Returns a bare 401 for unauthenticated requests — no {@code WWW-Authenticate}
     * header (so browsers don't pop the native Basic dialog) and no redirect to the
     * IdP (so the React SPA renders its own login screen and shows the SSO button).
     */
    private static final AuthenticationEntryPoint REST_401 =
            (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthSessionService authSessionService,
            OidcProperties oidcProperties,
            @Value("${briefen.cookies.secure:false}") boolean secureCookies,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<AuthorizationRequestRepository<OAuth2AuthorizationRequest>> authorizationRequestRepository,
            ObjectProvider<OidcAuthenticationSuccessHandler> oidcSuccessHandler,
            ObjectProvider<OidcAuthenticationFailureHandler> oidcFailureHandler) throws Exception {

        // SecurityHeadersFilter already writes all security headers — disable Spring Security's
        // duplicate header writing to avoid response header conflicts.
        http.headers(headers -> headers.disable());

        // CSRF protection is not applicable: stateless REST API using Bearer/Basic auth.
        // The API has no session cookies — every request carries an Authorization header,
        // so cross-site requests cannot be authenticated by a third-party page. The OIDC
        // handshake is protected by the OAuth2 `state` parameter instead.
        // codeql[java/spring-disabled-csrf-protection]
        http.csrf(csrf -> csrf.disable());

        // Fully stateless: no HttpSession is ever created. The OIDC handshake stores its
        // authorization request in a signed cookie, not a session.
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/setup/**", "/api/setup").permitAll()
                .requestMatchers("/api/auth/config").permitAll()
                .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/favicon.ico").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
        );

        // Force a plain 401 (no IdP redirect, no browser Basic dialog) for unauthenticated access.
        http.exceptionHandling(e -> e.authenticationEntryPoint(REST_401));

        // Password login (HTTP Basic) — on unless SSO-only mode is active.
        if (oidcProperties.isPasswordLoginEnabled()) {
            http.httpBasic(basic -> basic.authenticationEntryPoint(REST_401));
        } else {
            http.httpBasic(AbstractHttpConfigurer::disable);
        }

        // OIDC login — only when a client registration is available (SSO configured).
        ClientRegistrationRepository registrations = clientRegistrationRepository.getIfAvailable();
        if (registrations != null) {
            var authzRepo = authorizationRequestRepository.getIfAvailable();
            var successHandler = oidcSuccessHandler.getIfAvailable();
            var failureHandler = oidcFailureHandler.getIfAvailable();
            http.oauth2Login(oauth -> {
                oauth.clientRegistrationRepository(registrations);
                if (authzRepo != null) {
                    oauth.authorizationEndpoint(a -> a.authorizationRequestRepository(authzRepo));
                }
                if (successHandler != null) {
                    oauth.successHandler(successHandler);
                }
                if (failureHandler != null) {
                    oauth.failureHandler(failureHandler);
                }
            });
            // Tag mobile-initiated logins (?client=mobile) before the authorization redirect,
            // so the success handler can deep-link the token back into the app.
            http.addFilterBefore(
                    new MobileClientMarkerFilter(secureCookies),
                    OAuth2AuthorizationRequestRedirectFilter.class);
        }

        // Opaque bearer-token sessions authenticate ahead of HTTP Basic. When no Bearer
        // header is present this filter is a no-op, so password (Basic) auth is unchanged.
        http.addFilterBefore(
                new BearerTokenAuthenticationFilter(authSessionService),
                BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
