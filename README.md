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
- **Multiple LLM providers** — Ollama (local, default), OpenAI, or Anthropic Claude (optional, API key required)
- **Readeck integration** — browse and summarize bookmarks from a self-hosted Readeck instance
- **Length control** — shorter, default, or longer summaries; regenerate on demand
- **Export** — download summaries as Markdown
- **Dark/light theme** — toggleable, persisted in localStorage
- **Web notifications** — get notified when long-running summaries complete

## Architecture

```
Browser (React) → Spring Boot API → Jsoup (fetch article) → Ollama or OpenAI (summarize) → SQLite or PostgreSQL (persist)
```

| Layer          | Technology                                 |
|----------------|--------------------------------------------|
| Frontend       | React 19 (Vite, pnpm, plain JS)           |
| Backend        | Java 25, Spring Boot 4.0.5, Maven         |
| LLM            | Ollama (local), OpenAI, or Anthropic Claude (cloud) |
| Database       | SQLite (default) or PostgreSQL (optional)  |
| Orchestration  | Docker Compose                             |

## Prerequisites

- **Java 25** (Eclipse Temurin recommended)
- **Node.js 22+** and **pnpm**
- **Docker** (with Docker Compose) — required for Ollama

## Quick Start

### Self-Hosting (Docker — no build required)

The fastest path to a running instance. Uses the pre-built image from GHCR.

```bash
# Start the full stack (Ollama + Briefen app)
docker compose -f docker-compose.sample.yml up -d

# Watch the logs — Ollama pulls models on first run (~1–5 min depending on your connection)
docker compose -f docker-compose.sample.yml logs -f
```

Open **http://localhost:8080**. On first launch you'll see the **Setup** screen — create your admin account with a username and strong password. This only appears once; subsequent visits go straight to login.

> See **[docs/getting-started.md](docs/getting-started.md)** for a full walkthrough including reverse proxy setup, cloud LLM providers, and backups.

### Local Development

```bash
# 1. Start infrastructure (Ollama only)
make up

# 2. Wait for Ollama to pull the model (first time only, ~1–2 min)
docker compose logs -f ollama

# 3. Start backend and frontend with hot-reload
make dev
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Health check: http://localhost:8080/actuator/health

Or run each service independently:

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

Briefen supports two database engines: **SQLite** (default) and **PostgreSQL** (optional).

### SQLite (default)

SQLite is a file-based database that requires zero setup. The database file is created automatically at `./data/briefen.db` on first startup. To customize the path, set the `BRIEFEN_DB_PATH` environment variable.

No additional configuration is needed — SQLite is the default when `BRIEFEN_DB_TYPE` is unset or set to `sqlite`.

### PostgreSQL (optional)

PostgreSQL is available for larger-scale deployments, multi-instance setups, or environments where a dedicated database server is preferred.

**Environment variable setup:**

```yaml
BRIEFEN_DB_TYPE: postgres
BRIEFEN_DATASOURCE_URL: jdbc:postgresql://localhost:5432/briefen
BRIEFEN_DATASOURCE_USERNAME: briefen
BRIEFEN_DATASOURCE_PASSWORD: changeme
```

All three `BRIEFEN_DATASOURCE_*` variables are required when `BRIEFEN_DB_TYPE=postgres`. The app fails fast with a clear error if any are missing.

**Docker Compose (development):**

```bash
docker compose -f docker-compose.yml -f docker-compose.postgres.yml up -d
```

**Docker Compose (self-hosting):** Uncomment the `postgres` service and related environment variables in `docker-compose.sample.yml`. See the comments in the file for step-by-step instructions.

The database schema is created automatically on first startup via Hibernate's auto-DDL. No manual migration step is required.

### Backup & Restore

#### SQLite

**Your entire Briefen state** (summaries, notes, API keys, settings) lives in a single SQLite file.

> ⚠️ A plain `cp` or `tar` of a **running** SQLite database can produce a corrupt backup if a write occurs mid-copy. Use one of the safe methods below.

#### Online backup (app stays running) — Docker

SQLite's built-in `.backup` command is crash-safe under concurrent writes. This is the recommended method for Docker deployments.

```bash
# Create a safe online backup while the app is running
docker exec briefen-app sqlite3 /data/briefen.db ".backup /data/briefen-backup.db"

# Copy the backup file out of the container volume to the host
docker run --rm \
  -v briefen_briefen_data:/data \
  -v "$(pwd)":/backup \
  alpine cp /data/briefen-backup.db /backup/briefen-backup-$(date +%Y%m%d).db

# (Optional) remove the temporary backup file from the volume
docker exec briefen-app rm /data/briefen-backup.db
```

#### Offline backup (stop → copy → restart) — Docker

Use this if `sqlite3` is not available in the container or you want a compressed archive.

```bash
# Stop the app to ensure no writes during backup
docker compose -f docker-compose.sample.yml stop app

# Create a compressed archive from the volume
docker run --rm \
  -v briefen_briefen_data:/data \
  -v "$(pwd)":/backup \
  alpine tar czf /backup/briefen-backup-$(date +%Y%m%d).tar.gz -C /data .

# Restart
docker compose -f docker-compose.sample.yml start app
```

#### Restore — Docker

```bash
docker compose -f docker-compose.sample.yml down

docker run --rm \
  -v briefen_briefen_data:/data \
  -v "$(pwd)":/backup \
  alpine sh -c "rm -rf /data/* && tar xzf /backup/briefen-backup-YYYYMMDD.tar.gz -C /data"

docker compose -f docker-compose.sample.yml up -d
```

#### Local / bare-metal deployments

```bash
# Safe online backup (requires sqlite3 CLI)
sqlite3 ./data/briefen.db ".backup ./briefen-backup-$(date +%Y%m%d).db"

# Or stop the app first, then copy
cp ./data/briefen.db ./briefen-backup-$(date +%Y%m%d).db

# Restore
cp ./briefen-backup-YYYYMMDD.db ./data/briefen.db
```

#### Automated backups (optional)

For continuous replication to S3-compatible storage (Cloudflare R2, Backblaze B2, MinIO), add a [Litestream](https://litestream.io) sidecar alongside Briefen. Litestream streams SQLite's WAL to object storage in real time with no application changes required.

#### PostgreSQL

If you're using PostgreSQL (`BRIEFEN_DB_TYPE=postgres`), use standard PostgreSQL backup tools:

```bash
# Backup
docker exec briefen-postgres pg_dump -U briefen briefen > briefen-backup-$(date +%Y%m%d).sql

# Restore
docker exec -i briefen-postgres psql -U briefen briefen < briefen-backup-YYYYMMDD.sql
```

> ⚠️ **`make clean-all` destroys all data** including the database volume and Ollama model weights. Run it only when you want a completely fresh state.

## Changing the Ollama Model

The default model is `gemma3:4b`, chosen for its excellent summarization quality with a 128K context window. To change it, set the `OLLAMA_MODEL` environment variable:

```yaml
# docker-compose.sample.yml → environment:
OLLAMA_MODEL: gemma2:2b
```

Or in a `.env` file for local development:

```bash
OLLAMA_MODEL=gemma2:2b
```

The model must be pulled in Ollama before it can be used. Models are pulled automatically on first start in the default Docker Compose setup. To pull a model manually:

```bash
docker exec briefen-ollama ollama pull gemma2:2b
```

Good alternatives: `gemma2:2b` (faster, lighter), `llama3.2:3b` (~2 GB, well-rounded), `mistral` (7B, higher quality), `phi3` (3.8B, efficient).

Users can also override the model per-session from the model picker in the browser without changing any server configuration.

## OpenAI Integration

Briefen can optionally use OpenAI models instead of local Ollama.

**Option A — environment variable (recommended for self-hosted deployments):**

Set `BRIEFEN_OPENAI_API_KEY` before first startup. Briefen seeds it into the admin settings automatically — no UI visit required.

```yaml
# docker-compose.sample.yml → environment:
BRIEFEN_OPENAI_API_KEY: sk-...
```

**Option B — browser UI:**

1. Go to **Settings → Integrations**
2. Enter your OpenAI API key
3. Select an OpenAI model from the model picker

The API key is stored server-side in SQLite and masked in the UI. No data is sent to OpenAI unless you explicitly select an OpenAI model.

## Anthropic Integration

Briefen can optionally use Anthropic Claude models.

**Option A — environment variable (recommended for self-hosted deployments):**

Set `BRIEFEN_ANTHROPIC_API_KEY` before first startup. Briefen seeds it into the admin settings automatically.

```yaml
# docker-compose.sample.yml → environment:
BRIEFEN_ANTHROPIC_API_KEY: sk-ant-...
```

**Option B — browser UI:**

1. Go to **Settings → Integrations**
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

Briefen uses **HTTP Basic Auth** on every route except `/actuator/health` and `/api/setup/**` (first-run account creation). Authentication is always active — on first launch, the browser-based setup screen prompts you to create an admin account with a strong password.

**Using the API with auth:**

```bash
curl -u alice:changeme http://localhost:8080/api/summaries
```

> ⚠️ **Always use HTTPS when exposed to untrusted networks.** HTTP Basic Auth transmits credentials as base64, which is trivially decoded on unencrypted connections. Put Briefen behind a TLS-terminating reverse proxy (Nginx, Traefik, Caddy) before exposing it to the internet.

### Ollama exposure

Ensure the Ollama port (`11434`) is **not** exposed to the network. The default `docker-compose.yml` binds it to `127.0.0.1` — do not change this to `0.0.0.0` unless you have a specific reason. Ollama has no authentication.

See [SECURITY.md](SECURITY.md) for the full threat model, accepted risks, and how to report vulnerabilities.

---

## Deploying Behind a Reverse Proxy (Nginx / NPM)

If you expose Briefen or Ollama through a reverse proxy (e.g. **Nginx Proxy Manager**, Traefik, Caddy), you must raise the default proxy timeouts. Summarization of long articles can take 2–3 minutes on local models — the default 60s timeout will drop the connection before the response arrives, showing the user a "Could not reach the server" error even though the summary completed fine on the backend.

### Nginx Proxy Manager — Advanced tab

Open each affected proxy host → **Edit → Advanced** and paste:

```nginx
proxy_read_timeout    310s;
proxy_send_timeout    310s;
proxy_connect_timeout 10s;
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
    proxy_pass            http://briefen:8080;
    proxy_read_timeout    310s;
    proxy_send_timeout    310s;
    proxy_connect_timeout 10s;
}
```

### Why 310s?

Briefen's Ollama client timeout is **300s** (`ollama.timeout` in `application.yml`) and Tomcat's connection timeout is set to **310s**. Setting the proxy to 310s ensures it always outlasts the longest possible Ollama request without dropping the connection prematurely.

---

## Configuration Reference

All runtime behaviour is controlled through environment variables. In local development, place these in a `.env` file at the repo root — it is loaded automatically via `spring.config.import`.

| Variable | Default | Description |
|---|---|---|
| `BRIEFEN_DB_TYPE` | `sqlite` | Database engine: `sqlite` (default) or `postgres`. |
| `BRIEFEN_DB_PATH` | `./data/briefen.db` | Path to the SQLite database file. In Docker, point this at a named-volume mount path (e.g. `/data/briefen.db`). |
| `BRIEFEN_DATASOURCE_URL` | *(none)* | PostgreSQL JDBC URL. Required when `BRIEFEN_DB_TYPE=postgres`. |
| `BRIEFEN_DATASOURCE_USERNAME` | *(none)* | PostgreSQL username. Required when `BRIEFEN_DB_TYPE=postgres`. |
| `BRIEFEN_DATASOURCE_PASSWORD` | *(none)* | PostgreSQL password. Required when `BRIEFEN_DB_TYPE=postgres`. |
| `SERVER_PORT` | `8080` | HTTP port the server listens on. |
| `SERVER_CONTEXT_PATH` | `/` | URL sub-path prefix (e.g. `/briefen/` for `myserver.com/briefen/`). Requires a local image build with `--build-arg APP_BASE_PATH=<path>` to bake the matching asset paths into the frontend. The pre-built GHCR image always uses `/`. |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `NONE` | Set to `FRAMEWORK` when running behind a reverse proxy (Nginx, Traefik, Caddy) to trust `X-Forwarded-For` / `X-Forwarded-Proto` headers. |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Base URL of the Ollama API. Override when Ollama runs in a separate container or host. |
| `OLLAMA_MODEL` | `gemma3:4b` | Default Ollama model used for summarization. The model must be pulled in Ollama first. |
| `BRIEFEN_OPENAI_API_KEY` | *(empty — OpenAI disabled)* | OpenAI API key. When set on first startup, seeds into admin settings so cloud models are available immediately. Does not overwrite a key already saved in the database. |
| `BRIEFEN_ANTHROPIC_API_KEY` | *(empty — Anthropic disabled)* | Anthropic API key. Same first-startup seeding behaviour as `BRIEFEN_OPENAI_API_KEY`. |
| `BRIEFEN_CORS_ALLOWED_ORIGINS` | *(empty — CORS disabled)* | Comma-separated list of allowed CORS origins. Required when the Firefox extension or a custom frontend runs on a different origin than the backend (e.g. `moz-extension://*`). |
| `BRIEFEN_WEBHOOK_URL` | *(empty — webhooks disabled)* | HTTP/S URL to POST a JSON notification to whenever a summary is saved. Compatible with Home Assistant, ntfy, Gotify, and any HTTP endpoint. |
| `SERVER_BIND_ADDRESS` | `0.0.0.0` | Network interface the app binds to. Set to `127.0.0.1` when a reverse proxy runs on the same host. |
| `BRIEFEN_LOG_LEVEL` | `INFO` | Log verbosity for Briefen's own code. Values: `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. |
| `BRIEFEN_LOG_FORMAT` | *(unset — human-readable)* | Set to `json` for structured JSON logs (useful for Loki, Grafana). |
| `BRIEFEN_DEFAULT_PROMPT` | *(unset — built-in prompt)* | Custom default system prompt for summarization. Overrides the built-in prompt for all users. |

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

## Firefox Extension

The Briefen extension adds a toolbar button that sends the current tab's URL to your Briefen instance with one click.

### Installation

The extension is not yet published to AMO. Install it temporarily via `about:debugging`:

1. Open Firefox → address bar → `about:debugging#/runtime/this-firefox`
2. Click **Load Temporary Add-on…**
3. Navigate to `extension/` in the repo and select `manifest.json`

For persistent installation across Firefox restarts, sign the extension via the [AMO Developer Hub](https://addons.mozilla.org/developers/) or use Firefox Developer Edition which allows unsigned extensions.

### Configuration

After installing, click the extension icon → **Options** (or right-click → Manage Extension → Options):

| Field | Value |
|-------|-------|
| **Briefen URL** | Your instance URL, e.g. `http://localhost:8080` or `https://briefen.example.com` |
| **Username** | Your Briefen username (created during first-run setup) |
| **Password** | Your Briefen password |

### CORS requirement for remote instances

If your Briefen instance is on a different origin than the extension, add the extension origin to `BRIEFEN_CORS_ALLOWED_ORIGINS`:

```yaml
BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*
```

This is not required when Briefen is on `localhost`.

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
│       ├── config/          # DB profile activation, datasource config, security, RestClient beans
│       ├── controller/      # REST endpoints (Summarize, Settings, Models, Users, Setup, Readeck, etc.)
│       ├── dto/             # Request/response records
│       ├── exception/       # Custom exceptions
│       ├── model/           # Domain models (Summary, UserSettings, User)
│       ├── persistence/     # Database persistence layer — JPA (supports SQLite and PostgreSQL)
│       ├── service/         # Article fetcher, Ollama/OpenAI/Anthropic summarizer, orchestrator
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
