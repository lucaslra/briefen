# Getting Started with Briefen

This guide walks you from zero to a fully running Briefen instance. Pick the path that matches your situation.

---

## Which path is right for you?

| I want to… | Use |
|---|---|
| Self-host with a pre-built image (no build required) | [Docker — pre-built image](#option-1-docker-pre-built-image-recommended) |
| Self-host with a custom sub-path or local Ollama tweak | [Docker — local build](#option-2-docker-local-build) |
| Hack on the code | [Local development](#option-3-local-development) |

---

## Prerequisites

All three paths require **Docker** with the Compose plugin (`docker compose` — note: no hyphen).

```bash
docker compose version   # should print v2.x or higher
```

The local development path additionally requires **Java 25** (Eclipse Temurin recommended) and **Node.js 22+** with **pnpm**.

---

## Option 1: Docker — pre-built image (recommended)

No `git clone` required. The pre-built image is published to GHCR for both `linux/amd64` and `linux/arm64` (Raspberry Pi / ARM NAS).

### Step 1 — Get the compose file

Either grab the sample file from the repo:

```bash
curl -O https://raw.githubusercontent.com/lucaslra/briefen/main/docker-compose.sample.yml
```

Or clone the repo and use it in place:

```bash
git clone https://github.com/lucaslra/briefen.git
cd briefen
```

### Step 2 — Start the stack

```bash
docker compose -f docker-compose.sample.yml up -d
```

On first run Ollama downloads `gemma3:4b` (~3.3 GB). Watch the progress:

```bash
docker compose -f docker-compose.sample.yml logs -f ollama
```

Briefen starts serving at **http://localhost:8080** once the health check turns green:

```bash
docker compose -f docker-compose.sample.yml ps   # STATUS should show "healthy"
```

### Step 3 — Create your admin account

1. Open **http://localhost:8080** — on first launch you'll see the **Setup** screen
2. Choose a username (3+ characters) and a strong password (8+ characters, must include uppercase, lowercase, digit, and special character)
3. Click **Create Account**

Your credentials are stored as a BCrypt hash in the database. This setup screen only appears once — subsequent visits go straight to the login page.

### Step 4 — Summarize something

1. Log in with the credentials you just created
2. Paste any article URL into the input box
3. Click **Summarize** — the first request may take 10–30 seconds while the model warms up

---

## Option 2: Docker — local build

Use this when you need to:
- Deploy at a URL sub-path (e.g. `myserver.com/briefen/`)
- Pin to a specific Ollama model or base URL not covered by the sample file
- Build on a platform not yet covered by the published image

### Step 1 — Clone the repo

```bash
git clone https://github.com/lucaslra/briefen.git
cd briefen
```

### Step 2 — Build the image

**Root deployment (default):**

```bash
docker build -t briefen:local .
```

**Sub-path deployment** — pass `APP_BASE_PATH` to bake the correct asset prefix into the frontend:

```bash
docker build --build-arg APP_BASE_PATH=/briefen/ -t briefen:local .
```

> `APP_BASE_PATH` must end with a slash and match `SERVER_CONTEXT_PATH` exactly.

### Step 3 — Edit docker-compose.sample.yml to use your local image

Replace:
```yaml
image: ghcr.io/lucaslra/briefen:latest
```

With:
```yaml
image: briefen:local
```

And if using a sub-path, add to `environment:`:
```yaml
SERVER_CONTEXT_PATH: /briefen/
```

### Step 4 — Start the stack and follow Steps 2–4 from Option 1

```bash
docker compose -f docker-compose.sample.yml up -d
```

---

## Option 3: Local development

For contributing to Briefen or running the backend and frontend with hot-reload.

### Step 1 — Clone and start infrastructure

```bash
git clone https://github.com/lucaslra/briefen.git
cd briefen

# Start Ollama only (the app runs outside Docker in dev mode)
make up

# Wait for models to be pulled
docker compose logs -f ollama
```

### Step 2 — Start the app

```bash
make dev   # starts both backend (port 8080) and frontend (port 5173) in parallel
```

Or in separate terminals:

```bash
make backend    # Spring Boot with hot-reload via spring-boot:run
make frontend   # Vite dev server with HMR
```

Open **http://localhost:5173** — the Vite dev server proxies all `/api` requests to the backend.

### Step 3 — (Optional) Create a `.env` file

Copy `.env.example` to `.env` at the repo root and adjust as needed. Spring Boot loads it automatically:

```bash
cp .env.example .env
```

---

## Choosing your LLM provider

Briefen ships with three provider options. You can switch between them at any time from **Settings → Model**.

### Ollama (default — fully local, no API key)

Included in the Docker Compose stack. The default model is `gemma3:4b`.

To use a different model, set `OLLAMA_MODEL` in the compose environment and ensure Ollama has it pulled:

```yaml
OLLAMA_MODEL: gemma2:2b
```

Pull it manually if needed:
```bash
docker exec briefen-ollama ollama pull gemma2:2b
```

### OpenAI

Set `BRIEFEN_OPENAI_API_KEY` in the compose environment before first startup:

```yaml
BRIEFEN_OPENAI_API_KEY: sk-...
```

Briefen seeds the key into admin settings on first boot. Available models appear automatically in the model picker. You can also enter the key later via **Settings → Integrations** in the browser.

### Anthropic Claude

Set `BRIEFEN_ANTHROPIC_API_KEY` in the compose environment before first startup:

```yaml
BRIEFEN_ANTHROPIC_API_KEY: sk-ant-...
```

Same seeding behaviour as OpenAI.

---

## Deploying behind a reverse proxy

Always put Briefen behind a TLS-terminating proxy before exposing it to the internet.

### Required: raise proxy timeouts

Summarization can take 2–3 minutes for long articles on local models. The default 60s proxy timeout will kill the connection mid-request. Set read and send timeouts to **310s** (10 seconds more than Briefen's own Ollama timeout):

**Nginx:**
```nginx
location / {
    proxy_pass            http://localhost:8080;
    proxy_read_timeout    310s;
    proxy_send_timeout    310s;
    proxy_connect_timeout 10s;
}
```

**Caddy** (no extra config needed — Caddy has no default timeout):
```caddy
briefen.example.com {
    reverse_proxy localhost:8080
}
```

**Traefik** (static configuration — `traefik.yml`):
```yaml
serversTransport:
  respondingTimeouts:
    readTimeout: 310s
    writeTimeout: 310s
```

### Required: enable forwarded-header trust

Set `SERVER_FORWARD_HEADERS_STRATEGY=FRAMEWORK` so Spring Boot reads `X-Forwarded-Proto` from the proxy. Without this, redirect URLs after login use `http://` even on HTTPS deployments:

```yaml
SERVER_FORWARD_HEADERS_STRATEGY: FRAMEWORK
```

---

## Backing up your data

### SQLite (default)

Your entire Briefen state — summaries, notes, API keys, settings — lives in a single SQLite file.

#### Docker volume backup

```bash
# Create a compressed archive on the host
docker run --rm \
  -v briefen_briefen_data:/data \
  -v "$(pwd)":/backup \
  alpine tar czf /backup/briefen-backup-$(date +%Y%m%d).tar.gz -C /data .
```

#### Docker volume restore

```bash
# Stop the app first to avoid write conflicts
docker compose -f docker-compose.sample.yml down

docker run --rm \
  -v briefen_briefen_data:/data \
  -v "$(pwd)":/backup \
  alpine sh -c "rm -rf /data/* && tar xzf /backup/briefen-backup-YYYYMMDD.tar.gz -C /data"

docker compose -f docker-compose.sample.yml up -d
```

#### Bare-metal backup

```bash
cp ./data/briefen.db ./briefen-backup-$(date +%Y%m%d).db
```

> **Tip:** For continuous replication to S3-compatible storage, add a [Litestream](https://litestream.io) sidecar to your compose file. Litestream streams SQLite's WAL to object storage in real time with no application changes required.

### PostgreSQL

If you're using PostgreSQL (`BRIEFEN_DB_TYPE=postgres`), use standard PostgreSQL backup tools:

```bash
# Backup
docker exec briefen-postgres pg_dump -U briefen briefen > briefen-backup-$(date +%Y%m%d).sql

# Restore
docker exec -i briefen-postgres psql -U briefen briefen < briefen-backup-YYYYMMDD.sql
```

For continuous backups, consider [pgBackRest](https://pgbackrest.org/) or [WAL-G](https://github.com/wal-g/wal-g).

---

## Installing the browser extensions

Briefen provides extensions for Firefox and Chrome/Chromium. Each adds a toolbar button that sends the active tab to your instance with one click.

### Firefox

1. Open Firefox → `about:debugging#/runtime/this-firefox`
2. Click **Load Temporary Add-on…**
3. Select `extension/manifest.json` from the cloned repo

### Chrome / Chromium

Works with Chrome, Edge, Brave, Vivaldi, Arc, and any Chromium-based browser.

1. Open `chrome://extensions/` (or the equivalent in your browser)
2. Enable **Developer mode** (top-right toggle)
3. Click **Load unpacked** and select the `extension-chrome/` directory from the cloned repo

### Configuration

After installing either extension, open its options page and set:

| Field | Value |
|---|---|
| **Briefen URL** | e.g. `http://localhost:8080` or `https://briefen.example.com` |
| **Username** | Your Briefen username (created during first-run setup) |
| **Password** | Your Briefen password |

If your Briefen instance is on a different origin than the extension (any remote deployment), add the following to the compose environment so CORS allows extension requests:

```yaml
# Firefox only
BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*

# Chrome only
BRIEFEN_CORS_ALLOWED_ORIGINS: chrome-extension://*

# Both
BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*,chrome-extension://*
```

---

## Troubleshooting

### The app shows a blank page or 404 after login

If you are running behind a reverse proxy that strips a path prefix, ensure `SERVER_CONTEXT_PATH` is **not** set (let the proxy strip the prefix before passing requests to the app). If the proxy does **not** strip the prefix, set `SERVER_CONTEXT_PATH` and rebuild the image with the matching `APP_BASE_PATH` build arg.

### Summaries return a timeout error

Your reverse proxy is cutting the connection before Ollama finishes. Set `proxy_read_timeout 310s` (or equivalent) — see [Deploying behind a reverse proxy](#deploying-behind-a-reverse-proxy) above.

### Ollama models are not available / "no models found"

```bash
# Check Ollama is running and reachable from the app container
docker exec briefen-app curl -sf http://briefen-ollama:11434/api/tags

# Pull a model manually if the auto-pull failed
docker exec briefen-ollama ollama pull gemma3:4b
```

### I forgot my password

The credentials are stored as a BCrypt hash in the SQLite database. The simplest recovery is to delete the database and restart (all summaries are lost), or to modify the hash directly via the SQLite CLI:

```bash
# Generate a new BCrypt hash for "newpassword"
docker exec briefen-app java -cp app.jar \
  org.springframework.security.crypto.bcrypt.BCrypt

# Or use any BCrypt tool, then update the DB directly
docker exec -it briefen-app sqlite3 /data/briefen.db \
  "UPDATE users SET password_hash='\$2a\$10\$...' WHERE username='admin';"
```

### The container health check keeps failing

```bash
# Check what the health endpoint actually returns
docker exec briefen-app curl -sv http://localhost:8080/actuator/health

# Check the full application log for startup errors
docker compose -f docker-compose.sample.yml logs app
```

### ARM / Raspberry Pi performance

The pre-built image supports `linux/arm64`. Ollama runs in CPU-only mode inside Docker on ARM — this is slower than native, but `gemma2:2b` is usable on a Raspberry Pi 4 or 5. For better performance, run Ollama natively on the host and point Briefen at `http://host.docker.internal:11434` (Mac/Windows) or the host IP (Linux).
