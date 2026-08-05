package com.briefen.model;

import java.time.Instant;

/**
 * A server-issued opaque bearer-token session.
 *
 * <p>Only the SHA-256 hash of the token is persisted ({@code tokenHash}); the
 * plaintext token ({@code bfn_…}) is returned to the client exactly once at
 * creation and never stored. This is the credential OIDC logins mint and hand
 * to clients, and it coexists with HTTP Basic Auth.
 */
public class AuthSession {

    private String id;
    private String userId;
    private String tokenHash;
    private String source; // "oidc", "password", or "api" — how the session was created
    private String name;   // user-supplied label, for personal access tokens (source="api")
    private Instant createdAt;
    private Instant lastUsedAt;
    private Instant expiresAt;

    public AuthSession() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired(Instant now) {
        return expiresAt == null || now.isAfter(expiresAt);
    }
}
