package com.briefen.persistence.jpa;

import com.briefen.model.AuthSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_sessions", indexes = {
        @Index(name = "idx_auth_sessions_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_auth_sessions_user_id", columnList = "user_id")
})
public class JpaAuthSessionEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "name")
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public JpaAuthSessionEntity() {}

    public static JpaAuthSessionEntity fromDomain(AuthSession s) {
        var e = new JpaAuthSessionEntity();
        e.id = s.getId();
        e.userId = s.getUserId();
        e.tokenHash = s.getTokenHash();
        e.source = s.getSource();
        e.name = s.getName();
        e.createdAt = s.getCreatedAt();
        e.lastUsedAt = s.getLastUsedAt();
        e.expiresAt = s.getExpiresAt();
        return e;
    }

    public AuthSession toDomain() {
        var s = new AuthSession();
        s.setId(id);
        s.setUserId(userId);
        s.setTokenHash(tokenHash);
        s.setSource(source);
        s.setName(name);
        s.setCreatedAt(createdAt);
        s.setLastUsedAt(lastUsedAt);
        s.setExpiresAt(expiresAt);
        return s;
    }

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
}
