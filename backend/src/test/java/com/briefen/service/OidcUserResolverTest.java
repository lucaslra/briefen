package com.briefen.service;

import com.briefen.config.OidcProperties;
import com.briefen.exception.AuthReasonException;
import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OidcUserResolverTest {

    private OidcProperties props;
    private UserPersistence users;
    private OidcUserResolver resolver;

    @BeforeEach
    void setUp() {
        props = new OidcProperties();
        props.setIssuer("https://sso.example.com");
        users = mock(UserPersistence.class);
        resolver = new OidcUserResolver(props, users);
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Sensible defaults for a "nothing matches" world.
        when(users.findByOidc(any(), any())).thenReturn(Optional.empty());
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        when(users.findByUsername(any())).thenReturn(Optional.empty());
        when(users.count()).thenReturn(5L);
    }

    private OidcUserResolver.Claims claims(String username, String email, boolean verified, List<String> groups) {
        return new OidcUserResolver.Claims("https://sso.example.com", "sub-123", username, email, verified, groups);
    }

    private User existing(String id, String username, String role) {
        var u = new User(id, username, "hash", role);
        u.setCreatedAt(Instant.now());
        return u;
    }

    @Test
    void alreadyLinked_returnsExistingUser() {
        User linked = existing("u1", "alice", "USER");
        linked.setOidcIssuer("https://sso.example.com");
        linked.setOidcSubject("sub-123");
        when(users.findByOidc("https://sso.example.com", "sub-123")).thenReturn(Optional.of(linked));

        User result = resolver.resolve(claims("alice", "a@x.com", true, null));

        assertThat(result.getId()).isEqualTo("u1");
        verify(users, never()).count(); // did not fall through to provisioning
    }

    @Test
    void linkByVerifiedEmail_linksExistingUnlinkedAccount() {
        User byEmail = existing("u2", "bob", "USER");
        when(users.findByEmail("bob@x.com")).thenReturn(Optional.of(byEmail));

        User result = resolver.resolve(claims("bobby", "bob@x.com", true, null));

        assertThat(result.getId()).isEqualTo("u2");
        assertThat(result.getOidcSubject()).isEqualTo("sub-123");
        assertThat(result.getOidcIssuer()).isEqualTo("https://sso.example.com");
    }

    @Test
    void linkByEmail_unverifiedEmail_isSkipped_provisionsInstead() {
        User byEmail = existing("u2", "bob", "USER");
        when(users.findByEmail("bob@x.com")).thenReturn(Optional.of(byEmail));

        User result = resolver.resolve(claims("newuser", "bob@x.com", false, null));

        // email not verified → not linked to u2; a new account is provisioned
        assertThat(result.getId()).isNotEqualTo("u2");
        assertThat(result.getUsername()).isEqualTo("newuser");
    }

    @Test
    void linkByEmail_disabled_isSkipped() {
        props.setLinkByEmail(false);
        User byEmail = existing("u2", "bob", "USER");
        when(users.findByEmail(any())).thenReturn(Optional.of(byEmail));

        User result = resolver.resolve(claims("newuser", "bob@x.com", true, null));

        assertThat(result.getId()).isNotEqualTo("u2");
    }

    @Test
    void linkByEmail_existingAlreadyLinked_isSkipped() {
        User byEmail = existing("u2", "bob", "USER");
        byEmail.setOidcIssuer("https://other.example.com");
        byEmail.setOidcSubject("other-sub");
        when(users.findByEmail("bob@x.com")).thenReturn(Optional.of(byEmail));

        User result = resolver.resolve(claims("bob", "bob@x.com", true, null));

        // already linked elsewhere → do not hijack; provisions a fresh account (username deduped)
        assertThat(result.getId()).isNotEqualTo("u2");
    }

    @Test
    void linkByUsername_linksExistingUnlinkedAccount() {
        when(users.findByUsername("carol")).thenReturn(Optional.of(existing("u3", "carol", "USER")));

        User result = resolver.resolve(claims("carol", null, false, null));

        assertThat(result.getId()).isEqualTo("u3");
        assertThat(result.getOidcSubject()).isEqualTo("sub-123");
    }

    @Test
    void provision_firstEverUser_becomesAdmin() {
        when(users.count()).thenReturn(0L);

        User result = resolver.resolve(claims("dave", "d@x.com", true, null));

        assertThat(result.getRole()).isEqualTo("ADMIN");
        assertThat(result.getUsername()).isEqualTo("dave");
        assertThat(result.getPasswordHash()).isEmpty();
    }

    @Test
    void provision_nonFirstUser_becomesRegularUser() {
        when(users.count()).thenReturn(3L);

        User result = resolver.resolve(claims("erin", "e@x.com", true, null));

        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    void provision_inAdminGroup_becomesAdmin() {
        props.setAdminGroup("briefen-admins");
        when(users.count()).thenReturn(3L);

        User result = resolver.resolve(claims("frank", "f@x.com", true, List.of("briefen-admins", "staff")));

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void signupDisabled_throwsAuthReason() {
        props.setAllowSignup(false);

        assertThatThrownBy(() -> resolver.resolve(claims("grace", "g@x.com", true, null)))
                .isInstanceOf(AuthReasonException.class)
                .extracting("reason").isEqualTo("signup_disabled");
    }

    @Test
    void missingSubject_throwsAuthReason() {
        var noSubject = new OidcUserResolver.Claims("iss", "  ", "h", "h@x.com", true, null);

        assertThatThrownBy(() -> resolver.resolve(noSubject))
                .isInstanceOf(AuthReasonException.class)
                .extracting("reason").isEqualTo("missing_subject");
    }

    @Test
    void provision_dedupesUsernameOnCollision() {
        when(users.findByUsername("heidi")).thenReturn(Optional.of(existing("x", "heidi", "USER")));
        when(users.findByUsername("heidi-2")).thenReturn(Optional.empty());
        // link-by-username is on by default, but the collision target is unlinked, so it would link.
        // Disable username linking to force the dedupe path in provisioning.
        props.setLinkByUsername(false);

        User result = resolver.resolve(claims("heidi", null, false, null));

        assertThat(result.getUsername()).isEqualTo("heidi-2");
    }

    @Test
    void adminSync_demotesLinkedUser_whenNoLongerInGroup() {
        props.setAdminGroup("briefen-admins");
        User linked = existing("u1", "alice", "ADMIN");
        linked.setOidcIssuer("https://sso.example.com");
        linked.setOidcSubject("sub-123");
        when(users.findByOidc(any(), any())).thenReturn(Optional.of(linked));

        User result = resolver.resolve(claims("alice", null, false, List.of("staff")));

        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    void adminSync_neverDemotesMainAdmin() {
        props.setAdminGroup("briefen-admins");
        User linked = existing("u1", "root", "ADMIN");
        linked.setMainAdmin(true);
        linked.setOidcIssuer("https://sso.example.com");
        linked.setOidcSubject("sub-123");
        when(users.findByOidc(any(), any())).thenReturn(Optional.of(linked));

        User result = resolver.resolve(claims("root", null, false, List.of("staff")));

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void adminSync_noAdminGroupConfigured_leavesRoleUntouched() {
        // adminGroup unset (default)
        User linked = existing("u1", "alice", "ADMIN");
        linked.setOidcIssuer("https://sso.example.com");
        linked.setOidcSubject("sub-123");
        when(users.findByOidc(any(), any())).thenReturn(Optional.of(linked));

        User result = resolver.resolve(claims("alice", null, false, List.of()));

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }
}
