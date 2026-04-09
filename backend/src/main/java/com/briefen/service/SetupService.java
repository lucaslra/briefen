package com.briefen.service;

import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import com.briefen.validation.PasswordValidator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Handles the first-run setup flow: detecting whether setup is required
 * and creating the initial admin account.
 */
@Service
public class SetupService {

    private final UserPersistence userPersistence;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    public SetupService(UserPersistence userPersistence,
                        PasswordEncoder passwordEncoder,
                        PasswordValidator passwordValidator) {
        this.userPersistence = userPersistence;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }

    /**
     * @return true if no ADMIN user exists in the database (setup is required)
     */
    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        return userPersistence.countByRole("ADMIN") == 0;
    }

    /**
     * Creates the initial admin account. Only succeeds when no admin account exists yet.
     *
     * @throws ResponseStatusException 409 if an admin already exists (race-condition safe)
     * @throws ResponseStatusException 400 if username/password validation fails
     */
    @Transactional
    public User createInitialAdmin(String username, String password) {
        // Double-check inside the transaction — guards against race conditions
        if (!isSetupRequired()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Setup has already been completed");
        }

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username must be at least 3 characters long");
        }

        List<String> passwordErrors = passwordValidator.validate(password);
        if (!passwordErrors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.join("; ", passwordErrors));
        }

        if (userPersistence.existsByUsername(trimmedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        var user = new User(UUID.randomUUID().toString(), trimmedUsername,
                passwordEncoder.encode(password), "ADMIN");
        user.setMainAdmin(true);
        return userPersistence.save(user);
    }
}
