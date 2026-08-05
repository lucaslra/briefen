package com.briefen.persistence.jpa;

import com.briefen.model.AuthSession;
import com.briefen.persistence.AuthSessionPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class JpaAuthSessionPersistence implements AuthSessionPersistence {

    private final JpaAuthSessionRepository repository;

    public JpaAuthSessionPersistence(JpaAuthSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        return repository.save(JpaAuthSessionEntity.fromDomain(session)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthSession> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(JpaAuthSessionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthSession> findById(String id) {
        return repository.findById(id).map(JpaAuthSessionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthSession> findByUserIdAndSource(String userId, String source) {
        return repository.findByUserIdAndSourceOrderByCreatedAtDesc(userId, source)
                .stream().map(JpaAuthSessionEntity::toDomain).toList();
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        repository.deleteByTokenHash(tokenHash);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByUserId(String userId) {
        repository.deleteByUserId(userId);
    }

    @Override
    public long deleteExpired(Instant now) {
        return repository.deleteByExpiresAtBefore(now);
    }
}
