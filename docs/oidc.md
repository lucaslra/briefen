# OpenID Connect (SSO)

Briefen can delegate authentication to any standards-compliant OpenID Connect
provider (Keycloak, Authentik, Auth0, Okta, Google, Microsoft Entra ID, …). SSO
runs **alongside** password login by default — once a user authenticates via the
provider, Briefen issues its normal bearer-token session, so the reading list,
settings, mobile app, and every other feature work unchanged.

The flow uses the **authorization-code grant with PKCE**, a signed cookie for the
handshake state (CSRF protection), and nonce + ID-token signature validation.

## Quick start

1. Register a **confidential client** at your provider with the redirect URI:

   ```
   https://briefen.example.com/login/oauth2/code/briefen
   ```

2. Set the environment variables (see the full table in
   [environment-variables.md](environment-variables.md#authentication--sso-openid-connect)):

   ```yaml
   environment:
     BRIEFEN_OIDC_ISSUER: "https://sso.example.com/realms/briefen"
     BRIEFEN_OIDC_CLIENT_ID: "briefen"
     BRIEFEN_OIDC_CLIENT_SECRET: "<client-secret>"
     BRIEFEN_OIDC_REDIRECT_URL: "https://briefen.example.com/login/oauth2/code/briefen"
     BRIEFEN_OIDC_PROVIDER_NAME: "Company SSO"
     BRIEFEN_OIDC_ADMIN_GROUP: "briefen-admins"
     BRIEFEN_SECURE_COOKIES: "true"
   ```

3. Restart. The login screen now shows **"Sign in with Company SSO"** above the
   password form.

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `BRIEFEN_OIDC_ISSUER` | *(required to enable)* | Issuer URL serving `/.well-known/openid-configuration` |
| `BRIEFEN_OIDC_CLIENT_ID` | *(required)* | OAuth2 client ID |
| `BRIEFEN_OIDC_CLIENT_SECRET` | *(required)* | OAuth2 client secret (supports `_FILE`) |
| `BRIEFEN_OIDC_REDIRECT_URL` | `{baseUrl}/login/oauth2/code/briefen` | Must match the URI registered at the provider |
| `BRIEFEN_OIDC_PROVIDER_NAME` | `SSO` | Label on the sign-in button |
| `BRIEFEN_OIDC_SCOPES` | `openid,profile,email` | Extra scopes (`openid` always added) |
| `BRIEFEN_OIDC_USERNAME_CLAIM` | `preferred_username` | Claim used to derive the username |
| `BRIEFEN_OIDC_GROUPS_CLAIM` | `groups` | Claim holding group memberships |
| `BRIEFEN_OIDC_ADMIN_GROUP` | *(unset)* | Membership grants admin — **synced every login** |
| `BRIEFEN_OIDC_ALLOW_SIGNUP` | `true` | Auto-create an account on first SSO login |
| `BRIEFEN_OIDC_LINK_BY_EMAIL` | `true` | Link to an existing account by **verified** email |
| `BRIEFEN_OIDC_LINK_BY_USERNAME` | `true` | Link to an existing account by username |
| `BRIEFEN_DISABLE_PASSWORD_LOGIN` | `false` | SSO-only mode (ignored unless SSO is configured) |
| `BRIEFEN_SECURE_COOKIES` | `false` | Mark the handshake cookie `Secure` (set `true` on HTTPS) |

## How accounts are resolved

On each SSO login Briefen maps the verified ID token to an account in this order:

1. **Already linked** — an account previously linked to this provider `sub`.
2. **Link by verified email** — if `BRIEFEN_OIDC_LINK_BY_EMAIL=true`, the token
   carries `email_verified: true`, and a local account has that email.
3. **Link by username** — if `BRIEFEN_OIDC_LINK_BY_USERNAME=true` and a local
   account's username matches the token's username claim.
4. **Just-in-time provisioning** — if `BRIEFEN_OIDC_ALLOW_SIGNUP=true`, a new
   SSO-only account is created (username from the claim, deduplicated on collision).

The **first-ever** user created via SSO is always made admin, so you can bootstrap
a fresh instance without a password admin. When `BRIEFEN_OIDC_ADMIN_GROUP` is set,
admin status is kept in sync with group membership on every login (the protected
main admin is never demoted).

> **Linking trust model:** email/username linking trusts your IdP's namespace.
> Only enable it for a provider you control. Email linking additionally requires
> `email_verified: true`. To disable auto-linking entirely, set both
> `BRIEFEN_OIDC_LINK_BY_EMAIL=false` and `BRIEFEN_OIDC_LINK_BY_USERNAME=false`.

## Behind a reverse proxy

Briefen builds the redirect URI from the incoming request. Behind Nginx, Caddy,
or Traefik, set:

```yaml
SERVER_FORWARD_HEADERS_STRATEGY: FRAMEWORK
```

so `X-Forwarded-Proto` / `X-Forwarded-Host` are honored and the callback URL uses
your external `https://` host — otherwise the provider will reject a mismatched
`redirect_uri`. Also set `BRIEFEN_SECURE_COOKIES=true` when serving over HTTPS.

## SSO-only mode

To require SSO and remove the password form entirely:

```yaml
BRIEFEN_DISABLE_PASSWORD_LOGIN: "true"
```

This is honored **only when SSO is configured** — if `BRIEFEN_OIDC_ISSUER` is
unset, the flag is ignored so you can never lock everyone out. Existing bearer
sessions and API tokens keep working.

## Browser extensions & headless clients

The Firefox/Chrome extensions and other headless clients can't perform an
interactive SSO redirect. On an SSO-only (or any) instance, create a **personal
access token** in **Settings → Access tokens**, then paste it into the
extension's *Access token* field (Options) instead of a username/password. The
extension sends it as `Authorization: Bearer bfn_…`. Revoke it any time from the
same settings screen. Token lifetime is controlled by `BRIEFEN_API_TOKEN_TTL`.

## Example: Keycloak

1. Create a realm (e.g. `briefen`) and a **confidential** client `briefen`.
2. Set the valid redirect URI to
   `https://briefen.example.com/login/oauth2/code/briefen`.
3. (Optional) Add a `groups` client scope / mapper and a `briefen-admins` group.
4. Configure Briefen:

   ```yaml
   environment:
     BRIEFEN_OIDC_ISSUER: "https://keycloak.example.com/realms/briefen"
     BRIEFEN_OIDC_CLIENT_ID: "briefen"
     BRIEFEN_OIDC_CLIENT_SECRET: "<client-secret>"
     BRIEFEN_OIDC_REDIRECT_URL: "https://briefen.example.com/login/oauth2/code/briefen"
     BRIEFEN_OIDC_PROVIDER_NAME: "Keycloak"
     BRIEFEN_OIDC_ADMIN_GROUP: "briefen-admins"
     SERVER_FORWARD_HEADERS_STRATEGY: "FRAMEWORK"
     BRIEFEN_SECURE_COOKIES: "true"
   ```

## Security notes

- ID tokens are verified for signature (JWKS), issuer, audience, expiry, and nonce.
- The authorization handshake uses PKCE (S256) and a signed, `HttpOnly`,
  `SameSite=Lax`, short-lived cookie for state — no server-side session.
- The session token handed to the browser after login is an opaque bearer token
  stored (hashed) server-side; it is delivered via the URL fragment and never
  appears in query strings, logs, or `Referer` headers.
- If the identity provider is unreachable at startup, Briefen logs a warning and
  keeps running with password login — it does not crash-loop.
