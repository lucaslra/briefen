package com.briefen.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private HttpCookieOAuth2AuthorizationRequestRepository repo;

    @BeforeEach
    void setUp() {
        byte[] secret = "test-secret-0123456789-abcdefghij".getBytes();
        repo = new HttpCookieOAuth2AuthorizationRequestRepository(secret, false);
    }

    private OAuth2AuthorizationRequest sampleRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://sso.example.com/authorize")
                .clientId("briefen-client")
                .redirectUri("https://app.example.com/login/oauth2/code/briefen")
                .scopes(Set.of("openid", "profile"))
                .state("state-abc")
                .attributes(attrs -> attrs.put("registration_id", "briefen"))
                .build();
    }

    private String cookieValueFrom(MockHttpServletResponse response) {
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        String prefix = HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME + "=";
        int start = setCookie.indexOf(prefix) + prefix.length();
        int end = setCookie.indexOf(';', start);
        return setCookie.substring(start, end < 0 ? setCookie.length() : end);
    }

    @Test
    void saveThenLoad_roundTripsTheAuthorizationRequest() {
        var saveResp = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(sampleRequest(), new MockHttpServletRequest(), saveResp);

        String cookieValue = cookieValueFrom(saveResp);
        var loadReq = new MockHttpServletRequest();
        loadReq.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookieValue));

        OAuth2AuthorizationRequest loaded = repo.loadAuthorizationRequest(loadReq);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo("state-abc");
        assertThat(loaded.getClientId()).isEqualTo("briefen-client");
        assertThat(loaded.getScopes()).containsExactlyInAnyOrder("openid", "profile");
    }

    @Test
    void tamperedSignature_isRejected() {
        var saveResp = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(sampleRequest(), new MockHttpServletRequest(), saveResp);

        String cookieValue = cookieValueFrom(saveResp);
        // Flip the last character of the signature.
        char last = cookieValue.charAt(cookieValue.length() - 1);
        String tampered = cookieValue.substring(0, cookieValue.length() - 1) + (last == 'A' ? 'B' : 'A');

        var loadReq = new MockHttpServletRequest();
        loadReq.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, tampered));

        assertThat(repo.loadAuthorizationRequest(loadReq)).isNull();
    }

    @Test
    void differentSecret_cannotDecodeCookie() {
        var saveResp = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(sampleRequest(), new MockHttpServletRequest(), saveResp);
        String cookieValue = cookieValueFrom(saveResp);

        var otherRepo = new HttpCookieOAuth2AuthorizationRequestRepository(
                "a-totally-different-secret-key-000".getBytes(), false);
        var loadReq = new MockHttpServletRequest();
        loadReq.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookieValue));

        assertThat(otherRepo.loadAuthorizationRequest(loadReq)).isNull();
    }

    @Test
    void noCookie_returnsNull() {
        assertThat(repo.loadAuthorizationRequest(new MockHttpServletRequest())).isNull();
    }

    @Test
    void removeAuthorizationRequest_returnsRequestAndClearsCookie() {
        var saveResp = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(sampleRequest(), new MockHttpServletRequest(), saveResp);
        String cookieValue = cookieValueFrom(saveResp);

        var loadReq = new MockHttpServletRequest();
        loadReq.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, cookieValue));
        var removeResp = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repo.removeAuthorizationRequest(loadReq, removeResp);

        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("state-abc");
        // Cookie cleared (Max-Age=0).
        assertThat(removeResp.getHeader("Set-Cookie")).contains("Max-Age=0");
    }

    @Test
    void saveNull_clearsCookie() {
        var response = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(null, new MockHttpServletRequest(), response);
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
