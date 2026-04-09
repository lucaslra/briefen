package com.briefen.persistence.sqlite;

import com.briefen.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class SqliteUserEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "USER";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "main_admin", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT 0")
    private boolean mainAdmin;

    public SqliteUserEntity() {}

    public static SqliteUserEntity fromDomain(User u) {
        var entity = new SqliteUserEntity();
        entity.id = u.getId();
        entity.username = u.getUsername();
        entity.passwordHash = u.getPasswordHash();
        entity.role = u.getRole();
        entity.createdAt = u.getCreatedAt();
        entity.updatedAt = u.getUpdatedAt();
        entity.mainAdmin = u.isMainAdmin();
        return entity;
    }

    public User toDomain() {
        var u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash(passwordHash);
        u.setRole(role);
        u.setCreatedAt(createdAt);
        u.setUpdatedAt(updatedAt);
        u.setMainAdmin(mainAdmin);
        return u;
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
}
