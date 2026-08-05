package com.briefen.service;

import com.briefen.model.AuthSession;
import com.briefen.model.User;
import com.briefen.persistence.AuthSessionPersistence;
import com.briefen.persistence.UserPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthSessionServiceTest {

    private AuthSessionPersistence sessions;
    private UserPersistence users;
    private AuthSessionService service;

    private User user;

    @BeforeEach
    void setUp() {
        sessions = mock(AuthSessionPersistence.class);
        users = mock(UserPersistence.class);
        service = new AuthSessionService(sessions, users, Duration.ofDays(30), Duration.ofDays(3650));

        user = new User("user-1", "alice", "hash", "USER");
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void issueToken_returnsPrefixedTokenAndPersistsHash() {
        String token = service.issueToken(user, "oidc");

        assertThat(token).startsWith("bfn_");

        ArgumentCaptor<AuthSession> captor = ArgumentCaptor.forClass(AuthSession.class);
        verify(sessions).save(captor.capture());
        AuthSession saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getSource()).isEqualTo("oidc");
        // The plaintext token is never stored — only its SHA-256 hash.
        assertThat(saved.getTokenHash()).isEqualTo(AuthSessionService.hashToken(token));
        assertThat(saved.getTokenHash()).isNotEqualTo(token);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void resolveByRawToken_validToken_returnsUser() {
        String token = service.issueToken(user, "oidc");
        String hash = AuthSessionService.hashToken(token);

        var session = new AuthSession();
        session.setId("s1");
        session.setUserId("user-1");
        session.setTokenHash(hash);
        session.setSource("oidc");
        session.setCreatedAt(Instant.now());
        session.setLastUsedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));

        when(sessions.findByTokenHash(hash)).thenReturn(Optional.of(session));
        when(users.findById("user-1")).thenReturn(Optional.of(user));

        Optional<User> resolved = service.resolveByRawToken(token);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getId()).isEqualTo("user-1");
    }

    @Test
    void resolveByRawToken_unknownToken_returnsEmpty() {
        when(sessions.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(service.resolveByRawToken("bfn_whatever")).isEmpty();
        verify(users, never()).findById(any());
    }

    @Test
    void resolveByRawToken_expiredToken_returnsEmptyAndDeletes() {
        String token = "bfn_expired";
        String hash = AuthSessionService.hashToken(token);

        var session = new AuthSession();
        session.setTokenHash(hash);
        session.setUserId("user-1");
        session.setExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));

        when(sessions.findByTokenHash(hash)).thenReturn(Optional.of(session));

        assertThat(service.resolveByRawToken(token)).isEmpty();
        verify(sessions).deleteByTokenHash(hash);
        verify(users, never()).findById(any());
    }

    @Test
    void resolveByRawToken_orphanedSession_returnsEmptyAndDeletes() {
        String token = "bfn_orphan";
        String hash = AuthSessionService.hashToken(token);

        var session = new AuthSession();
        session.setTokenHash(hash);
        session.setUserId("ghost");
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));

        when(sessions.findByTokenHash(hash)).thenReturn(Optional.of(session));
        when(users.findById("ghost")).thenReturn(Optional.empty());

        assertThat(service.resolveByRawToken(token)).isEmpty();
        verify(sessions).deleteByTokenHash(hash);
    }

    @Test
    void resolveByRawToken_blankOrNull_returnsEmpty() {
        assertThat(service.resolveByRawToken(null)).isEmpty();
        assertThat(service.resolveByRawToken("  ")).isEmpty();
        verifyNoInteractions(sessions);
    }

    @Test
    void revokeByRawToken_deletesByHash() {
        service.revokeByRawToken("bfn_token");
        verify(sessions).deleteByTokenHash(AuthSessionService.hashToken("bfn_token"));
    }

    @Test
    void revokeByRawToken_nullIsNoOp() {
        service.revokeByRawToken(null);
        verify(sessions, never()).deleteByTokenHash(any());
    }

    @Test
    void issueApiToken_persistsNamedApiSessionAndReturnsPlaintext() {
        var issued = service.issueApiToken("user-1", "CI token");

        assertThat(issued.plaintext()).startsWith("bfn_");
        var captor = org.mockito.ArgumentCaptor.forClass(AuthSession.class);
        verify(sessions).save(captor.capture());
        AuthSession saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo("api");
        assertThat(saved.getName()).isEqualTo("CI token");
        assertThat(saved.getTokenHash()).isEqualTo(AuthSessionService.hashToken(issued.plaintext()));
        // Long-lived (api-token TTL of 3650 days).
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(3000)));
    }

    @Test
    void issueApiToken_blankNameFallsBackToDefault() {
        var issued = service.issueApiToken("user-1", "  ");
        var captor = org.mockito.ArgumentCaptor.forClass(AuthSession.class);
        verify(sessions).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("token");
    }

    @Test
    void revokeApiToken_deletesWhenOwnedApiToken() {
        var s = new AuthSession();
        s.setId("t1");
        s.setUserId("user-1");
        s.setSource("api");
        when(sessions.findById("t1")).thenReturn(Optional.of(s));

        assertThat(service.revokeApiToken("user-1", "t1")).isTrue();
        verify(sessions).deleteById("t1");
    }

    @Test
    void revokeApiToken_refusesWhenNotOwner() {
        var s = new AuthSession();
        s.setId("t1");
        s.setUserId("other-user");
        s.setSource("api");
        when(sessions.findById("t1")).thenReturn(Optional.of(s));

        assertThat(service.revokeApiToken("user-1", "t1")).isFalse();
        verify(sessions, never()).deleteById(any());
    }
}
