package com.briefen.security;

import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BriefenUserDetailsServiceTest {

    private User buildUser(String id, String username, String role) {
        var u = new User(id, username, "$2a$hashed", role);
        return u;
    }

    @Test
    void shouldReturnUserDetailsWhenUserExists() {
        var persistence = mock(UserPersistence.class);
        when(persistence.findByUsername("alice")).thenReturn(Optional.of(buildUser("uid-1", "alice", "USER")));

        BriefenUserDetailsService service = new BriefenUserDetailsService(persistence);
        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("$2a$hashed");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldReturnAdminRoleForAdminUser() {
        var persistence = mock(UserPersistence.class);
        when(persistence.findByUsername("admin")).thenReturn(Optional.of(buildUser("uid-2", "admin", "ADMIN")));

        BriefenUserDetailsService service = new BriefenUserDetailsService(persistence);
        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserMissing() {
        var persistence = mock(UserPersistence.class);
        when(persistence.findByUsername("ghost")).thenReturn(Optional.empty());

        BriefenUserDetailsService service = new BriefenUserDetailsService(persistence);

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void shouldReturnBriefenUserDetailsWithCorrectUserId() {
        var persistence = mock(UserPersistence.class);
        when(persistence.findByUsername("bob")).thenReturn(Optional.of(buildUser("uid-42", "bob", "USER")));

        BriefenUserDetailsService service = new BriefenUserDetailsService(persistence);
        UserDetails details = service.loadUserByUsername("bob");

        assertThat(details).isInstanceOf(BriefenUserDetails.class);
        assertThat(((BriefenUserDetails) details).userId()).isEqualTo("uid-42");
    }
}
