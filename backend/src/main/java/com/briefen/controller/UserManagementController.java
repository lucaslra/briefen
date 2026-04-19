package com.briefen.controller;

import com.briefen.dto.UserDto;
import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import com.briefen.security.BriefenUserDetails;
import com.briefen.validation.PasswordValidator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Exposes information about the currently authenticated user, and
 * admin-only endpoints for managing all users.
 */
@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserPersistence userPersistence;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    public UserManagementController(UserPersistence userPersistence, PasswordEncoder passwordEncoder,
                                    PasswordValidator passwordValidator) {
        this.userPersistence = userPersistence;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }

    /** Returns the currently authenticated user (including their role). */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        return userPersistence.findById(userDetails.userId())
                .map(UserDto::from)
                .orElseGet(() -> {
                    String role = userDetails.authorities().stream()
                            .findFirst()
                            .map(a -> a.getAuthority().replace("ROLE_", ""))
                            .orElse("USER");
                    return new UserDto(userDetails.userId(), userDetails.username(), role, null, false);
                });
    }

    /** Lists all users. Admin only. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> listUsers() {
        return userPersistence.findAll().stream()
                .map(UserDto::from)
                .toList();
    }

    /** Creates a new user. Admin only. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@RequestBody CreateUserRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        var passwordErrors = passwordValidator.validate(request.password());
        if (!passwordErrors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", passwordErrors));
        }
        if (userPersistence.existsByUsername(request.username().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        String role = "ADMIN".equalsIgnoreCase(request.role()) ? "ADMIN" : "USER";
        var user = new User(UUID.randomUUID().toString(), request.username().trim(),
                passwordEncoder.encode(request.password()), role);
        return UserDto.from(userPersistence.save(user));
    }

    /** Deletes a user by ID. Admin only. Cannot delete yourself, the main admin, or the sole remaining admin. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String id,
                           @AuthenticationPrincipal BriefenUserDetails userDetails) {
        if (id.equals(userDetails.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        }
        var target = userPersistence.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (target.isMainAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete the main admin account");
        }
        if ("ADMIN".equals(target.getRole()) && userPersistence.countByRole("ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete the sole remaining admin account");
        }
        userPersistence.deleteById(id);
    }

    public record CreateUserRequest(String username, String password, String role) {}
}
