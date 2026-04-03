# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Briefen** — a local-first article summarizer. Paste a URL → Jsoup fetches the article → Ollama (local LLM) or OpenAI summarizes it → result is cached in SQLite. No cloud dependencies by default; everything runs locally.

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

### Testing
```bash
cd backend && ./mvnw test       # Backend unit tests
cd frontend && pnpm lint        # Frontend linting (no test framework configured)
make e2e                        # Playwright E2E tests (requires running app)
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
pnpm dev       # Dev server on :5173
pnpm build     # Production build
pnpm lint      # ESLint
```

### Backend
```bash
cd backend
./mvnw spring-boot:run   # Run app
./mvnw test              # Run tests
./mvnw clean package     # Build JAR
```

## Architecture

```
# Local dev
Browser (React/Vite :5173)
  → /api/* proxied to Spring Boot (:8080)
    → Jsoup (fetch article HTML)
    → Ollama (:11434, local LLM) or OpenAI (cloud, optional)
    → SQLite (file-based, ./data/briefen.db)

# Docker (single image)
Browser → Spring Boot (:8080, serves React static + API)
    → Ollama (external) or OpenAI (external)
    → SQLite (local file)
```

### Backend (`backend/src/main/java/com/briefen/`)
- **controller/** — REST endpoints:
  - `SummarizeController` — all `/api/summarize` and `/api/summaries` routes (CRUD, export, read status, notes)
  - `ModelsController` — `/api/models` lists available LLM providers and models
  - `SettingsController` — `/api/settings` read/update user preferences
  - `ReadeckController` — `/api/readeck/*` proxies requests to a user-configured Readeck instance (API key stays server-side)
  - `GlobalExceptionHandler` — maps exceptions to HTTP responses
- **service/** — core logic: article fetcher (Jsoup with Next.js/React SSR support), summarizer (Ollama and OpenAI via RestClient), and an orchestration service
- **model/** — plain domain POJOs (no persistence annotations): `Summary` (url, title, summary, modelUsed, createdAt, isRead, savedAt, notes) and `UserSettings`
- **persistence/** — SQLite persistence layer:
  - `SummaryPersistence` / `SettingsPersistence` — interfaces used by services and controllers
  - `persistence/sqlite/` — JPA implementations using JpaRepository + JpaSpecificationExecutor
- **dto/** — request/response records
- **config/** — `OllamaProperties`, `OpenAiProperties`, RestClient beans, `OllamaHealthIndicator`, `WebConfig` (SPA static file serving), `SqliteConfig`

Key `application.yml` settings (all configurable via env vars):
```yaml
spring.datasource.url: jdbc:sqlite:${BRIEFEN_DB_PATH:./data/briefen.db}
server.port: ${SERVER_PORT:8080}
ollama.base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
ollama.model: ${OLLAMA_MODEL:gemma3:4b}
ollama.timeout: 300s
```

### Frontend (`frontend/src/`)
- **App.jsx** — React Router: `/` (home/summarize), `/reading-list`, `/settings`
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
- **components/** — UI components; no component library, plain CSS + CSS modules
- **constants/strings.js** — all user-facing strings centralized here (i18n-ready)
- **utils/** — shared utilities (relative date formatting)

All `/api` requests go through Vite's dev proxy to the Spring Boot backend. Fetch calls use `AbortController` for cancellation support.

### Infrastructure
- **SQLite** — file-based database at `./data/briefen.db`, no Docker service needed. Schema auto-managed by Hibernate `ddl-auto: update`
- **Ollama** — local LLM; Docker Compose pulls `gemma2:2b`, `gemma3:4b`, `llama3.2:3b` on first start
- **OpenAI** — optional cloud provider; API key configured in browser settings, stored server-side
- **Readeck** — optional bookmark integration; URL and API key configured in browser settings

## Key Conventions

- **No TypeScript** — frontend is plain JavaScript; backend is Java 25
- **Spring Boot 4.0.x** — uses Jackson 3 (`tools.jackson.*` packages, not `com.fasterxml.jackson`)
- **CSS approach** — plain CSS + CSS custom properties for dark/light theming; CSS modules for component scoping
- **All UI strings** go in `frontend/src/constants/strings.js`, never hardcoded in components
- **No frontend tests** configured; backend uses JUnit 5 via Spring Boot Test; E2E uses Playwright
- **pnpm** is the frontend package manager (not npm/yarn)
