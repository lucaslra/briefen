package com.briefen.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaAuthSessionRepository extends JpaRepository<JpaAuthSessionEntity, String> {

    Optional<JpaAuthSessionEntity> findByTokenHash(String tokenHash);

    java.util.List<JpaAuthSessionEntity> findByUserIdAndSourceOrderByCreatedAtDesc(String userId, String source);

    @Modifying
    @Query("delete from JpaAuthSessionEntity s where s.tokenHash = :tokenHash")
    void deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from JpaAuthSessionEntity s where s.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);

    @Modifying
    @Query("delete from JpaAuthSessionEntity s where s.expiresAt < :now")
    long deleteByExpiresAtBefore(@Param("now") Instant now);
}
