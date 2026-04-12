package com.briefen.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaSummaryRepository extends JpaRepository<JpaSummaryEntity, String>,
        JpaSpecificationExecutor<JpaSummaryEntity> {

    Optional<JpaSummaryEntity> findByUrlAndUserId(String url, String userId);

    Page<JpaSummaryEntity> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Modifying
    @Query("UPDATE JpaSummaryEntity s SET s.isRead = true WHERE s.userId = :userId AND s.isRead = false")
    int markAllAsRead(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE JpaSummaryEntity s SET s.isRead = false WHERE s.userId = :userId AND s.isRead = true")
    int markAllAsUnread(@Param("userId") String userId);

    @Query("SELECT COUNT(s) FROM JpaSummaryEntity s WHERE s.userId = :userId AND s.isRead = false")
    long countUnreadByUserId(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE JpaSummaryEntity s SET s.userId = :userId WHERE s.userId IS NULL")
    int assignOrphanedSummaries(@Param("userId") String userId);
}
