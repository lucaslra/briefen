# Briefen

[![CI](https://github.com/lucaslra/briefen/actions/workflows/ci.yml/badge.svg)](https://github.com/lucaslra/briefen/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-blue)](https://github.com/lucaslra/briefen)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-green)](https://github.com/lucaslra/briefen)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue)](https://github.com/lucaslra/briefen/blob/main/LICENSE)

**Article summaries, instantly.** Paste a URL to any online article, and Briefen will fetch, read, and summarize it using a local LLM — no API keys, no cloud dependencies.

## Features

- **Summarize any article** — paste a URL or raw text, get a concise summary
- **Batch summarization** — process multiple URLs at once
- **Reading list** — save, filter (unread/read/all), search, and annotate summaries
- **Multiple LLM providers** — Ollama (local, default) or OpenAI (optional, API key required)
- **Readeck integration** — browse and summarize bookmarks from a self-hosted Readeck instance
- **Length control** — shorter, default, or longer summaries; regenerate on demand
- **Export** — download summaries as Markdown
- **Dark/light theme** — toggleable, persisted in localStorage
- **Web notifications** — get notified when long-running summaries complete

## Architecture

```
Browser (React) → Spring Boot API → Jsoup (fetch article) → Ollama or OpenAI (summarize) → SQLite (persist)
```

| Layer          | Technology                                 |
|----------------|--------------------------------------------|
| Frontend       | React 19 (Vite, pnpm, plain JS)           |
| Backend        | Java 25, Spring Boot 4.0.5, Maven         |
| LLM            | Ollama (local) or OpenAI (cloud)           |
| Database       | SQLite (file-based, zero setup)            |
| Orchestration  | Docker Compose                             |

## Prerequisites

- **Java 25** (Eclipse Temurin recommended)
- **Node.js 22+** and **pnpm**
- **Docker** (with Docker Compose) — required for Ollama

## Quick Start

```bash
# 1. Start infrastructure (Ollama)
make up

# 2. Wait for Ollama to pull the model (first time only, ~1-2 min)
docker compose logs -f ollama

# 3. Start backend and frontend
make dev
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Health check: http://localhost:8080/actuator/health

Or run backend and frontend separately:

```bash
make backend   # in one terminal
make frontend  # in another terminal
```

### Cleanup

```bash
make clean      # stop containers, remove build artifacts (Ollama weights preserved)
make clean-all  # full reset — also removes Ollama model weights (re-download required)
```

`make clean` is safe to run frequently. Use `make clean-all` only when you want a completely fresh state.

The SQLite database file lives at `./data/briefen.db` — it is created automatically on first startup. To back up your data, copy this file. To reset, delete it.

## API

### Summarize an article

```
POST /api/summarize
Content-Type: application/json

{"url": "https://example.com/article"}
```

Add `?refresh=true` to force re-summarization of a previously cached URL.

### List recent summaries

```
GET /api/summaries?page=0&size=10
```

### Other endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/summaries/export` | Export summaries as Markdown |
| `GET` | `/api/summaries/unread-count` | Unread summary count |
| `PATCH` | `/api/summaries/{id}/read-status` | Toggle read status |
| `PATCH` | `/api/summaries/{id}/notes` | Update notes |
| `DELETE` | `/api/summaries/{id}` | Delete a summary |
| `GET` | `/api/models` | List available LLM providers and models |
| `GET/PUT` | `/api/settings` | Read/update user settings |
| `GET` | `/api/readeck/bookmarks` | List Readeck bookmarks (proxied) |

## Database

Briefen uses **SQLite** — a file-based database that requires zero setup. The database file is created automatically at `./data/briefen.db` on first startup. To customize the path, set the `BRIEFEN_DB_PATH` environment variable.

## Changing the Ollama Model

The default model is `gemma3:4b`, chosen for its excellent summarization quality with 128K context window. To change it:

1. Edit `docker-compose.yml` — change the model name in the `ollama` service command
2. Edit `backend/src/main/resources/application.yml` — update `ollama.model`
3. Restart: `make down && make up`

Good alternatives: `gemma2:2b` (faster, lighter), `mistral` (7B, higher quality), `llama3` (8B), `phi3` (3.8B).

## OpenAI Integration

Briefen can optionally use OpenAI models instead of local Ollama. To enable:

1. Go to **Settings → Integrations** in the browser UI
2. Enter your OpenAI API key
3. Select an OpenAI model from the model picker

The API key is stored server-side. No data is sent to OpenAI unless you explicitly select an OpenAI model.

## Anthropic Integration

Briefen can optionally use Anthropic Claude models. To enable:

1. Go to **Settings → Integrations** in the browser UI
2. Enter your Anthropic API key
3. Select a Claude model from the model picker

The API key is stored server-side. No data is sent to Anthropic unless you explicitly select a Claude model.

## Readeck Integration

Briefen can browse and summarize articles from a self-hosted [Readeck](https://readeck.org) instance:

1. Go to **Settings → Integrations** in the browser UI
2. Enter your Readeck URL and API key
3. Use the Readeck tab on the home page to browse bookmarks and summarize them

The Readeck API key never reaches the browser — all requests are proxied through the backend.

## Security & Network Exposure

Briefen has **no built-in authentication**. All API endpoints are open by design for single-user local use.

> **If you expose Briefen on a shared or public network**, add an authentication layer (HTTP Basic Auth, OAuth2 proxy, etc.) in front of it. Without auth, anyone who can reach the server can read your summaries, overwrite your API keys, and trigger LLM inference at your expense.

Additionally, ensure the Ollama port (`11434`) is **not** exposed to the network. The default `docker-compose.yml` binds it to `127.0.0.1` — do not change this to `0.0.0.0` unless you have a specific reason. Ollama has no authentication.

See [SECURITY.md](SECURITY.md) for the full threat model, accepted risks, and how to report vulnerabilities.

---

## Deploying Behind a Reverse Proxy (Nginx / NPM)

If you expose Briefen or Ollama through a reverse proxy (e.g. **Nginx Proxy Manager**, Traefik, Caddy), you must raise the default proxy timeouts. Summarization of long articles can take 2–3 minutes on local models — the default 60s timeout will drop the connection before the response arrives, showing the user a "Could not reach the server" error even though the summary completed fine on the backend.

### Nginx Proxy Manager — Advanced tab

Open each affected proxy host → **Edit → Advanced** and paste:

```nginx
proxy_read_timeout 310s;
proxy_send_timeout 310s;
proxy_connect_timeout 310s;
```

Apply this to **both** proxy hosts if both are behind NPM:

| Host | Why |
|------|-----|
| Briefen app | Browser ↔ Spring Boot requests can take 2–3 min on slow models |
| Ollama | Spring Boot ↔ Ollama inference requests can take equally long |

### Plain Nginx

Add the same directives inside the relevant `location` block:

```nginx
location / {
    proxy_pass         http://briefen:8080;
    proxy_read_timeout 310s;
    proxy_send_timeout 310s;
    proxy_connect_timeout 310s;
}
```

### Why 310s?

Briefen's Ollama client timeout is **300s** (`ollama.timeout` in `application.yml`) and Tomcat's connection timeout is set to **310s**. Setting the proxy to 310s ensures it always outlasts the longest possible Ollama request without dropping the connection prematurely.

---

## CSS Approach

Plain CSS with CSS custom properties and CSS Modules. Custom properties enable dark/light mode via a single `data-theme` attribute toggle on `<html>`. CSS Modules provide component-scoped styles without build tooling overhead. This keeps the stack minimal and consistent with the plain-JS-no-TypeScript philosophy.

## i18n Readiness

All user-facing strings are centralized in `frontend/src/constants/strings.js`. To add internationalization, replace this module with a library like `react-intl` or `i18next` and swap the constants for translation keys. No string literals appear in component JSX.

## Project Structure

```
briefen/
├── docker-compose.yml
├── Makefile
├── Dockerfile              # Multi-stage: frontend build → backend build → runtime
├── playwright.config.js    # E2E test configuration
├── e2e/                    # Playwright E2E tests
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/briefen/
│       ├── BriefenApplication.java
│       ├── config/          # Ollama/OpenAI properties, RestClient beans, health indicator
│       ├── controller/      # SummarizeController, SettingsController, ModelsController, ReadeckController
│       ├── dto/             # Request/response records
│       ├── exception/       # Custom exceptions
│       ├── model/           # Domain models (Summary, UserSettings)
│       ├── persistence/     # SQLite persistence layer (JPA)
│       ├── service/         # Article fetcher, Ollama/OpenAI summarizer, orchestrator
│       └── validation/      # URL validator
└── frontend/
    ├── vite.config.js
    └── src/
        ├── App.jsx          # React Router: /, /reading-list, /settings
        ├── constants/       # Centralized UI strings
        ├── hooks/           # useSummarize, useReadingList, useSettings, useReadeck, etc.
        ├── components/      # Header, UrlInput, SummaryDisplay, ReadingList, Settings, etc.
        ├── utils/           # Shared utilities (relative date formatting)
        └── styles/          # CSS variables, global reset
```
