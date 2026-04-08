package com.briefen.controller;

import com.briefen.model.User;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@WithMockBriefenUser(userId = "test-user-id", username = "admin", role = "ADMIN")
class UserManagementControllerTest {

    private static final String TEST_USER_ID = "test-user-id";

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

    // ---- GET /api/users/me ----

    @Test
    void shouldReturnCurrentUserWithRole() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ---- GET /api/users ----

    @Test
    void shouldListAllUsersForAdmin() throws Exception {
        var user1 = new User("id-1", "alice", "hash", "USER");
        user1.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        var user2 = new User("id-2", "bob", "hash", "ADMIN");
        user2.setCreatedAt(Instant.parse("2025-01-02T00:00:00Z"));
        when(userPersistence.findAll()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[1].username").value("bob"));
    }

    @Test
    @WithMockBriefenUser(role = "USER")
    void shouldReturn403WhenNonAdminListsUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // ---- POST /api/users ----

    @Test
    void shouldCreateUserAndReturn201() throws Exception {
        when(userPersistence.existsByUsername("newuser")).thenReturn(false);
        when(userPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"secret123","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldReturn409WhenUsernameAlreadyTaken() throws Exception {
        when(userPersistence.existsByUsername("admin")).thenReturn(true);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret123","role":"USER"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn400WhenUsernameIsBlank() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"secret123","role":"USER"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPasswordIsBlank() throws Exception {
        when(userPersistence.existsByUsername(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"","role":"USER"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockBriefenUser(role = "USER")
    void shouldReturn403WhenNonAdminCreatesUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"secret123","role":"USER"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ---- DELETE /api/users/{id} ----

    @Test
    void shouldDeleteUserAndReturn204() throws Exception {
        var user = new User("other-id", "alice", "hash", "USER");
        when(userPersistence.findById("other-id")).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/users/other-id"))
                .andExpect(status().isNoContent());

        verify(userPersistence).deleteById("other-id");
    }

    @Test
    void shouldReturn400WhenDeletingSelf() throws Exception {
        mockMvc.perform(delete("/api/users/" + TEST_USER_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenDeletingMainAdmin() throws Exception {
        var mainAdmin = new User("other-id", "admin", "hash", "ADMIN");
        mainAdmin.setMainAdmin(true);
        when(userPersistence.findById("other-id")).thenReturn(Optional.of(mainAdmin));

        mockMvc.perform(delete("/api/users/other-id"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteNonMainAdminAdminUser() throws Exception {
        var adminUser = new User("other-id", "alice", "hash", "ADMIN");
        adminUser.setMainAdmin(false);
        when(userPersistence.findById("other-id")).thenReturn(Optional.of(adminUser));

        mockMvc.perform(delete("/api/users/other-id"))
                .andExpect(status().isNoContent());

        verify(userPersistence).deleteById("other-id");
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentUser() throws Exception {
        when(userPersistence.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockBriefenUser(role = "USER")
    void shouldReturn403WhenNonAdminDeletesUser() throws Exception {
        mockMvc.perform(delete("/api/users/other-id"))
                .andExpect(status().isForbidden());
    }
}
