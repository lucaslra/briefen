package com.briefen.persistence.jpa;

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
public class JpaSummaryPersistence implements SummaryPersistence {

    private final JpaSummaryRepository repository;

    public JpaSummaryPersistence(JpaSummaryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Summary> findByUrl(String userId, String url) {
        return repository.findByUrlAndUserId(url, userId).map(JpaSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Summary> findById(String userId, String id) {
        return repository.findById(id)
                .filter(e -> userId.equals(e.getUserId()))
                .map(JpaSummaryEntity::toDomain);
    }

    @Override
    public Summary save(Summary summary) {
        var entity = JpaSummaryEntity.fromDomain(summary);
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
                .map(JpaSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Summary> findAll(String userId, int page, int size, String filter, String search) {
        return findAll(userId, page, size, filter, search, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Summary> findAll(String userId, int page, int size, String filter, String search, String tag) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<JpaSummaryEntity> spec = buildSpec(userId, filter, search, tag);
        return repository.findAll(spec, pageable).map(JpaSummaryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Summary> findAll(String userId, String filter, String search) {
        return findAll(userId, filter, search, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Summary> findAll(String userId, String filter, String search, String tag) {
        Specification<JpaSummaryEntity> spec = buildSpec(userId, filter, search, tag);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return repository.findAll(spec, sort).stream()
                .map(JpaSummaryEntity::toDomain).toList();
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

    private Specification<JpaSummaryEntity> buildSpec(String userId, String filter, String search) {
        return buildSpec(userId, filter, search, null);
    }

    private Specification<JpaSummaryEntity> buildSpec(String userId, String filter, String search, String tag) {
        Specification<JpaSummaryEntity> spec = (root, query, cb) ->
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
                    cb.like(cb.lower(root.get("notes")), pattern, '\\'),
                    cb.like(cb.lower(root.get("url")), pattern, '\\')
            ));
        }

        if (tag != null && !tag.isBlank()) {
            String escapedTag = tag.toLowerCase().trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            // Match tag in comma-separated list: exact, start, middle, or end position
            spec = spec.and((root, query, cb) -> {
                var tagsField = cb.lower(root.get("tags"));
                return cb.or(
                        cb.equal(tagsField, escapedTag),
                        cb.like(tagsField, escapedTag + ",%", '\\'),
                        cb.like(tagsField, "%," + escapedTag, '\\'),
                        cb.like(tagsField, "%," + escapedTag + ",%", '\\')
                );
            });
        }

        return spec;
    }
}
