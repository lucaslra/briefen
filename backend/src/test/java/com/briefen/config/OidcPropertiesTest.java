package com.briefen.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OidcPropertiesTest {

    @Test
    void isEnabled_falseWhenIssuerBlank() {
        var props = new OidcProperties();
        assertThat(props.isEnabled()).isFalse();

        props.setIssuer("  ");
        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_trueWhenIssuerSet() {
        var props = new OidcProperties();
        props.setIssuer("https://sso.example.com");
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void resolvedScopes_alwaysIncludesOpenidAndDedupes() {
        var props = new OidcProperties();
        props.setScopes(List.of("profile", "email", "profile"));
        assertThat(props.resolvedScopes()).containsExactly("openid", "profile", "email");
    }

    @Test
    void resolvedScopes_handlesNullScopes() {
        var props = new OidcProperties();
        props.setScopes(null);
        assertThat(props.resolvedScopes()).containsExactly("openid");
    }

    @Test
    void passwordLoginEnabled_trueByDefault() {
        var props = new OidcProperties();
        assertThat(props.isPasswordLoginEnabled()).isTrue();
    }

    @Test
    void passwordLoginDisabled_ignoredWhenOidcNotEnabled() {
        var props = new OidcProperties();
        props.setDisablePasswordLogin(true);
        // No issuer → OIDC disabled → flag ignored so nobody is locked out.
        assertThat(props.isPasswordLoginEnabled()).isTrue();
    }

    @Test
    void passwordLoginDisabled_honoredWhenOidcEnabled() {
        var props = new OidcProperties();
        props.setIssuer("https://sso.example.com");
        props.setDisablePasswordLogin(true);
        assertThat(props.isPasswordLoginEnabled()).isFalse();
    }
}
