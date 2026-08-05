# OIDC / SSO Implementation Plan for Briefen

**Status:** Proposed — **session model decided: opaque bearer tokens** (see §2)
**Reference implementation:** [Montly](../Montly) (`backend/oidc.go`, `backend/auth.go`, `frontend/src/components/LoginView.jsx`, `docs/deployment.md`)
**Author:** Planning doc — not yet implemented
**Scope:** Add optional "Sign in with SSO" via any standards-compliant OpenID Connect provider (Keycloak, Authentik, Auth0, Okta, Google, Entra ID), coexisting with the existing password login.

> This file lives at the repo root (not `docs/`) so it does **not** get published by the Jekyll Pages workflow, and `.md` changes are ignored by `ci.yml`.

---

## 1. Goal

Let a Briefen deployment delegate authentication to an external OIDC provider while keeping the current username/password login working (hybrid mode). Mirror Montly's proven account-resolution model:

1. Authorization-code flow **with PKCE**, signed-state CSRF protection, and nonce validation.
2. **Just-in-time provisioning** of new SSO users (`OIDC_ALLOW_SIGNUP`).
3. **Account linking** by verified email, then by username.
4. **Admin-group mapping** synced on every login (`OIDC_ADMIN_GROUP`).
5. First-ever SSO user is bootstrapped as admin (no password admin required).
6. Optional SSO-only mode (`DISABLE_PASSWORD_LOGIN`).

---

## 2. The core architectural gap (read this first)

Montly and Briefen have **fundamentally different session models**, and this is the single most important design constraint.

| | Montly (reference) | Briefen (today) |
|---|---|---|
| Credential transport | HMAC-signed **session cookie** (`_montly`) + `Bearer mt_…` API tokens | **HTTP Basic Auth** header on *every* request |
| Server state | Stateless signed cookie | Stateless — re-verifies bcrypt on every request |
| Where the browser keeps auth | `HttpOnly` cookie set by server | `sessionStorage.briefen_auth.authHeader` = `Basic <base64>` (read by JS) |
| Mobile | `Bearer` token in secure storage | username+password in secure storage, Dio adds `Basic` |
| OIDC fit | Callback simply **sets the session cookie** → done | ❌ No password exists after an OIDC login → **nothing to put in a Basic header** |

**Conclusion:** Briefen cannot represent an OIDC login as a Basic credential. We must introduce a **session credential** that OIDC can mint and that all clients can carry. Everything else in this plan follows from that decision.

### Decided approach: opaque **bearer-token sessions**, hybrid with Basic Auth

> **This is settled — the plan below builds only the bearer-token path.** The cookie-session alternative is kept in Appendix A purely as a rejected-option record.

Introduce a server-issued **opaque bearer token** (`bfn_<random>`), stored **hashed** in a new `auth_sessions` table (exactly like Montly's API tokens). Add a Spring Security filter that accepts `Authorization: Bearer <token>` *alongside* the existing HTTP Basic filter. OIDC login mints one of these tokens and hands it to the client.

**Why bearer tokens (not cookies) for Briefen:**

- The frontend already stores an opaque `authHeader` string in `sessionStorage` and attaches it to every `apiFetch`. **`authHeader` becomes `'Bearer <token>'` instead of `'Basic <base64>'` — the entire existing plumbing is reused unchanged.** (`frontend/src/apiFetch.js` already sends whatever `authHeader` holds.)
- Mobile Dio and both browser extensions also already attach an `Authorization` header — a Bearer token drops into the same slot.
- Keeps the backend stateless in spirit (token is opaque + hashed, no server session object graph), matching Briefen's design philosophy.
- Avoids CORS/cookie complications for the Firefox/Chrome extensions and the mobile app.

**Accepted trade-off vs. cookies:** a JS-readable token in `sessionStorage` is not `HttpOnly`, so it is exposed to XSS. This is *no worse than today* (the Basic header is already JS-readable in `sessionStorage`), and Briefen's strict CSP (`SecurityHeadersFilter`) mitigates XSS. We accept this trade-off in exchange for a uniform header-on-every-request model across web, mobile, and extensions, and document the exposure in the security checklist (§11).

---

## 3. Reference mapping: Montly → Briefen

| Montly (Go, hand-rolled) | Briefen (Spring Boot) |
|---|---|
| `loadOIDCConfig()` from env | `OidcProperties` (`@ConfigurationProperties(prefix="briefen.oidc")`) + `spring.security.oauth2.client.*` |
| `go-oidc` discovery + `provider.Verifier` | **Spring Security OAuth2 Client** (`spring-boot-starter-oauth2-client`) — discovery via `issuer-uri`, PKCE, nonce, JWKS verification all built in |
| HMAC-signed state cookie (state+nonce+PKCE) | Cookie-based `AuthorizationRequestRepository` (stateless) — Appendix B |
| `OIDCLogin` handler | `GET /oauth2/authorization/briefen` (Spring built-in) |
| `OIDCCallback` handler | `GET /login/oauth2/code/briefen` (Spring built-in) → custom `AuthenticationSuccessHandler` |
| `resolveOIDCUser()` (link/provision priority) | `OidcUserResolver` service — **port this logic almost verbatim** |
| `applyAdminSync()` | `OidcUserResolver.applyAdminSync()` |
| `uniqueUsername()` | `OidcUserResolver.uniqueUsername()` |
| `users.oidc_issuer` / `oidc_subject` columns | New columns on `JpaUserEntity` + `User` model + `email` |
| `GetUserByOIDC/Email/UsernameFull`, `LinkOIDCIdentity`, `CreateOIDCUser`, `SetUserAdmin` | New methods on `UserPersistence` + `JpaUserPersistence` |
| Session cookie issued on callback | Mint `auth_sessions` bearer token; redirect to SPA with token in **URL fragment** |
| `authConfigResponse` / `GET /auth/config` | `GET /api/auth/config` (public) → `{ passwordLogin, oidc: { enabled, providerName } }` |
| `DISABLE_PASSWORD_LOGIN` | `BRIEFEN_DISABLE_PASSWORD_LOGIN` (honored only when OIDC enabled) |
| `redirectAuthError(?auth_error=…)` | Same query param, read by `Login.jsx` |
| `LoginView` SSO button + error map | `Login.jsx` SSO button + `strings.js` error map |
| `e2e/mock-oidc/server.mjs` + `06-oidc.spec.ts` | Reuse the same mock IdP node script under `e2e/` |

**Design principle:** let Spring Security's mature OAuth2 client handle the protocol mechanics (discovery, PKCE, state, nonce, ID-token signature/aud/exp verification). Port only Montly's **account-resolution business logic**, which is the genuinely valuable, portable part.

---

## 4. Data model changes

### 4.1 `User` / `JpaUserEntity` (`model/User.java`, `persistence/jpa/JpaUserEntity.java`)

Add three nullable columns:

```java
private String email;        // from the `email` claim; used for linking
private String oidcIssuer;   // set for SSO-linked accounts
private String oidcSubject;  // stable per-user id from the IdP
```

- `JpaUserEntity`: `@Column(name = "email")`, `@Column(name = "oidc_issuer")`, `@Column(name = "oidc_subject")` — all nullable. Update `fromDomain`/`toDomain`.
- Unique index on `(oidc_issuer, oidc_subject)` where `oidc_subject` is not null. Hibernate `ddl-auto: update` adds the columns automatically; for SQLite add an idempotent `ALTER TABLE users ADD COLUMN …` + `CREATE UNIQUE INDEX …` guard to **`SchemaInitializer`** (mirrors existing SQLite column-migration pattern). Postgres picks it up via ddl-auto.
- Password-less SSO users store `passwordHash = ""` (empty). `passwordHash` column is currently `nullable = false` — keep it non-null, store `""`. Basic auth then simply never matches (bcrypt on `""` fails), which is the desired behavior.

### 4.2 New `auth_sessions` table (new JPA entity)

```
auth_sessions
  id           TEXT PRIMARY KEY (uuid)
  user_id      TEXT NOT NULL  (FK users.id, indexed)
  token_hash   TEXT NOT NULL UNIQUE  (sha256 hex of the opaque token)
  source       TEXT NOT NULL  ('oidc' | 'password')  -- future-proofing
  created_at   TIMESTAMP NOT NULL
  last_used_at TIMESTAMP
  expires_at   TIMESTAMP NOT NULL
```

- New `AuthSessionPersistence` interface + `JpaAuthSessionPersistence` + `JpaAuthSessionEntity` + repository, following the existing persistence pattern exactly.
- Token format: `bfn_` + 32 random bytes base64url (like Montly's `mt_`). Store only `sha256hex(token)`. Never log the plaintext.
- TTL: default 30 days (`BRIEFEN_SESSION_TTL`), sliding via `last_used_at` update (async, like Montly's `UpdateTokenLastUsed`).
- Cleanup: a `@Scheduled` sweep deletes expired rows daily (or lazy-delete on lookup miss).

> **Design note:** the table is deliberately generic (`source`) so a future "remember me" for password logins can reuse it. Out of scope for this plan but the shape supports it.

---

## 5. Backend implementation

### 5.1 Dependencies (`backend/pom.xml`)

Add:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```
This pulls in Spring Security's OAuth2/OIDC client (Nimbus JOSE for JWT verification). No other new deps.

### 5.2 Configuration

**`OidcProperties`** (`config/OidcProperties.java`) — `@ConfigurationProperties(prefix = "briefen.oidc")`:

| Property | Env var | Default | Notes |
|---|---|---|---|
| `enabled` (derived) | — | `issuer != null` | SSO on when issuer set |
| `issuer` | `BRIEFEN_OIDC_ISSUER` | *(unset)* | enables SSO |
| `clientId` | `BRIEFEN_OIDC_CLIENT_ID` | — | required when enabled |
| `clientSecret` | `BRIEFEN_OIDC_CLIENT_SECRET` | — | required when enabled |
| `redirectUri` | `BRIEFEN_OIDC_REDIRECT_URL` | — | `https://host/login/oauth2/code/briefen` |
| `scopes` | `BRIEFEN_OIDC_SCOPES` | `openid,profile,email` | `openid` always added |
| `providerName` | `BRIEFEN_OIDC_PROVIDER_NAME` | `SSO` | button label |
| `usernameClaim` | `BRIEFEN_OIDC_USERNAME_CLAIM` | `preferred_username` | |
| `groupsClaim` | `BRIEFEN_OIDC_GROUPS_CLAIM` | `groups` | |
| `adminGroup` | `BRIEFEN_OIDC_ADMIN_GROUP` | *(unset)* | membership ⇒ admin, synced every login |
| `allowSignup` | `BRIEFEN_OIDC_ALLOW_SIGNUP` | `true` | JIT provisioning |
| `linkByEmail` | `BRIEFEN_OIDC_LINK_BY_EMAIL` | `true` | requires `email_verified` |
| `linkByUsername` | `BRIEFEN_OIDC_LINK_BY_USERNAME` | `true` | |
| `disablePasswordLogin` | `BRIEFEN_DISABLE_PASSWORD_LOGIN` | `false` | honored only when OIDC enabled |
| `sessionTtl` | `BRIEFEN_SESSION_TTL` | `30d` | bearer-token lifetime |

Register in `BriefenApplication`'s `@EnableConfigurationProperties` list (consistent with the existing consolidation there).

**`ClientRegistration` wiring:** Provide a conditional `ClientRegistrationRepository` bean (only when `issuer` is set) built from `OidcProperties` via `ClientRegistrations.fromIssuerLocation(issuer)`, then `.clientId/.clientSecret/.scope/.redirectUri("{baseUrl}/login/oauth2/code/briefen")`. Registration id: **`briefen`**. Gate on `@ConditionalOnProperty(prefix="briefen.oidc", name="issuer")`. Fail-fast validation (like `DatabaseTypeValidator`): if `issuer` set but `clientId`/`clientSecret`/`redirectUri` missing → abort startup with a clear message.

> **Startup resilience (fix Montly's known bug):** Montly's `newRealOIDCProvider` does discovery at boot and `log.Fatalf`s on failure — a transient IdP outage crash-loops the app and locks out password users too (Montly BACKLOG P2). **Avoid this**: build the `ClientRegistration` lazily / tolerate discovery failure by degrading to password-only with a logged warning, rather than failing startup. Spring's `ClientRegistrations.fromIssuerLocation` is eager, so wrap it: attempt at startup, and on failure log a warning + leave OIDC disabled (or retry on first `/oauth2/authorization/briefen` hit). Add a test for the degraded path.

### 5.3 Security filter chain (`config/SecurityConfig.java`)

This is the biggest backend change. Update `filterChain`:

1. **Add a custom `BearerTokenAuthenticationFilter`** (new `security/BearerTokenAuthenticationFilter.java`, an `OncePerRequestFilter`) registered **before** `BasicAuthenticationFilter`:
   - If `Authorization: Bearer <token>` present → `sha256hex`, look up `auth_sessions`, load user, build `BriefenUserDetails`, set `SecurityContext`. Update `last_used_at` async. On miss/expired → 401 (dispatch nothing; let entry point handle).
   - If no Bearer header → do nothing; the existing HTTP Basic filter handles the request.
   - This makes auth **hybrid**: Basic *or* Bearer.

2. **Enable `oauth2Login`** (only when OIDC configured — guard with a `@ConditionalOnBean(ClientRegistrationRepository.class)` split config or an `if`):
   ```java
   http.oauth2Login(oauth -> oauth
       .authorizationEndpoint(a -> a.authorizationRequestRepository(cookieAuthzRequestRepo))
       .successHandler(oidcSuccessHandler)   // mints bearer token, redirects to SPA
       .failureHandler(oidcFailureHandler));  // redirect /login?auth_error=…
   ```

3. **permitAll** the new public endpoints:
   - `/oauth2/authorization/**` (Spring authorize redirect)
   - `/login/oauth2/code/**` (Spring callback)
   - `/api/auth/config`
   - keep existing `/actuator/health`, `/api/setup/**`, static assets.

4. **Statelessness:** keep the app session-less for API calls. The OAuth2 handshake needs to persist the authorization request across the redirect; use a **cookie-based `AuthorizationRequestRepository`** (Appendix B) so we don't introduce `HttpSession` for API requests. Set `sessionCreationPolicy(STATELESS)` and rely on the cookie repo. (Alternative: allow `IF_REQUIRED` sessions only for the ~10s handshake — simpler, but introduces `JSESSIONID`; the cookie repo is cleaner and matches Montly's stateless state cookie.)

5. CSRF stays disabled (documented, stateless bearer/basic API). The OAuth2 `state` parameter provides CSRF protection for the login handshake itself.

### 5.4 The success handler (`security/OidcAuthenticationSuccessHandler.java`)

The heart of the integration. On successful OIDC login Spring hands us an `OidcUser`:

1. Extract claims: `iss`, `sub`, username claim, `email`, `email_verified`, groups claim. Normalize like Montly's `extractClaims` (fallbacks: username claim → `preferred_username` → `email`).
2. Call `OidcUserResolver.resolve(claims)` → returns the Briefen `User` (link or provision). Same priority as Montly (see 5.5).
3. Mint an `auth_sessions` bearer token for that user.
4. **Redirect to the SPA with the token in the URL fragment** (not query — fragments are not sent to servers or stored in access logs / `Referer`):
   `302 → {contextPath}/#sso_token=<token>&uid=<id>&role=<ROLE>&username=<name>`
   The SPA reads the fragment, stores the session, strips it (`history.replaceState`).
5. Fire the existing audit/log path (`log.info("login via OIDC …")`). No webhook.

`OidcAuthenticationFailureHandler`: redirect `302 → {contextPath}/?auth_error=<reason>` mapping Spring `OAuth2AuthenticationException` error codes to Montly-style reasons (`provider_error`, `exchange_failed`, `signup_disabled`, `server_error`, etc.).

### 5.5 `OidcUserResolver` service (`service/OidcUserResolver.java`)

Port Montly's `resolveOIDCUser` + `applyAdminSync` + `uniqueUsername` almost verbatim (Java):

```
resolve(claims):
  inAdminGroup = adminGroup != "" && claims.groups.contains(adminGroup)

  1. findByOidc(iss, sub) present?        → applyAdminSync, return
  2. linkByEmail && email!="" && emailVerified:
       findByEmail(email) present & not already OIDC-linked
                                          → link, applyAdminSync, return
  3. linkByUsername && username!="":
       findByUsername(username) present & not already OIDC-linked
                                          → link, applyAdminSync, return
  4. JIT: if !allowSignup → throw AuthReason("signup_disabled")
       n = userCount()
       uname = uniqueUsername(usernameClaim)   // dedupe with -2, -3, …
       create SSO user (empty passwordHash, role = (inAdminGroup || n==0) ? ADMIN : USER)
```

- `applyAdminSync`: only when `adminGroup` set; flips `role` between `ADMIN`/`USER` to match group membership; persists if changed. Never touches role when `adminGroup` unset (so a manually promoted admin stays admin).
- New `AuthReasonException` (checked/runtime) carrying a `reason` string → mapped by the failure handler.
- **Linking trust model** (document verbatim from Montly): email/username linking trusts the IdP namespace; email linking additionally requires `email_verified: true`; both can be disabled.

### 5.6 New `UserPersistence` methods

```java
Optional<User> findByOidc(String issuer, String subject);
Optional<User> findByEmail(String email);            // lowest-id match
User linkOidcIdentity(String userId, String issuer, String subject, String email);
User createOidcUser(String username, String email, String issuer, String subject, String role);
User setRole(String userId, String role);
```
Implement in `JpaUserPersistence` + add finder methods to `JpaUserRepository`. (`findByUsername` already exists.)

### 5.7 Endpoints

**New public endpoint** — `AuthController` (new, or fold into `SetupController`):
- `GET /api/auth/config` → `AuthConfigResponse { boolean passwordLogin; OidcView oidc; }` where `OidcView { boolean enabled; String providerName; }`. Mirrors Montly's `AuthConfig`. Drives the login screen.

**New authenticated endpoint:**
- `POST /api/auth/logout` → deletes the caller's `auth_sessions` row (revokes the bearer token). Returns 204. (Password/Basic users can call it too; it's a no-op for them since they have no token.)
- *(Optional, later)* `POST /api/auth/logout?rp=true` → also return the IdP `end_session_endpoint` URL for RP-initiated logout (Montly feature-idea backlog).

**Existing endpoints unchanged:** `/api/setup/**`, `/api/users/me`, `/api/summarize`, etc. `BriefenUserDetails` is produced by *either* the Basic path or the Bearer path, so all `@AuthenticationPrincipal` controllers keep working untouched.

### 5.8 Interaction with existing setup/bootstrap

- **First-run setup** (`SetupService`): still valid. If an admin already logs in via SSO first (first SSO user ⇒ admin), then `isSetupRequired()` returns false and the browser setup screen is skipped. If OIDC is enabled but no admin exists yet, the login screen shows the SSO button; the first SSO login bootstraps the admin — document this as an alternative to the password setup.
- **`UserBootstrapService`**: unaffected. Legacy-data migration + env-var API-key seeding still run on `ApplicationReadyEvent`.
- **`DISABLE_PASSWORD_LOGIN`**: when true *and* OIDC enabled, `BriefenUserDetailsService`/Basic filter should reject password logins (return 401) and `/api/auth/config` reports `passwordLogin=false`. Guard: if password login disabled but OIDC not configured, **ignore it** and log a warning (don't lock everyone out — copy Montly's safety check in `main.go`).

---

## 6. Frontend (React) changes

The beauty of the bearer-token choice: **`authHeader` already abstracts the scheme.** Changes are additive.

### 6.1 `hooks/useAuth.js`
- On mount, **detect an SSO callback**: parse `window.location.hash` for `#sso_token=…&uid=…&role=…&username=…`. If present:
  - store `{ username, authHeader: 'Bearer ' + token, userId, role }` in `sessionStorage.briefen_auth` (same shape as today),
  - call `setAuthHeader('Bearer ' + token)`,
  - set state authenticated,
  - `history.replaceState` to strip the fragment.
- `logout()` → also `POST /api/auth/logout` (best-effort) before clearing `sessionStorage`, so the server revokes the token. Keep clearing local state regardless.
- Everything else (the `briefen:unauthorized` 401 listener, session restore) is unchanged.

### 6.2 `apiFetch.js`
- **No change required** — it already sends whatever `authHeader` string is set. A `Bearer …` value works as-is. (Optional: nothing.)

### 6.3 New `hooks/useAuthConfig.js`
- `GET /api/auth/config` on app load (public, no auth). Returns `{ passwordLogin, oidc }`. Cache in state. Used by `Login.jsx`.

### 6.4 `components/Login.jsx`
- Accept `authConfig` prop. Render:
  - SSO button `<a href="{BASE_URL}oauth2/authorization/briefen">Sign in with {providerName}</a>` when `oidc.enabled`.
  - "or" divider when both methods available.
  - Password form only when `passwordLogin !== false`.
- Read `?auth_error=` on mount, map to a friendly localized message, show as `role="alert"`, strip the param (port Montly's `AUTH_ERRORS` map into `strings.js`).

### 6.5 `App.jsx`
- Fetch auth config (via `useAuthConfig`) before showing `<Login>`; pass it down. The SSO callback handling in `useAuth` must run **before** the setup/login gate so a returning SSO user lands authenticated.

### 6.6 i18n
- Add `strings.js` / `locales/en.json` + `pt-BR.json` keys: `LOGIN_SSO_BUTTON` (`"Sign in with {{provider}}"`), `LOGIN_OR_DIVIDER`, and the `AUTH_ERROR_*` map. No hardcoded strings (project rule).

### 6.7 Tests
- `Login.test.jsx`: SSO button shown when enabled; hidden when not; password form hidden when `passwordLogin=false`; `?auth_error=` renders the mapped message.
- `useAuth.test.js`: `#sso_token=…` fragment → session persisted with `Bearer` authHeader + fragment stripped; logout posts to `/api/auth/logout`.

---

## 7. Mobile (Flutter) changes — later phase

OIDC in a native app needs an external-browser handshake. Recommend **`flutter_web_auth_2`** (ASWebAuthenticationSession on iOS, Custom Tabs on Android).

- Flow: open `{server}/oauth2/authorization/briefen?client=mobile`; the success handler detects the `mobile` marker (stored in the authz-request cookie/state) and redirects to a **custom scheme** `dev.azurecoder.briefen://auth#sso_token=…`; `flutter_web_auth_2` captures it and returns the token.
- Store the token in `flutter_secure_storage`; add a **Bearer** branch to the Dio interceptor (`core/api/api_client.dart`) — if an SSO token exists, send `Authorization: Bearer <token>`; else the existing Basic path.
- `AuthNotifier` gains an `authenticatedViaSso` path; `AuthStorage` stores either credentials or an SSO token.
- Register the custom URL scheme (Android intent filter; iOS `CFBundleURLTypes`). The redirect URI must be registered at the IdP.
- Login screen: add "Sign in with {provider}" when `/api/auth/config` reports `oidc.enabled`.
- **Mark as a follow-up phase** — ship web first; mobile OIDC is meaningfully more work and can trail.

---

## 8. Browser extensions — out of scope (document why)

The Firefox/Chrome popups collect a username/password and store them. OIDC's interactive redirect doesn't fit a popup that POSTs to `/api/articles`. Recommended path: users on SSO-only instances create a **long-lived bearer token** (the `auth_sessions` mechanism could expose an "API token" management UI later — mirrors Montly's `mt_` tokens) and paste it into the extension instead of a password. **List as a future enhancement**, not part of this plan.

---

## 9. Config, docs, and compose

Per the project rule ("when adding an env var, update all of"):
- **`application.yml`**: add `briefen.oidc.*` block + `spring.security.oauth2.client` note; all reading from env with defaults.
- **`.env.example`**: add the commented OIDC block (copy Montly's, re-prefixed `BRIEFEN_OIDC_*`).
- **`docs/environment-variables.md`**: full table (single source of truth).
- **`docker-compose.sample.yml`**: commented OIDC env entries.
- **New `docs/oidc.md`** (or a section in `getting-started.md`): port Montly's "OpenID Connect (SSO)" doc — env table, account-resolution order, linking trust model, Keycloak example, `redirect_uri` must be `https://host/login/oauth2/code/briefen`, `SERVER_FORWARD_HEADERS_STRATEGY=FRAMEWORK` behind a proxy so redirect URIs are built with the external scheme/host.
- **`CHANGELOG.md`**: `[Unreleased]` entry.

> **Reverse-proxy note:** Spring builds the redirect URI from the request; behind Nginx/Caddy/Traefik set `SERVER_FORWARD_HEADERS_STRATEGY=FRAMEWORK` (already supported) so `X-Forwarded-Proto/Host` are honored and the callback URL matches what's registered at the IdP. Call this out prominently — it's the #1 OIDC-behind-proxy footgun.

---

## 10. Testing strategy

### 10.1 Backend unit tests
- `OidcUserResolverTest` — the full resolution matrix (already-linked; link-by-verified-email; link-by-username; JIT provision; signup disabled; admin-group sync on/off; first-user-becomes-admin; username dedupe). This is the highest-value test file — port Montly's `oidc_test.go` cases.
- `OidcPropertiesValidationTest` — issuer set but missing clientId/secret/redirect → startup fails; missing issuer → OIDC disabled.
- `BearerTokenAuthenticationFilterTest` — valid token authenticates; expired/unknown → 401; no header → falls through to Basic.
- `AuthControllerTest` — `/api/auth/config` reflects enabled/disabled + `passwordLogin`; `/api/auth/logout` revokes the token.
- `JpaAuthSessionPersistenceTest`, `JpaUserPersistence` new-method tests.

### 10.2 Backend integration
- Use **WireMock** (already a test dep) to stub the OIDC discovery document (`/.well-known/openid-configuration`), JWKS, and token endpoint; drive `/login/oauth2/code/briefen` with a signed ID token and assert a bearer token is issued + user provisioned. (Spring Security has good test support for `oauth2Login`.)

### 10.3 E2E (Playwright)
- **Reuse Montly's mock IdP verbatim**: copy `e2e/mock-oidc/server.mjs` (zero-dependency node OIDC provider) and wire it in a compose/managed profile. Add `e2e/oidc.spec.js` porting Montly's `06-oidc.spec.ts`:
  - login screen shows SSO button alongside password form,
  - full authorization-code flow signs in and reaches the app,
  - session persists across reload,
  - logout returns to a login screen that still offers SSO,
  - callback with forged/missing state shows a friendly error and does **not** sign in.
- Extend `make e2e-managed` to optionally start the mock IdP + set `BRIEFEN_OIDC_*`.

### 10.4 CI
- No new job needed; the OIDC E2E runs under the existing Playwright flow. Backend/unit tests run in the existing `backend` job.

---

## 11. Security checklist (must-verify before merge)

- [ ] ID token verified: signature (JWKS), `iss`, `aud == clientId`, `exp`, **nonce** — all handled by Spring Security OAuth2 client; add a test asserting a tampered token is rejected.
- [ ] PKCE (S256) enabled on the client registration.
- [ ] Authorization-request state stored in a **signed, `HttpOnly`, `SameSite=Lax`, short-TTL** cookie (Lax so it survives the top-level redirect back); cleared on callback (Appendix B).
- [ ] Bearer tokens stored **hashed** (sha256); plaintext never logged, never persisted; `bfn_` prefix; ≥ 256 bits entropy.
- [ ] SSO token delivered to the SPA via **URL fragment**, never query string (avoids `Referer` + access-log + browser-history leakage). SPA strips it immediately.
- [ ] `email` linking gated on `email_verified: true`.
- [ ] `DISABLE_PASSWORD_LOGIN` ignored (with warning) when OIDC unconfigured — no lockout.
- [ ] OIDC discovery failure at startup **degrades to password-only**, does not crash-loop (fixes Montly's `log.Fatalf` bug).
- [ ] Rate-limit `/oauth2/authorization/briefen` + callback (Montly backlog: unthrottled callback triggers an outbound token exchange = mild DoS vector). Consider a simple per-IP limiter or rely on the proxy.
- [ ] CSP (`SecurityHeadersFilter`) still allows the IdP redirect (it's a top-level navigation, not an XHR — no CSP `connect-src` change needed; verify `form-action`/`frame-ancestors` don't block the flow).
- [ ] Token revocation on logout works; expired-token sweep runs.
- [ ] Document the `sessionStorage` XSS exposure trade-off and the CSP mitigation.

---

## 12. Rollout & backward compatibility

- **Fully backward compatible & opt-in.** With `BRIEFEN_OIDC_ISSUER` unset, nothing changes: no SSO button, Basic Auth only, new columns/table sit empty. Bearer filter is a no-op when no `Bearer` header arrives.
- New DB columns/table are additive; ddl-auto + `SchemaInitializer` handle SQLite and Postgres.
- Existing sessions (Basic Auth in `sessionStorage`) keep working — no forced re-login.
- Ship **web-only OIDC first** (Phases A–D). Mobile (Phase E) and extension token-auth are follow-ups.

---

## 13. Phased checklist (suggested execution order)

**Phase A — Data & token session foundation** (backend, no OIDC yet) — ✅ **DONE**
- [x] Add `email`/`oidcIssuer`/`oidcSubject` to `User` + `JpaUserEntity`; SQLite `SchemaInitializer` migration (defensive nullable-column add + `idx_users_oidc` unique index).
- [x] `AuthSession` model + `AuthSessionPersistence`/`JpaAuthSessionEntity`/`JpaAuthSessionRepository`/`JpaAuthSessionPersistence`; `AuthSessionService` with token mint/verify/revoke (`bfn_` generator, SHA-256 hashing, throttled `last_used_at`, fixed TTL via `briefen.session.ttl`).
- [x] `BearerTokenAuthenticationFilter` wired into `SecurityConfig` before `BasicAuthenticationFilter`; hybrid auth green.
- [x] `POST /api/auth/logout` (`AuthController`); unit + filter + persistence + controller tests (21 new, all green; full suite 335/335).
- *Deliverable:* ✅ a user can authenticate with a manually-inserted bearer token; Basic Auth unaffected.
- *Deferred to later phases:* revoking sessions on user deletion (currently orphaned sessions are cleaned lazily on resolve); scheduled `purgeExpired()` sweep (method exists, not yet scheduled).

**Phase B — OIDC login (backend)** — ✅ **DONE**
- [x] `spring-boot-starter-oauth2-client` dep; `OidcProperties` (`briefen.oidc.*`) + fail-fast validation (`OidcSecurityConfig.@PostConstruct`) + **lazy/degrading discovery** (`LazyClientRegistrationRepository` — discovery on first use, returns null + warns on failure, never crash-loops).
- [x] Conditional `ClientRegistrationRepository` (via custom `OidcEnabledCondition` — blank issuer = disabled); **cookie** `AuthorizationRequestRepository` (`HttpCookieOAuth2AuthorizationRequestRepository`, HMAC-signed, `SameSite=Lax`, `ObjectInputFilter`) + no-op `StatelessOAuth2AuthorizedClientRepository` → fully stateless (`SessionCreationPolicy.STATELESS`).
- [x] `OidcUserResolver` (Montly logic ported: linked → verified-email → username → JIT; admin-group sync; first-user-admin; username dedupe; never demotes main admin) + `UserPersistence.findByOidc/findByEmail`.
- [x] `OidcAuthenticationSuccessHandler` (resolve → mint token → **URL-fragment** redirect) + `OidcAuthenticationFailureHandler` (`?auth_error=<reason>`).
- [x] `oauth2Login` in `SecurityConfig` (conditional via `ObjectProvider`); permitAll `/oauth2/authorization/**`, `/login/oauth2/code/**`, `/api/auth/config`; REST 401 entry point (no IdP redirect, no Basic dialog).
- [x] `GET /api/auth/config` (`AuthConfigResponse`); `DISABLE_PASSWORD_LOGIN` handling (conditional Basic; ignored when SSO off).
- [x] Tests: `OidcUserResolverTest` (15 — full matrix), `OidcPropertiesTest` (7), `HttpCookieOAuth2AuthorizationRequestRepositoryTest` (6 — round-trip + tamper/wrong-secret rejection), `AuthControllerTest` config case, `OidcEnabledConfigTest` (full-context boot with OIDC on). Full suite: **365/365**.
- *Deferred:* the full WireMock token-exchange dance (discovery+JWKS+token+signed JWT through `oauth2Login`) is covered end-to-end by the Phase D mock-IdP Playwright test rather than duplicated here.

**Phase C — Frontend** — ✅ **DONE**
- [x] `hooks/useAuthConfig.js` (`GET /api/auth/config`, password-only fallback on error); `useAuth` SSO-fragment handling (`consumeSsoFragment` promotes `#sso_token=…` → Bearer session, primes `apiFetch`, strips URL) + best-effort `POST /api/auth/logout`.
- [x] `Login.jsx` SSO button (`/oauth2/authorization/briefen` via `BASE_URL`) + "or" divider + conditional password form + `?auth_error=` → localized message (derived in a lazy initializer to satisfy the `react-hooks/set-state-in-effect` rule); `Login.module.css` `.ssoButton`/`.divider`; `App.jsx` wires `useAuthConfig` → `Login`.
- [x] i18n keys added to `en.json` + `pt-BR.json` (`LOGIN_SSO_PREFIX`, `LOGIN_OR`, `AUTH_ERROR_*`), pt-BR inserted surgically to preserve file formatting.
- [x] Tests: `Login.test.jsx` (+4: SSO shown/hidden, SSO-only hides password form, `auth_error` message+strip), `useAuth.test.js` (+2: fragment→Bearer session+URL strip, logout POSTs revoke). Lint clean, **113/113** frontend tests, production build OK.
- *Note:* the existing `authHeader` abstraction meant zero change to `apiFetch.js` — a `Bearer …` value flows through the same path as `Basic …`.

**Phase D — Docs, compose, E2E** — ✅ **DONE**
- [x] `.env.example` (Authentication/SSO block), `application.yml` (Phase B), `docs/environment-variables.md` (SSO section + quick-ref + `_FILE` rows), `docker-compose.sample.yml` (commented OIDC + `_FILE` secret), new `docs/oidc.md` (full guide + Keycloak example + reverse-proxy note), `CHANGELOG.md` `[Unreleased]`. `FileSecretsEnvironmentPostProcessor` allowlist extended with `BRIEFEN_OIDC_CLIENT_SECRET` + `BRIEFEN_SESSION_SECRET`.
- [x] Mock IdP `e2e/mock-oidc/server.mjs` (zero-dep RS256 provider); `e2e/oidc.spec.js` (5 tests, opt-in via `E2E_OIDC`); `global-setup`/`global-teardown` start/stop the mock IdP + inject hybrid SSO env; new `make e2e-oidc` target.
- [x] **Full managed run green: 5/5 OIDC E2E pass** against the real backend + mock IdP (SSO button, full auth-code+PKCE sign-in as JIT-provisioned admin, session-across-reload, logout-still-offers-SSO, missing-state error). Existing `make e2e`/`e2e-managed` runs are unchanged (OIDC off by default).

**Phase E — Mobile (Flutter)** — ✅ **DONE**
- [x] Backend mobile handoff: `MobileClientMarkerFilter` sets a `briefen_login_client=mobile` cookie on `/oauth2/authorization/briefen?client=mobile`; `OidcAuthenticationSuccessHandler` reads it and deep-links the token via the custom scheme (`briefen://auth?sso_token=…`, query not fragment — captured by the native app, never sent to a server) instead of the web fragment; `OidcProperties.mobileScheme` (default `briefen`). Test: `OidcAuthenticationSuccessHandlerTest` (web-fragment + mobile-scheme branches).
- [x] `flutter_web_auth_2` dep; `AuthStorage`/`Credentials` gained a `token` field (`isBearer`); `ApiClient` interceptor sends `Bearer` for SSO sessions, `Basic` otherwise; `AuthNotifier.loginWithSso()` (external-browser auth-code flow → parse callback → store bearer session) + `logout()` now POSTs `/api/auth/logout`.
- [x] Login screen: gated "Sign in with SSO" button + "or" divider (enabled once a valid server URL is entered; taps check `/api/auth/config` first, snackbar if SSO is off). ARB strings `loginWithSso`/`orDivider`/`ssoNotEnabled`/`ssoFailed` in en/pt/pt-BR. Android `CallbackActivity` registered for the `briefen` scheme (iOS uses ASWebAuthenticationSession's runtime callback scheme — no Info.plist change; coexists with the existing `briefen://share` extension).
- [x] Tests: `credentials_test.dart` (isBearer) + `auth_notifier_test` logout now verifies the revoke POST. `flutter analyze` clean for changed files; **101/101 mobile tests**; backend **367/367**; web OIDC E2E re-run **5/5** (no regression from the mobile changes).
- *Not run here:* on-device/emulator execution of the mobile SSO browser handshake (no emulator in this environment) — validated via unit tests, `flutter analyze`, and the backend mobile-redirect test.

**Phase F — Extensions & extras** — ✅ **DONE** (RP-logout deferred)
- [x] **Personal access tokens** (reuse `auth_sessions`, `source="api"`, `name` label, long TTL `BRIEFEN_API_TOKEN_TTL`): `AuthSessionService.issueApiToken/listApiTokens/revokeApiToken`; `GET/POST/DELETE /api/auth/tokens` (max 20/user, owner-scoped); web Settings → "Access tokens" section (`useApiTokens` hook, create-reveal-once + copy, list, revoke; en/pt-BR strings). Both **Firefox & Chrome extensions** accept a token (Options *Access token* field → `Authorization: Bearer`), with a shared `buildAuthHeader` preferring token over Basic; READMEs + `docs/oidc.md` updated.
- [x] **Auth endpoint rate limiting**: `RateLimiter` (in-memory fixed-window, bounded) + `RateLimitFilter` (per-IP, ahead of Spring Security) on the OIDC handshake + `/api/setup`; `BRIEFEN_RATE_LIMIT_ENABLED/MAX_REQUESTS/WINDOW`. Fixes the Montly-flagged unthrottled-callback DoS vector.
- [x] Tests: backend `RateLimiterTest` (3) + `AuthSessionServiceTest` API-token cases + `AuthControllerTest` token endpoints → **backend 379/379**; Firefox extension **jest 79/79** (incl. `buildAuthHeader` + token persistence); frontend lint/113 tests/build green.
- [ ] **RP-initiated logout (`end_session_endpoint`) — intentionally deferred.** It requires the `id_token` (we deliberately don't persist it) and a registered `post_logout_redirect_uri`; it's an "unscheduled idea" even in the reference. Documented here rather than shipped half-formed.

---

## 14. Open questions / decisions to confirm

> **Decided:** session model = **opaque bearer tokens** (§2). No longer open.

1. **Session TTL & sliding renewal** — 30 days sliding? Absolute cap?
2. **Should password logins also move to bearer sessions?** Out of scope here (keeps blast radius small), but the `auth_sessions` table is designed to allow it later. Confirm we keep Basic for password logins for now.
3. **Multiple IdPs** — plan supports one registration (`briefen`). Multi-provider is a larger change; defer.
4. **Account-linking default** — ship with `linkByEmail`/`linkByUsername` = `true` (Montly's default) or default them **off** for a stricter posture? Recommend matching Montly (`true`) but documenting the trust model.

---

## Appendix A — Cookie-session alternative (rejected; kept for the record)

**Not chosen** — bearer tokens (§2) are the committed approach. This section documents why, so the decision isn't relitigated.

Instead of a bearer token, issue an `HttpOnly`, `Secure`, `SameSite=Strict` signed session cookie on the OIDC callback (Montly's exact model):
- Add a `CookieSessionAuthenticationFilter` that validates the signed cookie (HMAC over `{userId, role, exp}` with a `BRIEFEN_SESSION_SECRET`), alongside Basic.
- Frontend switches to `fetch(..., { credentials: 'include' })` and **stops** attaching a Basic header when a cookie session is active; `apiFetch` needs a branch.
- Mobile needs a Dio cookie jar; extensions need `credentials: 'include'` + CORS `Access-Control-Allow-Credentials`.
- **Pros:** token not readable by JS (XSS-safer). **Cons:** larger frontend/mobile/extension churn, CORS-with-credentials complexity, diverges from Briefen's header-on-every-request model. **Net: more work, more moving parts** — which is why bearer tokens were chosen instead.

## Appendix B — Cookie-based `AuthorizationRequestRepository` (statelessness)

Spring's default `HttpSessionOAuth2AuthorizationRequestRepository` needs an `HttpSession`. To keep Briefen stateless, implement `AuthorizationRequestRepository<OAuth2AuthorizationRequest>` backed by a signed cookie (HMAC-SHA256 over the serialized authz request — state, nonce, PKCE verifier, redirect), `HttpOnly` + `SameSite=Lax` (Lax is required so the cookie survives the top-level redirect back from the IdP) + ~10 min TTL, cleared on callback. This is the direct analog of Montly's `signOIDCState`/`parseOIDCState` HMAC state cookie. Well-trodden pattern; several reference implementations exist.

---

## Appendix C — Key files touched (quick index)

**Backend (new):** `config/OidcProperties.java`, `config/OidcClientConfig.java` (registration + cookie authz repo), `security/BearerTokenAuthenticationFilter.java`, `security/OidcAuthenticationSuccessHandler.java`, `security/OidcAuthenticationFailureHandler.java`, `service/OidcUserResolver.java`, `exception/AuthReasonException.java`, `controller/AuthController.java`, `persistence/AuthSessionPersistence.java` + `persistence/jpa/{JpaAuthSessionEntity,JpaAuthSessionPersistence,JpaAuthSessionRepository}.java`.
**Backend (edit):** `config/SecurityConfig.java`, `model/User.java`, `persistence/jpa/JpaUserEntity.java`, `persistence/UserPersistence.java`, `persistence/jpa/{JpaUserPersistence,JpaUserRepository}.java`, `config/SchemaInitializer.java`, `BriefenApplication.java`, `pom.xml`, `application.yml`.
**Frontend (new):** `hooks/useAuthConfig.js`. **(edit):** `hooks/useAuth.js`, `components/Login.jsx`, `App.jsx`, `constants/strings.js`, `locales/en.json`, `locales/pt-BR.json`, `apiFetch.js` (optional).
**Ops/docs:** `.env.example`, `docs/environment-variables.md`, `docs/oidc.md`, `docker-compose.sample.yml`, `CHANGELOG.md`, `e2e/mock-oidc/server.mjs`, `e2e/oidc.spec.js`.
**Mobile (Phase E):** `core/api/api_client.dart`, `core/auth/*`, login screen, platform URL-scheme config, `pubspec.yaml`.
