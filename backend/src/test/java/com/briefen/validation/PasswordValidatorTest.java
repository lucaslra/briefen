package com.briefen.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    @Test
    void shouldAcceptValidPassword() {
        List<String> errors = validator.validate("Str0ng!Pass");
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldRejectNull() {
        List<String> errors = validator.validate(null);
        assertThat(errors).contains("Password must be at least 8 characters long");
    }

    @Test
    void shouldRejectTooShort() {
        List<String> errors = validator.validate("Ab1!xyz");
        assertThat(errors).anyMatch(e -> e.contains("at least 8 characters"));
    }

    @Test
    void shouldRejectMissingUppercase() {
        List<String> errors = validator.validate("str0ng!pass");
        assertThat(errors).anyMatch(e -> e.contains("uppercase"));
        assertThat(errors).noneMatch(e -> e.contains("lowercase"));
    }

    @Test
    void shouldRejectMissingLowercase() {
        List<String> errors = validator.validate("STR0NG!PASS");
        assertThat(errors).anyMatch(e -> e.contains("lowercase"));
        assertThat(errors).noneMatch(e -> e.contains("uppercase"));
    }

    @Test
    void shouldRejectMissingDigit() {
        List<String> errors = validator.validate("Strong!Pass");
        assertThat(errors).anyMatch(e -> e.contains("digit"));
    }

    @Test
    void shouldRejectMissingSpecialCharacter() {
        List<String> errors = validator.validate("Str0ngPass1");
        assertThat(errors).anyMatch(e -> e.contains("special character"));
    }

    @Test
    void shouldReturnMultipleErrors() {
        List<String> errors = validator.validate("short");
        // Missing: length, uppercase, digit, special
        assertThat(errors).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldAcceptMinimumValidPassword() {
        // Exactly 8 chars, has upper, lower, digit, special
        List<String> errors = validator.validate("Aa1!xxxx");
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldRejectPasswordExceedingMaxLength() {
        String tooLong = "Aa1!" + "x".repeat(125); // 129 chars
        List<String> errors = validator.validate(tooLong);
        assertThat(errors).anyMatch(e -> e.contains("128"));
    }

    @Test
    void shouldAcceptPasswordAtMaxLength() {
        String atMax = "Aa1!" + "x".repeat(124); // exactly 128 chars
        List<String> errors = validator.validate(atMax);
        assertThat(errors).isEmpty();
    }
}
