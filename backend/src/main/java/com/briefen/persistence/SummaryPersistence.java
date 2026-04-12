package com.briefen.persistence;

import com.briefen.model.Summary;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface SummaryPersistence {

    Optional<Summary> findByUrl(String userId, String url);

    Optional<Summary> findById(String userId, String id);

    Summary save(Summary summary);

    boolean existsById(String userId, String id);

    void deleteById(String userId, String id);

    Page<Summary> findAll(String userId, int page, int size);

    Page<Summary> findAll(String userId, int page, int size, String filter, String search);

    Page<Summary> findAll(String userId, int page, int size, String filter, String search, String tag);

    List<Summary> findAll(String userId, String filter, String search);

    List<Summary> findAll(String userId, String filter, String search, String tag);

    long markAllAsRead(String userId);

    long markAllAsUnread(String userId);

    long countUnread(String userId);

    /**
     * Assigns all summaries with a null userId to the given userId.
     * Used during upgrade migration from single-user to multi-user mode.
     */
    long assignOrphanedSummaries(String userId);
}
