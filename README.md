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

### Backup & Restore

**Your entire Briefen state** (summaries, notes, API keys, settings) lives in a single file. Backing up is just copying that file.

#### Docker deployments (named volume)

```bash
# Find where Docker stores the volume on your host
docker volume inspect briefen_briefen_data

# Back up — creates a compressed archive on the host
docker run --rm \
  -v briefen_briefen_data:/data \
  -v "$(pwd)":/backup \
  alpine tar czf /backup/briefen-backup-$(date +%Y%m%d).tar.gz -C /data .

# Restore — stops the app first to avoid write conflicts
docker compose down
docker run --rm \
  -v briefen_briefen_data:/data \
  -v "$(pwd)":/backup \
  alpine sh -c "rm -rf /data/* && tar xzf /backup/briefen-backup-YYYYMMDD.tar.gz -C /data"
docker compose up -d
```

#### Local / bare-metal deployments

```bash
# Back up
cp ./data/briefen.db ./briefen-backup-$(date +%Y%m%d).db

# Restore
cp ./briefen-backup-YYYYMMDD.db ./data/briefen.db
```

#### Automated backups (optional)

For continuous replication to S3-compatible storage (Cloudflare R2, Backblaze B2, MinIO), add a [Litestream](https://litestream.io) sidecar alongside Briefen. Litestream streams SQLite's WAL to object storage in real time with no application changes required.

> ⚠️ **`make clean-all` destroys all data** including the database volume and Ollama model weights. Run it only when you want a completely fresh state.

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

### Authentication

Briefen includes **optional HTTP Basic Auth**. By default it is disabled so local single-user installs work without any configuration. To enable it, set two environment variables:

```bash
BRIEFEN_AUTH_USERNAME=alice
BRIEFEN_AUTH_PASSWORD=changeme
```

When both are set, every route except `/actuator/health` requires a valid username and password. The browser will show a native auth dialog on first visit, and most HTTP clients (curl, Postman, etc.) support Basic Auth natively.

```bash
# curl example with auth
curl -u alice:changeme http://localhost:8080/api/summaries
```

> ⚠️ **Always use HTTPS when auth is enabled.** HTTP Basic Auth transmits credentials as base64, which is trivially decoded on an unencrypted connection. Put Briefen behind a TLS-terminating reverse proxy (Nginx, Traefik, Caddy) before exposing it to the internet.

When auth is not configured, all API endpoints remain open — this is intentional for local use.

### Ollama exposure

Ensure the Ollama port (`11434`) is **not** exposed to the network. The default `docker-compose.yml` binds it to `127.0.0.1` — do not change this to `0.0.0.0` unless you have a specific reason. Ollama has no authentication.

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

## Configuration Reference

All runtime behaviour is controlled through environment variables. In local development, place these in a `.env` file at the repo root — it is loaded automatically via `spring.config.import`.

| Variable | Default | Description |
|---|---|---|
| `BRIEFEN_DB_PATH` | `./data/briefen.db` | Path to the SQLite database file. In Docker, point this at a named-volume mount path (e.g. `/data/briefen.db`). |
| `SERVER_PORT` | `8080` | HTTP port the server listens on. |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `NONE` | Set to `FRAMEWORK` when running behind a reverse proxy (Nginx, Traefik, Caddy) to trust `X-Forwarded-For` / `X-Forwarded-Proto` headers. |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Base URL of the Ollama API. Override when Ollama runs in a separate container or host. |
| `OLLAMA_MODEL` | `gemma3:4b` | Default Ollama model used for summarization. The model must be pulled in Ollama first. |
| `BRIEFEN_CORS_ALLOWED_ORIGINS` | *(empty — CORS disabled)* | Comma-separated list of allowed CORS origins. Only required when the frontend is served from a different origin than the backend. |
| `BRIEFEN_AUTH_USERNAME` | *(empty — auth disabled)* | Username for HTTP Basic Auth. Set together with `BRIEFEN_AUTH_PASSWORD` to enable authentication. |
| `BRIEFEN_AUTH_PASSWORD` | *(empty — auth disabled)* | Password for HTTP Basic Auth. Set together with `BRIEFEN_AUTH_USERNAME`. Always use HTTPS when auth is enabled. |
| `BRIEFEN_WEBHOOK_URL` | *(empty — webhooks disabled)* | HTTP/S URL to POST a JSON notification to whenever a summary is saved. Compatible with Home Assistant, ntfy, Gotify, and any HTTP endpoint. |

---

## Webhook Notifications

Briefen can POST a JSON notification to any HTTP endpoint whenever a summary is saved. Set `BRIEFEN_WEBHOOK_URL` to enable it:

```bash
BRIEFEN_WEBHOOK_URL=https://ntfy.sh/my-briefen-topic
```

### Payload

```json
{
  "event": "summary.completed",
  "id": "3f2a1b...",
  "url": "https://example.com/article",
  "title": "Article Title",
  "model": "gemma3:4b",
  "createdAt": "2026-04-07T10:00:00Z"
}
```

Delivery is **fire-and-forget** on a virtual thread — webhook failures are logged at `WARN` level but never affect the summarization response. The connection and read timeout is 10 seconds.

### Integration examples

**ntfy (self-hosted push notifications)**
```bash
BRIEFEN_WEBHOOK_URL=https://ntfy.example.com/briefen
```

**Home Assistant**
```bash
BRIEFEN_WEBHOOK_URL=https://homeassistant.local:8123/api/webhook/briefen-summary-done
```

**Gotify**
```bash
# Gotify expects a different body format — use an intermediary like n8n or a small proxy
BRIEFEN_WEBHOOK_URL=https://gotify.example.com/message?token=YOUR_TOKEN
```

---

## Browser Integration

A bookmarklet lets you send any page to Briefen with one click — no copy-pasting required.

### Bookmarklet setup

1. Create a new browser bookmark (right-click the bookmarks bar → **Add page** or **New bookmark**)
2. Set the **name** to `Summarize with Briefen`
3. Set the **URL** to the snippet below, replacing `http://localhost:8080` with your Briefen base URL:

```
javascript:window.open('http://localhost:8080/?url='+encodeURIComponent(location.href))
```

Click the bookmark on any article page and Briefen opens in a new tab with the URL pre-filled, ready to summarize.

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
