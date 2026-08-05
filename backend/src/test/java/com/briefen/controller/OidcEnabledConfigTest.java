package com.briefen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full context with OIDC enabled (issuer set) to verify the SSO beans
 * wire correctly and the public config endpoint advertises SSO. Discovery is lazy,
 * so no live identity provider is required for the context to start.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "briefen.oidc.issuer=https://sso.example.com",
        "briefen.oidc.client-id=briefen-test",
        "briefen.oidc.client-secret=test-secret",
        "briefen.oidc.provider-name=Acme SSO"
})
class OidcEnabledConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void config_reportsOidcEnabledWithProviderName() throws Exception {
        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordLogin").value(true))
                .andExpect(jsonPath("$.oidc.enabled").value(true))
                .andExpect(jsonPath("$.oidc.providerName").value("Acme SSO"));
    }
}
