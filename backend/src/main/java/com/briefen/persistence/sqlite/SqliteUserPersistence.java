package com.briefen.persistence.sqlite;

import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class SqliteUserPersistence implements UserPersistence {

    private final SqliteUserRepository repository;

    public SqliteUserPersistence(SqliteUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return repository.findById(id).map(SqliteUserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(SqliteUserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return repository.findAll().stream().map(SqliteUserEntity::toDomain).toList();
    }

    @Override
    public User save(User user) {
        return repository.save(SqliteUserEntity.fromDomain(user)).toDomain();
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
}
