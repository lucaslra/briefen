package com.briefen.persistence.sqlite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SqliteUserRepository extends JpaRepository<SqliteUserEntity, String> {

    Optional<SqliteUserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
