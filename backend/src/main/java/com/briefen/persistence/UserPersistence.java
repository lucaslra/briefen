package com.briefen.persistence;

import com.briefen.model.User;

import java.util.List;
import java.util.Optional;

public interface UserPersistence {

    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    User save(User user);

    void deleteById(String id);

    boolean existsByUsername(String username);

    long count();

    long countByRole(String role);

    List<User> findByRole(String role);
}
