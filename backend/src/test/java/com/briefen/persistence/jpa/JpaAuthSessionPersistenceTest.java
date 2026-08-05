package com.briefen.persistence.jpa;

import com.briefen.model.AuthSession;
import com.briefen.persistence.AuthSessionPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class JpaAuthSessionPersistenceTest {

    @Autowired
    private AuthSessionPersistence persistence;

    private AuthSession build(String userId, String tokenHash, Instant expiresAt) {
        var s = new AuthSession();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId);
        s.setTokenHash(tokenHash);
        s.setSource("oidc");
        s.setCreatedAt(Instant.now());
        s.setLastUsedAt(Instant.now());
        s.setExpiresAt(expiresAt);
        return s;
    }

    @Test
    void saveAndFindByTokenHash() {
        persistence.save(build("user-1", "hash-abc", Instant.now().plus(Duration.ofDays(1))));

        Optional<AuthSession> found = persistence.findByTokenHash("hash-abc");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
        assertThat(found.get().getSource()).isEqualTo("oidc");
    }

    @Test
    void findByTokenHash_missing_returnsEmpty() {
        assertThat(persistence.findByTokenHash("nope")).isEmpty();
    }

    @Test
    void deleteByTokenHash_removesSession() {
        persistence.save(build("user-1", "hash-del", Instant.now().plus(Duration.ofDays(1))));

        persistence.deleteByTokenHash("hash-del");

        assertThat(persistence.findByTokenHash("hash-del")).isEmpty();
    }

    @Test
    void deleteByUserId_removesAllUserSessions() {
        persistence.save(build("user-2", "h1", Instant.now().plus(Duration.ofDays(1))));
        persistence.save(build("user-2", "h2", Instant.now().plus(Duration.ofDays(1))));
        persistence.save(build("user-3", "h3", Instant.now().plus(Duration.ofDays(1))));

        persistence.deleteByUserId("user-2");

        assertThat(persistence.findByTokenHash("h1")).isEmpty();
        assertThat(persistence.findByTokenHash("h2")).isEmpty();
        assertThat(persistence.findByTokenHash("h3")).isPresent();
    }

    @Test
    void deleteExpired_removesOnlyExpiredSessions() {
        persistence.save(build("user-1", "expired", Instant.now().minus(Duration.ofMinutes(1))));
        persistence.save(build("user-1", "active", Instant.now().plus(Duration.ofDays(1))));

        long removed = persistence.deleteExpired(Instant.now());

        assertThat(removed).isEqualTo(1);
        assertThat(persistence.findByTokenHash("expired")).isEmpty();
        assertThat(persistence.findByTokenHash("active")).isPresent();
    }
}
