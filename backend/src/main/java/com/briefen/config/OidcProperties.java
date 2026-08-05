package com.briefen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenID Connect / SSO configuration, bound from {@code briefen.oidc.*}.
 *
 * <p>SSO is enabled only when {@link #issuer} is set. All other fields have
 * sensible defaults mirroring the reference implementation.
 */
@ConfigurationProperties(prefix = "briefen.oidc")
public class OidcProperties {

    /** Issuer URL serving {@code /.well-known/openid-configuration}. Setting this enables SSO. */
    private String issuer;

    private String clientId;
    private String clientSecret;

    /** Absolute callback URL registered at the provider. Defaults to {@code {baseUrl}/login/oauth2/code/briefen}. */
    private String redirectUri;

    /** Requested scopes; {@code openid} is always included. */
    private List<String> scopes = List.of("openid", "profile", "email");

    /** Button label shown on the login screen. */
    private String providerName = "SSO";

    /** ID-token claim used to derive the Briefen username. */
    private String usernameClaim = "preferred_username";

    /** Claim holding the user's group memberships. */
    private String groupsClaim = "groups";

    /** Membership grants admin — synced on every login. Unset = never touch role from groups. */
    private String adminGroup;

    /** Auto-create a Briefen account on first SSO login. */
    private boolean allowSignup = true;

    /** Link an SSO identity to an existing account by verified email. */
    private boolean linkByEmail = true;

    /** Link an SSO identity to an existing account by username. */
    private boolean linkByUsername = true;

    /** SSO-only mode: hide/reject password login. Honored only when SSO is enabled. */
    private boolean disablePasswordLogin = false;

    /** Custom URL scheme the mobile app registers for the SSO callback (e.g. {@code briefen://auth}). */
    private String mobileScheme = "briefen";

    /** True when SSO is configured. */
    public boolean isEnabled() {
        return issuer != null && !issuer.isBlank();
    }

    /** Scopes with {@code openid} guaranteed present, de-duplicated, order preserved. */
    public Set<String> resolvedScopes() {
        Set<String> resolved = new LinkedHashSet<>();
        resolved.add("openid");
        if (scopes != null) {
            for (String s : scopes) {
                if (s != null && !s.isBlank()) {
                    resolved.add(s.trim());
                }
            }
        }
        return resolved;
    }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getUsernameClaim() { return usernameClaim; }
    public void setUsernameClaim(String usernameClaim) { this.usernameClaim = usernameClaim; }

    public String getGroupsClaim() { return groupsClaim; }
    public void setGroupsClaim(String groupsClaim) { this.groupsClaim = groupsClaim; }

    public String getAdminGroup() { return adminGroup; }
    public void setAdminGroup(String adminGroup) { this.adminGroup = adminGroup; }

    public boolean isAllowSignup() { return allowSignup; }
    public void setAllowSignup(boolean allowSignup) { this.allowSignup = allowSignup; }

    public boolean isLinkByEmail() { return linkByEmail; }
    public void setLinkByEmail(boolean linkByEmail) { this.linkByEmail = linkByEmail; }

    public boolean isLinkByUsername() { return linkByUsername; }
    public void setLinkByUsername(boolean linkByUsername) { this.linkByUsername = linkByUsername; }

    public boolean isDisablePasswordLogin() { return disablePasswordLogin; }
    public void setDisablePasswordLogin(boolean disablePasswordLogin) { this.disablePasswordLogin = disablePasswordLogin; }

    public String getMobileScheme() { return mobileScheme; }
    public void setMobileScheme(String mobileScheme) { this.mobileScheme = mobileScheme; }

    /** Whether password login is effectively available (disabled only when SSO is also enabled). */
    public boolean isPasswordLoginEnabled() {
        return !(disablePasswordLogin && isEnabled());
    }
}
