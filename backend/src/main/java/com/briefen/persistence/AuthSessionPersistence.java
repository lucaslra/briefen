package com.briefen.persistence;

import com.briefen.model.AuthSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuthSessionPersistence {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findByTokenHash(String tokenHash);

    Optional<AuthSession> findById(String id);

    /** Lists a user's sessions of a given source (e.g. "api"), newest first. */
    List<AuthSession> findByUserIdAndSource(String userId, String source);

    void deleteByTokenHash(String tokenHash);

    void deleteById(String id);

    /** Deletes all sessions belonging to a user (e.g. on account deletion). */
    void deleteByUserId(String userId);

    /** Purges expired sessions; returns the number removed. */
    long deleteExpired(Instant now);
}
