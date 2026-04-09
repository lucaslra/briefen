package com.briefen.service;

import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import com.briefen.validation.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SetupService setupService;

    @BeforeEach
    void setUp() {
        setupService = new SetupService(userPersistence, passwordEncoder, new PasswordValidator());
    }

    // ---- isSetupRequired ----

    @Test
    void shouldReturnTrueWhenNoAdminExists() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        assertThat(setupService.isSetupRequired()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenAdminExists() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(1L);
        assertThat(setupService.isSetupRequired()).isFalse();
    }

    // ---- createInitialAdmin ----

    @Test
    void shouldCreateAdminWhenSetupRequired() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        when(userPersistence.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("hashed");
        when(userPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = setupService.createInitialAdmin("admin", "Str0ng!Pass");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getRole()).isEqualTo("ADMIN");
        assertThat(result.isMainAdmin()).isTrue();
        assertThat(result.getPasswordHash()).isEqualTo("hashed");

        verify(passwordEncoder).encode("Str0ng!Pass");
    }

    @Test
    void shouldTrimUsername() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        when(userPersistence.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = setupService.createInitialAdmin("  admin  ", "Str0ng!Pass");
        assertThat(result.getUsername()).isEqualTo("admin");
    }

    @Test
    void shouldRejectWhenAdminAlreadyExists() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> setupService.createInitialAdmin("admin", "Str0ng!Pass"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already been completed");
    }

    @Test
    void shouldRejectBlankUsername() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);

        assertThatThrownBy(() -> setupService.createInitialAdmin("", "Str0ng!Pass"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username is required");
    }

    @Test
    void shouldRejectShortUsername() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);

        assertThatThrownBy(() -> setupService.createInitialAdmin("ab", "Str0ng!Pass"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least 3 characters");
    }

    @Test
    void shouldRejectWeakPassword() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);

        assertThatThrownBy(() -> setupService.createInitialAdmin("admin", "weak"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least 8 characters");

        verify(userPersistence, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userPersistence.countByRole("ADMIN")).thenReturn(0L);
        when(userPersistence.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> setupService.createInitialAdmin("admin", "Str0ng!Pass"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already taken");
    }
}
