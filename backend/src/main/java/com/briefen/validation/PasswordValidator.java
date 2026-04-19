package com.briefen.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates passwords against the security policy.
 * Returns specific error messages for each violated rule.
 */
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    /**
     * Validates the given password against the security policy.
     *
     * @return a list of specific validation error messages; empty if the password is valid
     */
    public List<String> validate(String password) {
        var errors = new ArrayList<String>();

        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password != null && password.length() > MAX_LENGTH) {
            errors.add("Password must not exceed " + MAX_LENGTH + " characters");
        }
        if (password == null) {
            return errors;
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            errors.add("Password must contain at least one lowercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            errors.add("Password must contain at least one digit");
        }
        if (password.chars().allMatch(c -> Character.isLetterOrDigit(c))) {
            errors.add("Password must contain at least one special character");
        }
        return errors;
    }
}
