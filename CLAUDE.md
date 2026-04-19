# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Briefen** — a local-first article summarizer. Paste a URL → Jsoup fetches the article → Ollama (local LLM), OpenAI, or Anthropic Claude summarizes it → result is cached in SQLite (default) or PostgreSQL. No cloud dependencies by default; everything runs locally.

## Commands

### Start everything
```bash
make up      # Start Docker services (Ollama)
make dev     # Start Docker services + backend + frontend in parallel
```

### Run services individually
```bash
make backend   # cd backend && ./mvnw spring-boot:run
make frontend  # cd frontend && pnpm dev
```

### Docker (single-image build)
```bash
make docker-build  # Build the Briefen Docker image
make docker-up     # Start full stack: app + Ollama
make docker-down   # Stop full stack
```

### Self-hosting (pre-built image, no clone required)
```bash
docker compose -f docker-compose.sample.yml up -d
```

### Testing
```bash
cd backend && ./mvnw test       # Backend unit tests
cd backend && ./mvnw verify     # Full verification including integration tests
cd frontend && pnpm lint        # Frontend linting
cd frontend && pnpm test        # Frontend unit tests (Vitest + Testing Library)
make e2e                        # Playwright E2E tests (requires running app)
make e2e-managed                # E2E with WireMock + auto-start backend
```

### Other
```bash
make logs      # Tail Docker service logs
make down      # Stop Docker services
make clean     # Stop containers, remove build artifacts (preserves Ollama weights)
make clean-all # Full reset including Ollama model weights
```

### Frontend
```bash
cd frontend
pnpm dev             # Dev server on :5173
pnpm build           # Production build
pnpm lint            # ESLint
pnpm test            # Vitest unit tests
pnpm test:coverage   # Coverage report
```

### Backend
```bash
cd backend
./mvnw spring-boot:run   # Run app
./mvnw test              # Run tests
./mvnw verify            # Run tests + integration tests
./mvnw clean package     # Build JAR
```

## Architecture

```
# Local dev
Browser (React/Vite :5173)
  → /api/* proxied to Spring Boot (:8080)
    → Jsoup (fetch article HTML) or PDFBox (PDF)
    → Ollama (:11434, local LLM) or OpenAI or Anthropic (cloud, optional)
    → SQLite (file-based, ./data/briefen.db) or PostgreSQL (optional)

# Docker (single image, self-hosting)
Browser → Spring Boot (:8080, serves React static + API)
    → Ollama (Docker service or external) or OpenAI/Anthropic (external)
    → SQLite (named Docker volume) or PostgreSQL (optional)
```

### Backend (`backend/src/main/java/com/briefen/`)
- **controller/** — REST endpoints:
  - `SummarizeController` — all `/api/summarize` and `/api/summaries` routes (CRUD, export, read status, notes)
  - `ArticlesController` — `POST /api/articles` async queue endpoint (used by Firefox extension; returns 202)
  - `ModelsController` — `/api/models` lists available LLM providers and models
  - `SettingsController` — `/api/settings` read/update user preferences
  - `ReadeckController` — `/api/readeck/*` proxies requests to a user-configured Readeck instance (API key stays server-side)
  - `SetupController` — `/api/setup` first-run admin account creation (unauthenticated)
  - `UserManagementController` — `/api/admin/users` user management (admin only)
  - `VersionController` — `/api/version` build info
  - `HealthController` — supplementary health endpoint
  - `GlobalExceptionHandler` — maps exceptions to HTTP responses
- **service/** — core logic:
  - `ArticleFetcherService` — Jsoup HTML fetcher with DNS rebinding protection, PDFBox for PDFs, MDX/JSX source parser
  - `OllamaSummarizerService` / `OpenAiSummarizerService` / `AnthropicSummarizerService` — LLM summarizers
  - `SummaryService` — orchestration: cache lookup, fetching, summarizing, persisting
  - `WebhookService` — fire-and-forget POST on summary save (virtual thread)
  - `UserBootstrapService` — migrates pre-multi-user data and seeds cloud LLM API keys from env vars on startup
  - `SetupService` — handles browser-based first-run admin account creation
  - `PromptBuilder` — assembles LLM prompts from user settings and article content
  - `TagExtractor` — extracts/normalizes tags from LLM responses
- **model/** — plain domain POJOs (no persistence annotations): `Summary`, `UserSettings`, `User`
- **persistence/** — database persistence layer (supports SQLite and PostgreSQL):
  - `SummaryPersistence` / `SettingsPersistence` / `UserPersistence` — interfaces
  - `persistence/jpa/` — JPA implementations using JpaRepository + JpaSpecificationExecutor (shared by both SQLite and PostgreSQL)
- **dto/** — request/response records
- **exception/** — custom exceptions: `ArticleExtractionException`, `ArticleFetchException`, `InvalidUrlException`, `SummarizationException`, `SummaryNotFoundException`
- **validation/** — `PasswordValidator`, `UrlValidator`
- **security/** — `BriefenUserDetails`, `BriefenUserDetailsService`
- **config/** — `SecurityConfig` (HTTP Basic Auth, always on; `/api/setup/**` unauthenticated for first-run flow), `SecurityHeadersFilter` (CSP, X-Frame-Options, etc.), `CorsConfig`, `BriefenProperties`, `OllamaProperties`, `OpenAiProperties`, `AnthropicProperties`, `RestClientConfig`, `OpenAiRestClientConfig`, `AnthropicRestClientConfig`, `OllamaHealthIndicator`, `ApplicationReadinessValidator`, `WebConfig` (SPA routing), `SqliteConfig`, `PostgresConfig`, `DatabaseProfileActivator` (EnvironmentPostProcessor for DB profile/datasource selection), `DatabaseTypeValidator` (fail-fast validation of DB config at startup), `FileSecretsEnvironmentPostProcessor` (reads `_FILE` suffix env vars for Docker secrets support)

Key `application.yml` settings (all configurable via env vars):
```yaml
server.port:                    ${SERVER_PORT:8080}
server.address:                 ${SERVER_BIND_ADDRESS:0.0.0.0}
server.servlet.context-path:    ${SERVER_CONTEXT_PATH:/}
server.forward-headers-strategy:${SERVER_FORWARD_HEADERS_STRATEGY:NONE}
ollama.base-url:                ${OLLAMA_BASE_URL:http://localhost:11434}
ollama.model:                   ${OLLAMA_MODEL:gemma3:4b}
ollama.timeout:                 300s
briefen.cors.allowed-origins:   ${BRIEFEN_CORS_ALLOWED_ORIGINS:}
briefen.openai.api-key:         ${BRIEFEN_OPENAI_API_KEY:}
briefen.anthropic.api-key:      ${BRIEFEN_ANTHROPIC_API_KEY:}
briefen.webhook.url:            ${BRIEFEN_WEBHOOK_URL:}
logging.level.com.briefen:      ${BRIEFEN_LOG_LEVEL:INFO}
```

Database configuration is handled by `DatabaseProfileActivator` (an `EnvironmentPostProcessor`) and profile-specific YAML files (`application-sqlite.yml`, `application-postgres.yml`). Key database env vars:
- `BRIEFEN_DB_TYPE` — `sqlite` (default) or `postgres`
- `BRIEFEN_DB_PATH` — SQLite file path (default: `./data/briefen.db`)
- `BRIEFEN_DATASOURCE_URL` / `BRIEFEN_DATASOURCE_USERNAME` / `BRIEFEN_DATASOURCE_PASSWORD` — required when `postgres`

Full variable reference: `docs/environment-variables.md`

### Frontend (`frontend/src/`)
- **App.jsx** — React Router: `/` (home/summarize), `/reading-list`, `/settings`
- **main.jsx** — `BrowserRouter` with `basename={import.meta.env.BASE_URL}` for sub-path support
- **hooks/** — custom hooks encapsulate all API calls and state:
  - `useSummarize` — single URL/text summarization with abort support
  - `useBatchSummarize` — sequential multi-URL processing
  - `useReadingList` — full reading list management (fetch, filter, search, toggle read, delete, notes)
  - `useSettings` — loads/syncs user settings with optimistic updates
  - `useReadeck` — Readeck integration (status, bookmarks, article extraction)
  - `useSummaries` — recent summaries with pagination
  - `useUnreadCount` — unread badge count
  - `useElapsedTime` — real-time elapsed timer for loading states
  - `useTheme` — dark/light theme toggle with localStorage persistence
  - `useNotification` — web notification permission and dispatch
  - `useAuth` — HTTP Basic Auth state (login/logout, credential storage)
  - `useSetup` — first-run setup flow state
  - `useUsers` — user management (admin)
- **components/** — UI components; no component library, plain CSS + CSS modules
- **constants/strings.js** — all user-facing strings accessed here via an i18next proxy; actual strings live in `locales/en.json` (and other locale files)
- **i18n.js** — i18next initialization
- **locales/** — JSON translation files (`en.json`, `pt-BR.json`)
- **utils/** — shared utilities (relative date formatting)

Vite config injects `__APP_COMMIT__` (git short hash) and `__BUILD_DATE__` as compile-time constants, displayed in the app footer. The `base` option reads `VITE_APP_BASE_PATH` (set by the Dockerfile `APP_BASE_PATH` build arg) for sub-path support.

All `/api` requests go through Vite's dev proxy to the Spring Boot backend. Fetch calls use `AbortController` for cancellation support.

### Firefox Extension (`extension/`)
- Manifest V2, vanilla JS
- Popup sends the active tab's URL to `POST /api/articles` (202 Accepted)
- Options page: Briefen instance URL, username, password
- Requires `BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*` for remote instances

### Chrome Extension (`extension-chrome/`)
- Manifest V3, vanilla JS, `chrome.*` API (not `browser.*`)
- Popup sends the active tab's URL to `POST /api/articles` (202 Accepted)
- Options page opens in a new tab (not an in-popup panel like Firefox)
- Uses `chrome.storage.local` for persisting settings
- Requires `BRIEFEN_CORS_ALLOWED_ORIGINS: chrome-extension://*` for remote instances
- Compatible with Chrome 116+, Edge, Brave, Vivaldi, Arc, and other Chromium browsers

### Infrastructure
- **SQLite** (default) — file-based database at `./data/briefen.db`. Schema managed by Hibernate `ddl-auto: update` with a custom `SchemaInitializer` for SQLite-specific migrations. Three tables: `users`, `summaries`, `settings`.
- **PostgreSQL** (optional) — for larger-scale or multi-instance deployments. Enabled via `BRIEFEN_DB_TYPE=postgres`. Schema also managed by `ddl-auto: update`.
- **Ollama** — local LLM; Docker Compose pulls `gemma2:2b`, `gemma3:4b`, `llama3.2:3b` on first start
- **OpenAI** — optional cloud provider; API key seeded from `BRIEFEN_OPENAI_API_KEY` on first startup or configured via browser settings, stored server-side
- **Anthropic** — optional cloud provider; same pattern as OpenAI, key from `BRIEFEN_ANTHROPIC_API_KEY`
- **Readeck** — optional bookmark integration; URL and API key configured in browser settings

### Mobile App (`mobile/`)

Flutter 3.x cross-platform client for Android and iOS. **Phases 1–4 complete.**

**Running:**
```bash
cd mobile
flutter run -d emulator-5554      # Android emulator
flutter run -d <ios-device-id>    # iOS device/simulator
flutter run --no-resident          # Run without hot-reload (one-shot)
flutter gen-l10n                   # Regenerate localizations after editing .arb files
flutter analyze --no-fatal-infos  # Lint
```

**App namespace:** `dev.azurecoder.briefen`

**Key dependencies:**
- `flutter_riverpod: ^2.6.1` — state management
- `go_router: ^17.2.1` — declarative routing with auth redirect guards
- `dio: ^5.8.0` — HTTP client with Basic Auth interceptor
- `flutter_secure_storage: ^9.2.4` — credentials (Keychain / EncryptedSharedPreferences)
- `shared_preferences: ^2.5.3` — theme mode persistence
- `flutter_markdown: ^0.7.7` — renders summary markdown
- `flutter_local_notifications: ^18.0.1` — background summarization complete alert
- `flutter_foreground_task: ^8.17.0` — keeps network alive while app is backgrounded
- `share_plus: ^11.0.0` — native share sheet
- `url_launcher: ^6.3.1` — open original articles in browser
- `local_auth: ^2.3.0` — biometric / device PIN authentication
- `flutter_svg: ^2.1.0` — renders the Briefen logo SVG asset

**Implemented features:**
- Login, first-run setup, auth persistence across reinstalls (`hasFragileUserData`)
- URL summarization with background foreground service + push notification on complete
- Text paste summarization (tab on summarize screen; no title field — title inferred by backend)
- Batch URL summarization (Batch tab) with sequential processing, progress indicator, and foreground service for background safety
- Reading list: paginated, filter (all/unread/read), search, swipe to delete/mark read
- Summary detail: markdown rendering, auto-mark-read after 3 s, notes editing (dialog), tags editing (chip dialog with add/remove)
- Bulk actions: mark all read / mark all unread (scoped to current filter)
- Export: reading list → markdown → native share sheet
- Recent summaries: collapsible panel on summarize screen
- Unread badge on Reading List tab
- Light/dark theme toggle (persisted)
- Settings: summarization defaults (length, model, custom prompt), integrations (OpenAI/Anthropic/Readeck/webhook keys), language selector (EN/PT, persisted)
- Batch notifications: local push when batch completes in background ("N/M articles ready")
- User management: admin-only screen to list, create (with role selector), and delete users; "Manage Users" entry visible only to admins in Settings
- Android share intent: share a URL from any app → Briefen opens, switches to URL tab, pre-fills the field (cold start via `getInitialUrl` MethodChannel; warm start via `onNewIntent` push)
- Readeck bookmark browser (4th tab on Summarize screen): checks `/api/readeck/status` on mount; if unconfigured shows link to Settings; if configured shows search + paginated bookmark list; tap a bookmark to fetch article text and summarize, then navigates to summary detail
- Haptic feedback: swipe-to-mark-read → `mediumImpact`; swipe-to-delete threshold + delete dialog → `heavyImpact`; bulk mark-all → `mediumImpact`
- Skeleton loading: `SkeletonListView` replaces spinner while reading list loads; all cards pulse in sync via shared `AnimationController` passed through `InheritedWidget`
- List item animations: staggered fade + slide-up per item (40 ms/item, capped at 200 ms) via `_AnimatedItem` / `TweenAnimationBuilder`
- Offline reading cache: `ReadingListCache` persists first-page per-filter results to `SharedPreferences`; `readingListProvider` falls back to cache on any network error; `PaginatedSummaries.isOffline` flag drives an amber banner ("You're offline — showing cached content"); only active when no search/tag filter is set
- Biometric auth: optional lock-on-background using `local_auth`; `BiometricService` wraps `LocalAuthentication`; `BiometricEnabledNotifier` persists toggle to `SharedPreferences`; `BriefenApp` (`ConsumerStatefulWidget` + `WidgetsBindingObserver`) sets `_locked = true` on `paused` and shows `_LockScreen` overlay until biometric success; enabling requires a passing auth challenge; toggle hidden on devices without biometrics
- iOS share extension: `ShareExtension` target writes URL to App Group (`group.dev.azurecoder.briefen`) then opens `briefen://share`; `AppDelegate` reads the URL and passes it through the `dev.azurecoder.briefen/share` MethodChannel; fully configured in `project.pbxproj` — no manual Xcode steps needed beyond registering the App Group in Apple Developer Portal

**Structure (`mobile/lib/`):**
- `main.dart` — entry point; initializes notifications; uses `ProviderContainer` + `UncontrolledProviderScope` (not `ProviderScope`) so the share MethodChannel can update providers before the widget tree builds
- `app.dart` — `ConsumerStatefulWidget` + `WidgetsBindingObserver`; `MaterialApp.router` with `builder` for biometric lock overlay; locks on `AppLifecycleState.paused`, auto-prompts on `resumed`
- `core/api/` — `ApiClient` (Dio + Basic Auth interceptor; per-request `Options(receiveTimeout:)` — 310 s for single-URL/text summarize, 10 min for batch), `api_exceptions.dart` (sealed: `AuthException`, `NetworkException`, `ApiTimeoutException`, etc.)
- `core/auth/` — `AuthNotifier` (4 states: `unknown` → `unauthenticated` / `needsSetup` / `authenticated`), `AuthStorage` (secure storage wrapper), `BiometricService` (wraps `LocalAuthentication`), `BiometricEnabledNotifier` (`StateNotifierProvider<bool>` persisted to `SharedPreferences`)
- `core/locale/` — `LocaleNotifier` (`NotifierProvider<Locale>`), persists language code in `SharedPreferences`
- `core/router.dart` — GoRouter using `refreshListenable` (NOT `ref.watch`) to avoid remounting screens on auth state changes; `ScaffoldWithNavBar` reads `unreadCountProvider` for badge
- `core/notifications/` — `NotificationService` wrapping `FlutterLocalNotificationsPlugin`
- `core/theme/` — Material 3 seed color `#5b50e8` (matches web app primary purple), `ThemeMode` stored in `SharedPreferences`
- `features/summarize/` — `SummarizeScreen` is a `ConsumerStatefulWidget` with a manual `TabController` (URL/Text/Batch/Readeck); `sharedUrlProvider` (`StateProvider<String?>`) holds incoming share-intent URLs and triggers tab switch + `UrlInput` pre-fill via `ref.listen`; `SummarizeActionNotifier._run()` shared path for URL/Text; `BatchSummarizeNotifier` for sequential multi-URL with foreground service + 10-min per-article timeout + background notification on complete; `RecentSummaries` collapsible; `SummaryDisplay` tappable card
- `features/reading_list/` — paginated list with load-more (`ReadingListNotifier` is an `AutoDisposeAsyncNotifier` that accumulates pages; `loadMore()` appends next page, rolls back page counter on failure; filter/search/tag change resets to page 0); filter chips, swipe gestures (with haptics), staggered item animations, skeleton loading, offline banner; `ReadingListCache` (`SharedPreferences`-backed, per-filter, falls back on network error); `SummaryDetailScreen` (cache-first → `GET /api/summaries/{id}` fallback, auto-marks read with `_disposed` guard, inline notes/tags editing, Make shorter/longer/Regenerate buttons for URL-based summaries, tag chips navigate to filtered reading list); all error paths (toggle-read, delete, bulk actions, load-more) catch and show snackbars
- `features/setup/` — first-run setup + login screens; both show the Briefen SVG logo (`assets/images/logo.svg`) via `flutter_svg`, colored with `colorScheme.primary`
- `features/settings/` — `domain/user_settings.dart` + `domain/llm_models.dart`; `SettingsNotifier` (`AsyncNotifierProvider<UserSettings>`) with `save(patch)` for partial updates; `modelsProvider` fetches `GET /api/models`; screen has 6 sections: Account (+ "Manage Users" tile for admins), Summarization (length/model/custom prompt), Integrations (API keys + URLs via edit dialogs; Readeck URL + API key combined into one `_ReadeckTile` dialog), Appearance (theme + language + biometric toggle, hidden if unavailable), About, Logout
- `features/users/` — `AppUser` domain model; `UsersRepository` (`GET/POST/DELETE /api/users`); `usersProvider` (`FutureProvider.autoDispose`); `UsersScreen` with FAB to create, delete button hidden for self/mainAdmin; `/settings/users` route pushed over root navigator (admin-only)
- `l10n/` — ARB-based i18n: `app_en.arb` (source), `app_pt.arb`; generated output in `l10n/generated/`
- `assets/images/logo.svg` — Briefen lightning bolt mark; uses `currentColor` so `ColorFilter.mode(colorScheme.primary, BlendMode.srcIn)` recolors it for light/dark

**State management patterns & known gotchas:**
- `summaryDetailProvider` uses `ref.read(readingListProvider)` (sync cache hit) then falls back to `GET /api/summaries/{id}` — never `ref.watch` the filtered list, or auto-mark-read will cause "No results found" when the summary leaves the `unread` filter
- `TextEditingController` disposal in dialogs must use `Future.delayed(400ms, controller.dispose)` — `addPostFrameCallback` fires before the dialog exit animation finishes, causing `_dependents.isEmpty` assertion crash
- Auto-mark-read timer guard: `_disposed = true` set in `dispose()` before `super.dispose()`; always check `if (_disposed) return` inside the timer callback
- `ReadingListActions` methods all rethrow — all call sites in `reading_list_screen.dart` wrap in try/catch and show snackbars (toggle-read, delete, bulk actions, load-more)
- After summarize (single or batch): invalidate both `unreadCountProvider` and `readingListProvider`
- Network errors at app startup do NOT force-logout — `AuthNotifier` only clears credentials on `AuthException`; other errors keep the user authenticated with stored credentials
- `import 'package:flutter/foundation.dart' hide Summary` in `summary_detail_screen.dart` — Flutter's `foundation` exports a `Summary` annotation that conflicts with the domain model
- `unreadCountProvider` watches `authProvider.username` — returns 0 immediately when `username` is null (unauthenticated or user switch) and refetches for the new user; without this, the badge shows the previous user's stale count after login

**Platform notes:**
- Android: core library desugaring enabled (`isCoreLibraryDesugaringEnabled = true`); permissions: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `USE_BIOMETRIC`; `android:hasFragileUserData="true"` prompts user to keep data on uninstall; `ACTION_SEND text/plain` intent filter enables share-from-browser; `MainActivity` handles `onNewIntent` + `configureFlutterEngine` for the `dev.azurecoder.briefen/share` MethodChannel
- iOS: bundle identifier set via `PRODUCT_BUNDLE_IDENTIFIER`; `NSFaceIDUsageDescription` in Info.plist; `briefen://` URL scheme registered; App Group `group.dev.azurecoder.briefen` in both `Runner.entitlements` and `ShareExtension.entitlements`; **iOS Share Extension** — target `dev.azurecoder.briefen.ShareExtension` fully configured in `project.pbxproj`: entitlements, Info.plist, bundle ID all set; `Base.lproj/` empty (storyboard removed); `NSExtensionPrincipalClass` set to `$(PRODUCT_MODULE_NAME).ShareViewController`; remaining step: register the App Group in Apple Developer Portal and enable it via Xcode Signing & Capabilities on both targets (required for `UserDefaults(suiteName:)` to work at runtime); **Xcode build cycle fix**: `Embed Foundation Extensions` phase moved before `Thin Binary` in Runner build phases — without this ordering, Xcode's build system creates a cycle because "Thin Binary" scans the entire app bundle (including PlugIns/) but the extension hasn't been embedded yet
- Localization: `l10n.yaml` uses `output-dir: lib/l10n/generated` (no `synthetic-package`, removed in Flutter 3.41)
- Hooks: `.claude/settings.local.json` auto-runs `dart format` on `.dart` edits, `flutter gen-l10n` on `.arb` edits, `eslint --fix` on `.js`/`.jsx` edits

**Tests (`test/`):**
- `test/core/auth/auth_notifier_test.dart` — 5 unit tests for `AuthNotifier`: no credentials→unauthenticated, valid credentials→authenticated, 401→unauthenticated+cleared, network error→stays authenticated (graceful degradation), logout; uses `FakeAuthStorage` + `MockApiClient` (mocktail); `_checkSetupStatus` uses unreachable URL so it silently returns false, keeping tests focused on auth logic
- `test/features/reading_list/reading_list_notifier_test.dart` — 8 unit tests for `ReadingListNotifier`: initial load, `loadMore` appends + no-op on last page + rollback on failure, filter change resets, offline cache fallback + rethrow when no cache
- `test/features/settings/biometric_notifier_test.dart` — 5 unit tests for `BiometricEnabledNotifier`: starts false, persists true/false to SharedPreferences, restores on new notifier instance; note: `pumpEventQueue()` needed after provider read to let `_load()` complete; must read provider before draining queue (lazy init)
- `test/features/settings/settings_notifier_test.dart` — 3 unit tests for `SettingsNotifier`: build loads settings, `save(patch)` updates only patched fields, save exposes server response as new state; uses `FakeSettingsRepository`
- `test/features/users/users_screen_test.dart` — 5 widget tests for `UsersScreen`: renders user rows, empty state, FAB, retry on error, app bar title; uses `_FakeAuthNotifier` that sets state via `Future.microtask` to avoid running real credential check
- `test/helpers/test_app.dart` — `buildTestApp()` helper wraps widget in `ProviderScope` + `MaterialApp` with localizations

**Test coverage gaps (known, by priority):**
- `ApiClient` — error type mapping (401→`AuthException`, timeout→`ApiTimeoutException`); hard to unit test without a mock HTTP adapter package
- `SummarizeActionNotifier` — URL vs text paths, timeout handling
- `BatchSummarizeNotifier` — sequential processing, foreground service lifecycle
- `unreadCountProvider` — stale-cache-on-user-switch regression risk
- Widget tests for `SummarizeScreen`, `ReadingListScreen`, `SummaryDetailScreen`

### Documentation (`docs/`)
- `docs/getting-started.md` — step-by-step self-hosting guide
- `docs/environment-variables.md` — complete env var reference (single source of truth)
- `docs/reverse-proxy.md` — Nginx, Caddy, Traefik configuration examples

## Custom Slash Commands (`.claude/commands/`)

Project-specific agents invoked via `/command-name`:

| Command | Purpose |
|---|---|
| `/backend` | Spring Boot specialist — Java, REST endpoints, services, persistence, security |
| `/frontend` | React specialist — hooks, components, CSS modules, i18next, Vitest |
| `/mobile` | Flutter specialist — full mobile context (state, routing, auth, notifications) |
| `/mobile-api` | Mobile API layer — Dio client, repositories, data models, error handling |
| `/mobile-ui` | Mobile presentation — screens, widgets, Material 3 theming, navigation |
| `/mobile-test` | Mobile testing — unit, widget, and integration tests |
| `/security` | Security audit specialist — SSRF, auth, CSP, input validation, secrets |
| `/uiux` | UX/design review — layout, accessibility, mobile UX patterns |

## Key Conventions

- **No TypeScript** — frontend is plain JavaScript; backend is Java 25
- **Spring Boot 4.0.x** — uses Jackson 3 (`tools.jackson.*` packages, not `com.fasterxml.jackson`)
- **CSS approach** — plain CSS + CSS custom properties for dark/light theming; CSS modules for component scoping
- **All UI strings** go in `frontend/src/locales/en.json` (i18next); accessed via the `STRINGS` proxy in `constants/strings.js`, never hardcoded in components
- **Frontend tests** — Vitest + Testing Library + MSW (configured in `frontend/src/test/`)
- **Backend tests** — JUnit 5 via Spring Boot Test; WireMock for HTTP integration tests
- **E2E tests** — Playwright in `e2e/`
- **pnpm** is the frontend package manager (not npm/yarn)
- **New env vars** — when adding one, update all of: `application.yml`, `.env.example`, `docs/environment-variables.md`, and `docker-compose.sample.yml` (as a commented entry)
- **Auth is always on** — `SecurityConfig` requires authentication on all routes except `/actuator/health` and `/api/setup/**`; the admin account is created through the browser-based first-run setup flow (`SetupService`)
- **Hibernate ddl-auto** — schema is managed by `ddl-auto: update` (in `application.yml`) with a custom `SchemaInitializer` for SQLite-specific column migrations. No Flyway dependency exists in the project
