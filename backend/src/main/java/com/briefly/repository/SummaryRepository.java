package com.briefly.repository;

import com.briefly.model.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SummaryRepository extends MongoRepository<Summary, String> {

    Optional<Summary> findByUrl(String url);

    Page<Summary> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
