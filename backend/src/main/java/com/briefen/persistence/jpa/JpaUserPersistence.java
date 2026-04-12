package com.briefen.persistence.jpa;

import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class JpaUserPersistence implements UserPersistence {

    private final JpaUserRepository repository;

    public JpaUserPersistence(JpaUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return repository.findById(id).map(JpaUserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(JpaUserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return repository.findAll().stream().map(JpaUserEntity::toDomain).toList();
    }

    @Override
    public User save(User user) {
        return repository.save(JpaUserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(String role) {
        return repository.countByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(String role) {
        return repository.findByRole(role).stream().map(JpaUserEntity::toDomain).toList();
    }
}
