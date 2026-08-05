package com.briefen.model;

import java.time.Instant;

public class User {

    private String id;
    private String username;
    private String passwordHash;
    private String role; // "USER" or "ADMIN"
    private Instant createdAt;
    private Instant updatedAt;
    private boolean mainAdmin; // true only for the bootstrap/setup admin — never deletable

    // OIDC / SSO identity. Null for password-only accounts.
    private String email;       // from the `email` claim; used for account linking
    private String oidcIssuer;  // set for SSO-linked accounts
    private String oidcSubject; // stable per-user id from the identity provider

    public User() {}

    public User(String id, String username, String passwordHash, String role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isMainAdmin() { return mainAdmin; }
    public void setMainAdmin(boolean mainAdmin) { this.mainAdmin = mainAdmin; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOidcIssuer() { return oidcIssuer; }
    public void setOidcIssuer(String oidcIssuer) { this.oidcIssuer = oidcIssuer; }

    public String getOidcSubject() { return oidcSubject; }
    public void setOidcSubject(String oidcSubject) { this.oidcSubject = oidcSubject; }

    /** True when this account is linked to an external OIDC identity. */
    public boolean isOidcLinked() { return oidcSubject != null && !oidcSubject.isBlank(); }
}
