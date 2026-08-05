package com.briefen.security;

import com.briefen.config.OidcProperties;
import com.briefen.model.User;
import com.briefen.service.AuthSessionService;
import com.briefen.service.OidcUserResolver;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OidcAuthenticationSuccessHandlerTest {

    private OidcProperties props;
    private OidcUserResolver resolver;
    private AuthSessionService authSessionService;
    private OidcAuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        props = new OidcProperties();
        props.setIssuer("https://sso.example.com");
        props.setMobileScheme("briefen");
        resolver = mock(OidcUserResolver.class);
        authSessionService = mock(AuthSessionService.class);
        handler = new OidcAuthenticationSuccessHandler(props, resolver, authSessionService);

        var user = new User("user-1", "alice", "", "USER");
        when(resolver.resolve(any())).thenReturn(user);
        when(authSessionService.issueToken(any(), eq("oidc"))).thenReturn("bfn_token123");
    }

    private OAuth2AuthenticationToken authentication() {
        var claims = Map.<String, Object>of(
                "sub", "sub-1",
                "iss", "https://sso.example.com",
                "preferred_username", "alice",
                "email", "alice@example.com",
                "email_verified", true);
        var idToken = new OidcIdToken("raw-token",
                Instant.now(), Instant.now().plusSeconds(3600), claims);
        var oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "briefen");
    }

    @Test
    void webLogin_redirectsToRootWithTokenInFragment() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        String location = response.getRedirectedUrl();
        assertThat(location).startsWith("/#sso_token=bfn_token123");
        assertThat(location).contains("uid=user-1").contains("role=USER").contains("username=alice");
        verify(authSessionService).issueToken(any(), eq("oidc"));
    }

    @Test
    void mobileLogin_redirectsToCustomSchemeWithTokenInQuery() throws Exception {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("briefen_login_client", "mobile"));
        var response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        String location = response.getRedirectedUrl();
        assertThat(location).startsWith("briefen://auth?sso_token=bfn_token123");
        assertThat(location).contains("uid=user-1").contains("role=USER").contains("username=alice");
        // The marker cookie is cleared.
        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(h -> h.startsWith("briefen_login_client=") && h.contains("Max-Age=0"));
    }
}
