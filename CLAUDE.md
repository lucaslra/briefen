# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Briefen** — a local-first article summarizer. Paste a URL → Jsoup fetches the article → Ollama (local LLM), OpenAI, or Anthropic Claude summarizes it → result is cached in SQLite. No cloud dependencies by default; everything runs locally.

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
    → SQLite (file-based, ./data/briefen.db)

# Docker (single image, self-hosting)
Browser → Spring Boot (:8080, serves React static + API)
    → Ollama (Docker service or external) or OpenAI/Anthropic (external)
    → SQLite (named Docker volume)
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
- **model/** — plain domain POJOs (no persistence annotations): `Summary`, `UserSettings`, `User`
- **persistence/** — SQLite persistence layer:
  - `SummaryPersistence` / `SettingsPersistence` / `UserPersistence` — interfaces
  - `persistence/sqlite/` — JPA implementations using JpaRepository + JpaSpecificationExecutor
- **dto/** — request/response records
- **security/** — `BriefenUserDetails`, `BriefenUserDetailsService`
- **config/** — `SecurityConfig` (HTTP Basic Auth, always on; `/api/setup/**` unauthenticated for first-run flow), `SecurityHeadersFilter` (CSP, X-Frame-Options, etc.), `CorsConfig`, `OllamaProperties`, `OpenAiProperties`, `AnthropicProperties`, RestClient beans, `OllamaHealthIndicator`, `ApplicationReadinessValidator`, `WebConfig` (SPA routing), `SqliteConfig`

Key `application.yml` settings (all configurable via env vars):
```yaml
spring.datasource.url:          jdbc:sqlite:${BRIEFEN_DB_PATH:./data/briefen.db}
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
- **components/** — UI components; no component library, plain CSS + CSS modules
- **constants/strings.js** — all user-facing strings centralized here (i18n-ready)
- **utils/** — shared utilities (relative date formatting)

Vite config injects `__APP_COMMIT__` (git short hash) and `__BUILD_DATE__` as compile-time constants, displayed in the app footer. The `base` option reads `VITE_APP_BASE_PATH` (set by the Dockerfile `APP_BASE_PATH` build arg) for sub-path support.

All `/api` requests go through Vite's dev proxy to the Spring Boot backend. Fetch calls use `AbortController` for cancellation support.

### Firefox Extension (`extension/`)
- Manifest V2, vanilla JS
- Popup sends the active tab's URL to `POST /api/articles` (202 Accepted)
- Options page: Briefen instance URL, username, password
- Requires `BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*` for remote instances

### Infrastructure
- **SQLite** — file-based database at `./data/briefen.db`. Schema managed by **Flyway** (versioned migrations in `backend/src/main/resources/db/migration/`). Three tables: `users`, `summaries`, `settings`.
- **Ollama** — local LLM; Docker Compose pulls `gemma2:2b`, `gemma3:4b`, `llama3.2:3b` on first start
- **OpenAI** — optional cloud provider; API key seeded from `BRIEFEN_OPENAI_API_KEY` on first startup or configured via browser settings, stored server-side
- **Anthropic** — optional cloud provider; same pattern as OpenAI, key from `BRIEFEN_ANTHROPIC_API_KEY`
- **Readeck** — optional bookmark integration; URL and API key configured in browser settings

### Documentation (`docs/`)
- `docs/getting-started.md` — step-by-step self-hosting guide
- `docs/environment-variables.md` — complete env var reference (single source of truth)
- `docs/reverse-proxy.md` — Nginx, Caddy, Traefik configuration examples

## Key Conventions

- **No TypeScript** — frontend is plain JavaScript; backend is Java 25
- **Spring Boot 4.0.x** — uses Jackson 3 (`tools.jackson.*` packages, not `com.fasterxml.jackson`)
- **CSS approach** — plain CSS + CSS custom properties for dark/light theming; CSS modules for component scoping
- **All UI strings** go in `frontend/src/constants/strings.js`, never hardcoded in components
- **Frontend tests** — Vitest + Testing Library + MSW (configured in `frontend/src/test/`)
- **Backend tests** — JUnit 5 via Spring Boot Test; WireMock for HTTP integration tests
- **E2E tests** — Playwright in `e2e/`
- **pnpm** is the frontend package manager (not npm/yarn)
- **New env vars** — when adding one, update all of: `application.yml`, `.env.example`, `docs/environment-variables.md`, and `docker-compose.sample.yml` (as a commented entry)
- **Auth is always on** — `SecurityConfig` requires authentication on all routes except `/actuator/health` and `/api/setup/**`; the admin account is created through the browser-based first-run setup flow (`SetupService`)
- **Flyway not ddl-auto** — schema changes go in a new versioned migration file (`V{n}__description.sql`), never by editing existing migrations or relying on `ddl-auto: update`
