package com.briefen.persistence;

import com.briefen.model.User;

import java.util.List;
import java.util.Optional;

public interface UserPersistence {

    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    /** Finds the account linked to the given OIDC (issuer, subject) pair, if any. */
    Optional<User> findByOidc(String issuer, String subject);

    /** Finds the (oldest) account with the given non-blank email, if any. */
    Optional<User> findByEmail(String email);

    List<User> findAll();

    User save(User user);

    void deleteById(String id);

    boolean existsByUsername(String username);

    long count();

    long countByRole(String role);

    List<User> findByRole(String role);
}
