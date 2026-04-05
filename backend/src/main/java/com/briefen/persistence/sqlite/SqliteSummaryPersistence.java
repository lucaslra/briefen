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
    public Optional<Summary> findByUrl(String url) {
        return repository.findByUrl(url).map(SqliteSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Summary> findById(String id) {
        return repository.findById(id).map(SqliteSummaryEntity::toDomain);
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
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Summary> findAll(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(SqliteSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Summary> findAll(int page, int size, String filter, String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<SqliteSummaryEntity> spec = buildSpec(filter, search);
        return repository.findAll(spec, pageable).map(SqliteSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Summary> findAll(String filter, String search) {
        Specification<SqliteSummaryEntity> spec = buildSpec(filter, search);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return repository.findAll(spec, sort).stream()
                .map(SqliteSummaryEntity::toDomain).toList();
    }

    @Override
    public long markAllAsRead() {
        return repository.markAllAsRead();
    }

    @Override
    public long markAllAsUnread() {
        return repository.markAllAsUnread();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread() {
        return repository.countUnread();
    }

    private Specification<SqliteSummaryEntity> buildSpec(String filter, String search) {
        Specification<SqliteSummaryEntity> spec = (root, query, cb) -> cb.conjunction();

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
