package com.briefen.controller;

import com.briefen.persistence.UserPersistence;
import com.briefen.security.WithMockBriefenUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class SetupControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private UserPersistence userPersistence;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---- GET /api/setup/status ----

    @Test
    void shouldReturnSetupRequiredTrueWhenNoAdmin() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    void shouldReturnSetupRequiredFalseWhenAdminExists() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(1L);

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));
    }

    @Test
    void shouldAccessSetupStatusWithoutAuthentication() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);

        // No @WithMockBriefenUser — unauthenticated request
        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk());
    }

    // ---- POST /api/setup ----

    @Test
    void shouldCreateAdminAndReturn201() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        when(userPersistence.existsByUsername("admin")).thenReturn(false);
        when(userPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Str0ng!Pass1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.mainAdmin").value(true));
    }

    @Test
    void shouldRejectSetupWhenAdminAlreadyExists() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(1L);

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Str0ng!Pass1"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectWeakPassword() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"weak"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankUsername() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"Str0ng!Pass1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotReturnPasswordInResponse() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        when(userPersistence.existsByUsername("admin")).thenReturn(false);
        when(userPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Str0ng!Pass1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldAccessSetupEndpointWithoutAuthentication() throws Exception {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        when(userPersistence.existsByUsername("admin")).thenReturn(false);
        when(userPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // No @WithMockBriefenUser — unauthenticated
        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Str0ng!Pass1"}
                                """))
                .andExpect(status().isCreated());
    }

    // ---- Full flow integration ----

    @Test
    void fullSetupFlow_checkRequired_createAdmin_checkNotRequired_rejectDuplicate() throws Exception {
        // Step 1: Setup is required
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        mockMvc.perform(get("/api/setup/status"))
                .andExpect(jsonPath("$.setupRequired").value(true));

        // Step 2: Create admin
        when(userPersistence.existsByUsername("admin")).thenReturn(false);
        when(userPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Str0ng!Pass1"}
                                """))
                .andExpect(status().isCreated());

        // Step 3: Setup is no longer required
        when(userPersistence.countByRole("ADMIN")).thenReturn(1L);
        mockMvc.perform(get("/api/setup/status"))
                .andExpect(jsonPath("$.setupRequired").value(false));

        // Step 4: Duplicate setup attempt is rejected
        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin2","password":"An0ther!Pass"}
                                """))
                .andExpect(status().isConflict());
    }
}
