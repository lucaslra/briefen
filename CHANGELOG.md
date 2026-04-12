# Changelog

All notable changes to Briefen are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Briefen uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- HTTP Basic Auth with browser-based first-run setup — on first launch, the browser prompts you to create an admin account with a strong password (8+ chars, uppercase, lowercase, digit, special character). All routes except `/actuator/health` and `/api/setup/**` require authentication.
- Outgoing webhook notifications — set `BRIEFEN_WEBHOOK_URL` to receive a JSON `summary.completed` event every time a summary is saved. Fire-and-forget on a virtual thread; failures are logged and do not affect summarization. Compatible with Home Assistant, ntfy, Gotify, and any HTTP endpoint.
- Cloud LLM API key seeding via environment variables — set `BRIEFEN_OPENAI_API_KEY` or `BRIEFEN_ANTHROPIC_API_KEY` to pre-configure cloud providers at deploy time without logging into the UI. Keys are written into admin settings on first startup only; existing values in the database are never overwritten.
- Sub-path deployment support — set `SERVER_CONTEXT_PATH` (e.g. `/briefen/`) to serve the app at a URL prefix. Requires a local image build with `--build-arg APP_BASE_PATH=<path>` so Vite bakes the correct asset paths; the pre-built GHCR image always uses `/`. React Router `basename` is wired to `import.meta.env.BASE_URL` automatically.
- Docker HEALTHCHECK honours `SERVER_CONTEXT_PATH` at runtime — the health probe URL is now `http://localhost:8080${SERVER_CONTEXT_PATH:-}/actuator/health` so it works correctly when a context path is configured.

---

## [0.1.0] — 2026-04-07

### Added

- **Article summarization** — paste a URL or raw text; Jsoup fetches the content and a local or cloud LLM summarizes it
- **Batch summarization** — process multiple URLs sequentially with per-URL progress tracking
- **Reading list** — save summaries, filter by read/unread/all, full-text search, per-summary notes, and Markdown export
- **Multiple LLM providers** — Ollama (local, default) and optional OpenAI and Anthropic cloud models; provider and model selectable per-session
- **Readeck integration** — browse and one-click summarize articles from a self-hosted [Readeck](https://readeck.org) instance; API key is proxied server-side and never exposed to the browser
- **Summary length control** — short / standard / detailed modes; regenerate with a different length at any time
- **Dark/light theme** — toggleable, persisted in `localStorage`
- **Web notifications** — opt-in browser notification when a long-running summary completes
- **SQLite persistence** — single-file database at `./data/briefen.db`; zero setup, trivially portable
- **Hibernate auto-DDL schema management** — schema managed by `ddl-auto: update` with a custom `SchemaInitializer` for SQLite-specific column migrations; no separate migration tool required
- **Docker single-image build** — multi-stage Dockerfile (Node → JDK → JRE); serves both the React SPA and the Spring Boot API from one container
- **Dockerfile `HEALTHCHECK`** — polls `/actuator/health` so orchestrators (Docker Compose, Kubernetes) know when the app is ready
- **Security response headers** — `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy` applied via `SecurityHeadersFilter`
- **Startup validation** — logs warnings at startup when the database directory is not writable or Ollama is unreachable; does not fail startup so cloud-only deployments work without Ollama
- **Reverse proxy support** — `SERVER_FORWARD_HEADERS_STRATEGY` environment variable; set to `FRAMEWORK` to trust `X-Forwarded-*` headers from Nginx, Traefik, or Caddy
- **Browser bookmarklet** — one-click article sending from any page without copy-pasting
- **App version in UI** — Settings page footer shows the running version (`/api/version` endpoint backed by Maven build-info)
- **Anthropic API key persistence fix** — API key was silently dropped on save due to a missing entity field; corrected in the SQLite persistence layer

### Infrastructure

- Spring Boot 4.0.5 / Java 25 backend
- React 19 + Vite 8 frontend (plain JavaScript, no TypeScript)
- SQLite via `sqlite-jdbc` + Hibernate Community Dialects
- Hibernate `ddl-auto: update` + custom `SchemaInitializer` for schema management
- Ollama default model: `gemma3:4b` (128K context, strong summarization quality)
- Docker Compose for local development (Ollama service); separate single-image compose file for self-hosting

---

[Unreleased]: https://github.com/lucaslra/briefen/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/lucaslra/briefen/releases/tag/v0.1.0
