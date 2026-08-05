package com.briefen.service;

import com.briefen.model.AuthSession;
import com.briefen.model.User;
import com.briefen.persistence.AuthSessionPersistence;
import com.briefen.persistence.UserPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Mints, verifies, and revokes opaque bearer-token sessions.
 *
 * <p>Tokens look like {@code bfn_<base64url(32 random bytes)>}. Only the
 * SHA-256 hash is persisted; the plaintext is returned once at creation and
 * never stored or logged. This is the credential the OIDC flow hands to
 * clients, and it authenticates alongside HTTP Basic Auth.
 */
@Service
public class AuthSessionService {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionService.class);

    /** Opaque token prefix (Briefen). Mirrors the "mt_"/"gh_"-style convention. */
    static final String TOKEN_PREFIX = "bfn_";

    /** Only refresh last_used_at when it is older than this, to avoid a write per request. */
    private static final Duration LAST_USED_THROTTLE = Duration.ofMinutes(5);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    private final AuthSessionPersistence sessions;
    private final UserPersistence users;
    private final Duration ttl;
    private final Duration apiTokenTtl;

    public AuthSessionService(AuthSessionPersistence sessions,
                              UserPersistence users,
                              @Value("${briefen.session.ttl:30d}") Duration ttl,
                              @Value("${briefen.session.api-token-ttl:3650d}") Duration apiTokenTtl) {
        this.sessions = sessions;
        this.users = users;
        this.ttl = ttl;
        this.apiTokenTtl = apiTokenTtl;
    }

    /** A newly-created personal access token: the persisted session plus its one-time plaintext. */
    public record IssuedApiToken(AuthSession session, String plaintext) {}

    /**
     * Issues a new session for the given user and returns the plaintext token.
     * The caller must hand this to the client immediately — it cannot be recovered.
     *
     * @param source "oidc" or "password"
     */
    public String issueToken(User user, String source) {
        String rawToken = generateToken();
        Instant now = Instant.now();

        var session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setTokenHash(hashToken(rawToken));
        session.setSource(source);
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAt(now.plus(ttl));
        sessions.save(session);

        log.debug("Issued {} session for user {} (expires {})", source, user.getId(), session.getExpiresAt());
        return rawToken;
    }

    /**
     * Resolves a raw bearer token to its user, or empty when the token is
     * unknown or expired. Refreshes last_used_at opportunistically.
     */
    public Optional<User> resolveByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String hash = hashToken(rawToken);
        Optional<AuthSession> found = sessions.findByTokenHash(hash);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        AuthSession session = found.get();

        Instant now = Instant.now();
        if (session.isExpired(now)) {
            // Opportunistically clean up the expired row.
            sessions.deleteByTokenHash(hash);
            return Optional.empty();
        }

        Optional<User> user = users.findById(session.getUserId());
        if (user.isEmpty()) {
            // Orphaned session (user deleted) — remove it.
            sessions.deleteByTokenHash(hash);
            return Optional.empty();
        }

        touchLastUsed(session, now);
        return user;
    }

    /**
     * Creates a long-lived personal access token (source {@code "api"}) for headless
     * or browser-extension use. The plaintext is returned once and never recoverable.
     */
    public IssuedApiToken issueApiToken(String userId, String name) {
        String rawToken = generateToken();
        Instant now = Instant.now();

        var session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTokenHash(hashToken(rawToken));
        session.setSource("api");
        session.setName((name != null && !name.isBlank()) ? name.trim() : "token");
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAt(now.plus(apiTokenTtl));
        AuthSession saved = sessions.save(session);
        log.info("Issued API token '{}' for user {}", saved.getName(), userId);
        return new IssuedApiToken(saved, rawToken);
    }

    /** Lists a user's personal access tokens (never includes token material). */
    public java.util.List<AuthSession> listApiTokens(String userId) {
        return sessions.findByUserIdAndSource(userId, "api");
    }

    /** Revokes a personal access token by id, scoped to its owner. Returns false if not found. */
    public boolean revokeApiToken(String userId, String id) {
        var found = sessions.findById(id);
        if (found.isPresent()
                && userId.equals(found.get().getUserId())
                && "api".equals(found.get().getSource())) {
            sessions.deleteById(id);
            return true;
        }
        return false;
    }

    /** Revokes the session identified by the raw bearer token. No-op when absent. */
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessions.deleteByTokenHash(hashToken(rawToken));
    }

    /** Removes all sessions for a user (e.g. on account deletion). */
    public void revokeAllForUser(String userId) {
        sessions.deleteByUserId(userId);
    }

    /** Deletes expired sessions; returns how many were removed. */
    public long purgeExpired() {
        return sessions.deleteExpired(Instant.now());
    }

    private void touchLastUsed(AuthSession session, Instant now) {
        Instant last = session.getLastUsedAt();
        if (last == null || last.plus(LAST_USED_THROTTLE).isBefore(now)) {
            session.setLastUsedAt(now);
            try {
                sessions.save(session);
            } catch (RuntimeException e) {
                // A best-effort timestamp update must never fail the request.
                log.debug("Failed to update last_used_at for session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return TOKEN_PREFIX + B64URL.encodeToString(bytes);
    }

    /** SHA-256 hex of the token. Package-visible for tests. */
    static String hashToken(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
