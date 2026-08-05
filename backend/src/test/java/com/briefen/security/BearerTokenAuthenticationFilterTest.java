package com.briefen.security;

import com.briefen.model.User;
import com.briefen.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BearerTokenAuthenticationFilterTest {

    private AuthSessionService authSessionService;
    private BearerTokenAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        authSessionService = mock(AuthSessionService.class);
        filter = new BearerTokenAuthenticationFilter(authSessionService);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerToken_authenticatesAndSetsPrincipal() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bfn_valid");
        var response = new MockHttpServletResponse();

        var user = new User("user-9", "bob", "hash", "ADMIN");
        when(authSessionService.resolveByRawToken("bfn_valid")).thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isInstanceOf(BriefenUserDetails.class);
        BriefenUserDetails principal = (BriefenUserDetails) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo("user-9");
        assertThat(principal.username()).isEqualTo("bob");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        verify(chain).doFilter(request, response);
    }

    @Test
    void noAuthorizationHeader_leavesContextUnauthenticatedAndProceeds() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(authSessionService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void basicAuthHeader_isIgnoredByBearerFilter() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(authSessionService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidBearerToken_leavesContextUnauthenticated() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bfn_invalid");
        var response = new MockHttpServletResponse();

        when(authSessionService.resolveByRawToken("bfn_invalid")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void existingAuthentication_isNotOverwritten() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bfn_valid");
        var response = new MockHttpServletResponse();

        var existing = new UsernamePasswordAuthenticationToken("preset", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(authSessionService, never()).resolveByRawToken(any());
        verify(chain).doFilter(request, response);
    }
}
