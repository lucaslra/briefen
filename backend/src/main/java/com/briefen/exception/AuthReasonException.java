package com.briefen.exception;

/**
 * A controlled authentication failure whose {@code reason} is safe to surface on
 * the login page via {@code ?auth_error=<reason>} (e.g. {@code signup_disabled}).
 *
 * <p>Used by the OIDC user-resolution flow to distinguish expected, user-facing
 * outcomes from unexpected server errors.
 */
public class AuthReasonException extends RuntimeException {

    private final String reason;

    public AuthReasonException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
