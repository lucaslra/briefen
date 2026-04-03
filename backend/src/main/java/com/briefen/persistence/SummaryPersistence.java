package com.briefen.persistence;

import com.briefen.model.Summary;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface SummaryPersistence {

    Optional<Summary> findByUrl(String url);

    Optional<Summary> findById(String id);

    Summary save(Summary summary);

    boolean existsById(String id);

    void deleteById(String id);

    Page<Summary> findAll(int page, int size);

    Page<Summary> findAll(int page, int size, String filter, String search);

    List<Summary> findAll(String filter, String search);

    long markAllAsRead();

    long markAllAsUnread();

    long countUnread();
}
