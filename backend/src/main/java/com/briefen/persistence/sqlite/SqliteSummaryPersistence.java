package com.briefen.persistence.sqlite;

import com.briefen.model.Summary;
import com.briefen.persistence.SummaryPersistence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class SqliteSummaryPersistence implements SummaryPersistence {

    private final SqliteSummaryRepository repository;

    public SqliteSummaryPersistence(SqliteSummaryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Summary> findByUrl(String userId, String url) {
        return repository.findByUrlAndUserId(url, userId).map(SqliteSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Summary> findById(String userId, String id) {
        return repository.findById(id)
                .filter(e -> userId.equals(e.getUserId()))
                .map(SqliteSummaryEntity::toDomain);
    }

    @Override
    public Summary save(Summary summary) {
        var entity = SqliteSummaryEntity.fromDomain(summary);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String userId, String id) {
        return repository.findById(id)
                .map(e -> userId.equals(e.getUserId()))
                .orElse(false);
    }

    @Override
    public void deleteById(String userId, String id) {
        repository.findById(id)
                .filter(e -> userId.equals(e.getUserId()))
                .ifPresent(repository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Summary> findAll(String userId, int page, int size) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(SqliteSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Summary> findAll(String userId, int page, int size, String filter, String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<SqliteSummaryEntity> spec = buildSpec(userId, filter, search);
        return repository.findAll(spec, pageable).map(SqliteSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Summary> findAll(String userId, String filter, String search) {
        Specification<SqliteSummaryEntity> spec = buildSpec(userId, filter, search);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return repository.findAll(spec, sort).stream()
                .map(SqliteSummaryEntity::toDomain).toList();
    }

    @Override
    public long markAllAsRead(String userId) {
        return repository.markAllAsRead(userId);
    }

    @Override
    public long markAllAsUnread(String userId) {
        return repository.markAllAsUnread(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return repository.countUnreadByUserId(userId);
    }

    @Override
    public long assignOrphanedSummaries(String userId) {
        return repository.assignOrphanedSummaries(userId);
    }

    private Specification<SqliteSummaryEntity> buildSpec(String userId, String filter, String search) {
        Specification<SqliteSummaryEntity> spec = (root, query, cb) ->
                cb.equal(root.get("userId"), userId);

        if ("unread".equals(filter)) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isRead")));
        } else if ("read".equals(filter)) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isRead")));
        }

        if (search != null && !search.isBlank()) {
            String escaped = search.toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, '\\'),
                    cb.like(cb.lower(root.get("summary")), pattern, '\\'),
                    cb.like(cb.lower(root.get("notes")), pattern, '\\')
            ));
        }

        return spec;
    }
}
