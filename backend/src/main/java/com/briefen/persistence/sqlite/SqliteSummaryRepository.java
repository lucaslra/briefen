package com.briefen.persistence.sqlite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SqliteSummaryRepository extends JpaRepository<SqliteSummaryEntity, String>,
        JpaSpecificationExecutor<SqliteSummaryEntity> {

    Optional<SqliteSummaryEntity> findByUrl(String url);

    Page<SqliteSummaryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE SqliteSummaryEntity s SET s.isRead = true WHERE s.isRead = false")
    int markAllAsRead();

    @Modifying
    @Query("UPDATE SqliteSummaryEntity s SET s.isRead = false WHERE s.isRead = true")
    int markAllAsUnread();

    @Query("SELECT COUNT(s) FROM SqliteSummaryEntity s WHERE s.isRead = false")
    long countUnread();
}
