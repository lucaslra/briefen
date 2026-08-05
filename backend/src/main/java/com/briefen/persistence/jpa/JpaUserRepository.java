package com.briefen.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<JpaUserEntity, String> {

    Optional<JpaUserEntity> findByUsername(String username);

    Optional<JpaUserEntity> findByOidcIssuerAndOidcSubject(String oidcIssuer, String oidcSubject);

    Optional<JpaUserEntity> findFirstByEmailOrderByCreatedAtAsc(String email);

    boolean existsByUsername(String username);

    long countByRole(String role);

    List<JpaUserEntity> findByRole(String role);
}
