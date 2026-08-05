package com.briefen.dto;

import com.briefen.config.OidcProperties;

/**
 * Public description of the available sign-in methods, consumed by the login screen.
 */
public record AuthConfigResponse(boolean passwordLogin, Oidc oidc) {

    public record Oidc(boolean enabled, String providerName) {}

    public static AuthConfigResponse from(OidcProperties props) {
        Oidc oidc = props.isEnabled()
                ? new Oidc(true, props.getProviderName())
                : new Oidc(false, null);
        return new AuthConfigResponse(props.isPasswordLoginEnabled(), oidc);
    }
}
