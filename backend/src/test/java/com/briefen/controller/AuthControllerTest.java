package com.briefen.controller;

import com.briefen.model.AuthSession;
import com.briefen.security.WithMockBriefenUser;
import com.briefen.service.AuthSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockBriefenUser
    void logout_withBearerToken_revokesTheToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer bfn_session123"))
                .andExpect(status().isNoContent());

        verify(authSessionService).revokeByRawToken("bfn_session123");
    }

    @Test
    @WithMockBriefenUser
    void logout_withoutBearerToken_isNoOp() throws Exception {
        // An authenticated caller that did not present a Bearer token (e.g. Basic auth)
        // has no server-side session to revoke — logout succeeds without calling revoke.
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authSessionService, never()).revokeByRawToken(any());
    }

    @Test
    void logout_requiresAuthentication() throws Exception {
        // No authenticated principal — the endpoint is under /api/** which requires auth.
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void config_isPublic_andReportsPasswordOnlyWhenOidcDisabled() throws Exception {
        // Default test profile has no OIDC issuer configured.
        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordLogin").value(true))
                .andExpect(jsonPath("$.oidc.enabled").value(false));
    }

    private AuthSession apiSession(String id, String name) {
        var s = new AuthSession();
        s.setId(id);
        s.setUserId("test-user-id");
        s.setSource("api");
        s.setName(name);
        s.setCreatedAt(Instant.now());
        return s;
    }

    @Test
    @WithMockBriefenUser
    void createToken_returnsPlaintextOnce() throws Exception {
        var session = apiSession("tok-1", "CI");
        when(authSessionService.listApiTokens("test-user-id")).thenReturn(List.of());
        when(authSessionService.issueApiToken("test-user-id", "CI"))
                .thenReturn(new AuthSessionService.IssuedApiToken(session, "bfn_secret"));

        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"CI\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("bfn_secret"))
                .andExpect(jsonPath("$.name").value("CI"))
                .andExpect(jsonPath("$.id").value("tok-1"));
    }

    @Test
    @WithMockBriefenUser
    void createToken_rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockBriefenUser
    void listTokens_returnsTokensWithoutSecret() throws Exception {
        when(authSessionService.listApiTokens("test-user-id"))
                .thenReturn(List.of(apiSession("tok-1", "CI"), apiSession("tok-2", "Extension")));

        mockMvc.perform(get("/api/auth/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CI"))
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[1].id").value("tok-2"));
    }

    @Test
    @WithMockBriefenUser
    void revokeToken_returns204WhenFound() throws Exception {
        when(authSessionService.revokeApiToken("test-user-id", "tok-1")).thenReturn(true);

        mockMvc.perform(delete("/api/auth/tokens/tok-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockBriefenUser
    void revokeToken_returns404WhenMissing() throws Exception {
        when(authSessionService.revokeApiToken(eq("test-user-id"), any())).thenReturn(false);

        mockMvc.perform(delete("/api/auth/tokens/nope"))
                .andExpect(status().isNotFound());
    }
}
