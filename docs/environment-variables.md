# Environment Variable Reference

All runtime behaviour is controlled through environment variables. This document is the single source of truth for every variable Briefen reads, its accepted values, and its default.

---

## How to set variables

**Docker Compose (recommended):** add them to the `environment:` block in your compose file.

```yaml
environment:
  SERVER_PORT: 9000
  OLLAMA_MODEL: gemma2:2b
```

**`.env` file (local development):** copy `.env.example` to `.env` at the repo root. Spring Boot loads it automatically on startup via `spring.config.import`.

```bash
cp .env.example .env
# edit .env
make dev
```

**Shell environment:** export variables before running the JAR directly.

```bash
export OLLAMA_MODEL=gemma2:2b
java -jar app.jar
```

---

## Quick reference

| Variable | Default | Required |
|---|---|---|
| [`BRIEFEN_DB_PATH`](#briefen_db_path) | `./data/briefen.db` | No |
| [`SERVER_PORT`](#server_port) | `8080` | No |
| [`SERVER_BIND_ADDRESS`](#server_bind_address) | `0.0.0.0` | No |
| [`SERVER_CONTEXT_PATH`](#server_context_path) | `/` | No |
| [`SERVER_FORWARD_HEADERS_STRATEGY`](#server_forward_headers_strategy) | `NONE` | No |
| [`BRIEFEN_CORS_ALLOWED_ORIGINS`](#briefen_cors_allowed_origins) | *(empty — disabled)* | No |
| [`OLLAMA_BASE_URL`](#ollama_base_url) | `http://localhost:11434` | No |
| [`OLLAMA_MODEL`](#ollama_model) | `gemma3:4b` | No |
| [`BRIEFEN_OPENAI_API_KEY`](#briefen_openai_api_key) | *(empty — disabled)* | No |
| [`BRIEFEN_ANTHROPIC_API_KEY`](#briefen_anthropic_api_key) | *(empty — disabled)* | No |
| [`BRIEFEN_WEBHOOK_URL`](#briefen_webhook_url) | *(empty — disabled)* | No |
| [`BRIEFEN_LOG_LEVEL`](#briefen_log_level) | `INFO` | No |
| [`BRIEFEN_LOG_FORMAT`](#briefen_log_format) | *(unset — human-readable)* | No |
| [`BRIEFEN_DEFAULT_PROMPT`](#briefen_default_prompt) | *(unset — built-in prompt)* | No |

**Build-time only:**

| Variable | Default |
|---|---|
| [`APP_BASE_PATH`](#app_base_path) | `/` |

---

## Database

### `BRIEFEN_DB_PATH`

Path to the SQLite database file. The file and its parent directory are created automatically on first startup if they do not exist.

| | |
|---|---|
| **Type** | File path (string) |
| **Default** | `./data/briefen.db` |
| **Docker recommendation** | Point to a path inside a named volume, e.g. `/data/briefen.db` |

```yaml
BRIEFEN_DB_PATH: /data/briefen.db
```

> The directory must be writable by the process. In the Docker image the default data directory is `/data`, owned by the `briefen` user.

---

## Server

### `SERVER_PORT`

The TCP port Briefen listens on.

| | |
|---|---|
| **Type** | Integer (1–65535) |
| **Default** | `8080` |

```yaml
SERVER_PORT: 9000
```

---

### `SERVER_BIND_ADDRESS`

The network interface Briefen binds to. Set to `127.0.0.1` when a reverse proxy runs on the same host to prevent direct external access to the application port.

| | |
|---|---|
| **Type** | IP address string |
| **Default** | `0.0.0.0` (all interfaces) |

```yaml
# Restrict to localhost — only the proxy can reach Briefen
SERVER_BIND_ADDRESS: 127.0.0.1

# All interfaces (default)
SERVER_BIND_ADDRESS: 0.0.0.0
```

> When using `127.0.0.1`, the `ports:` mapping in Docker Compose should also be updated to `127.0.0.1:8080:8080` (or removed entirely if the proxy is in the same Docker network).

---

### `SERVER_CONTEXT_PATH`

Serves the entire application (API + frontend) under a URL sub-path. Use this when Briefen shares a domain with other services and a reverse proxy does **not** strip the prefix before forwarding.

| | |
|---|---|
| **Type** | URL path string |
| **Default** | `/` (root — no prefix) |
| **Format** | Must start with `/`. Use a trailing slash for sub-paths: `/briefen/` not `/briefen`. |

```yaml
SERVER_CONTEXT_PATH: /briefen/
```

> **Build-time constraint.** The pre-built GHCR image has `/` baked into its frontend asset paths. If you set `SERVER_CONTEXT_PATH` to anything other than `/`, you must also build the image locally with the matching `APP_BASE_PATH` build argument:
>
> ```bash
> docker build --build-arg APP_BASE_PATH=/briefen/ -t briefen:local .
> ```
>
> If the proxy strips the path prefix before passing requests to Briefen (e.g. `proxy_pass http://localhost:8080/` in Nginx), leave `SERVER_CONTEXT_PATH` unset — the app sees all requests at `/` regardless.

---

### `SERVER_FORWARD_HEADERS_STRATEGY`

Controls how Spring Boot processes `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Forwarded-Host` headers sent by a reverse proxy. Must be set correctly when Briefen is behind a proxy, otherwise redirects and HTTPS detection break.

| | |
|---|---|
| **Type** | Enum |
| **Default** | `NONE` |

| Value | When to use |
|---|---|
| `NONE` | Direct access — no reverse proxy in front of Briefen. Default. |
| `FRAMEWORK` | Behind a trusted reverse proxy (Nginx, Traefik, Caddy). Spring Boot reads and applies the forwarded headers. **Use this for all proxied deployments.** |
| `NATIVE` | Delegates header processing to the embedded Tomcat server rather than Spring. Rarely needed; use `FRAMEWORK` in most cases. |

```yaml
SERVER_FORWARD_HEADERS_STRATEGY: FRAMEWORK
```

> Always pair this with `FRAMEWORK` when you expose Briefen via HTTPS through a proxy. Without it, Spring Boot generates `http://` redirect URLs even on an HTTPS deployment.

---

## Networking & CORS

### `BRIEFEN_CORS_ALLOWED_ORIGINS`

Comma-separated list of origins that are permitted to make cross-origin requests to the API. CORS is disabled entirely when this variable is empty (the default).

| | |
|---|---|
| **Type** | Comma-separated string |
| **Default** | *(empty — CORS disabled)* |

```yaml
# Firefox extension only
BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*

# Chrome extension only
BRIEFEN_CORS_ALLOWED_ORIGINS: chrome-extension://*

# Both extensions
BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*,chrome-extension://*

# Extensions + local dev frontend
BRIEFEN_CORS_ALLOWED_ORIGINS: moz-extension://*,chrome-extension://*,http://localhost:5173

# Specific remote origin
BRIEFEN_CORS_ALLOWED_ORIGINS: https://briefen.example.com
```

**When do you need this?**

| Scenario | Needed? |
|---|---|
| Browser accessing Briefen on the same origin | No |
| Firefox extension connecting to a remote Briefen instance | **Yes** — add `moz-extension://*` |
| Chrome extension connecting to a remote Briefen instance | **Yes** — add `chrome-extension://*` |
| Vite dev server (port 5173) calling the backend (port 8080) | **Yes** — add `http://localhost:5173` |
| Vite dev server accessing its own Vite proxy | No — the proxy rewrites the origin |

---

## Ollama (local LLM)

### `OLLAMA_BASE_URL`

The base URL of the Ollama API server.

| | |
|---|---|
| **Type** | HTTP/HTTPS URL |
| **Default** | `http://localhost:11434` |

```yaml
# Ollama as a sibling Docker Compose service (use the service name)
OLLAMA_BASE_URL: http://ollama:11434

# Ollama running on the Docker host (Mac/Windows)
OLLAMA_BASE_URL: http://host.docker.internal:11434

# Ollama running on the Docker host (Linux — replace with actual IP)
OLLAMA_BASE_URL: http://172.17.0.1:11434

# Remote Ollama server
OLLAMA_BASE_URL: http://192.168.1.100:11434
```

---

### `OLLAMA_MODEL`

The default Ollama model used for summarization when no model is selected in the UI.

| | |
|---|---|
| **Type** | String (Ollama model tag) |
| **Default** | `gemma3:4b` |

The model must already be pulled in Ollama before it can be used. The default Docker Compose setup pulls `gemma3:4b`, `gemma2:2b`, and `llama3.2:3b` automatically on first start.

```yaml
OLLAMA_MODEL: gemma2:2b
```

**Recommended models by use case:**

| Model | Size | Best for |
|---|---|---|
| `gemma2:2b` | ~1.6 GB | Low-RAM devices (Raspberry Pi, small VPS) |
| `gemma3:4b` | ~3.3 GB | **Default** — best quality-to-size balance, 128K context |
| `llama3.2:3b` | ~2 GB | Alternative mid-tier option |
| `mistral` | ~4.1 GB | Higher quality on complex articles |
| `llama3` | ~4.7 GB | Strong general-purpose summarization |

> Users can override the model per-session from the model picker in the browser UI without any server restart.

---

## Cloud LLM providers

### `BRIEFEN_OPENAI_API_KEY`

OpenAI API key. When set, Briefen seeds it into the admin settings on first startup so OpenAI models appear immediately in the model picker without any UI configuration.

| | |
|---|---|
| **Type** | String (OpenAI API key — starts with `sk-`) |
| **Default** | *(empty — OpenAI disabled)* |
| **Seeding behaviour** | Written to admin settings on first startup only. Does not overwrite a key already saved in the database. Can also be set or changed later via **Settings → Integrations** in the browser. |

```yaml
BRIEFEN_OPENAI_API_KEY: sk-...
```

**Available OpenAI models (once key is set):**

| Model | Notes |
|---|---|
| `gpt-4o-mini` | Fast, cost-effective — recommended for most use cases |
| `gpt-4o` | Higher quality |
| `gpt-4.1-nano` | Fastest, lowest cost |
| `gpt-4.1-mini` | Balanced |
| `o4-mini` | Reasoning model |

> No data is sent to OpenAI unless the user explicitly selects an OpenAI model.

---

### `BRIEFEN_ANTHROPIC_API_KEY`

Anthropic API key. Same first-startup seeding behaviour as `BRIEFEN_OPENAI_API_KEY`.

| | |
|---|---|
| **Type** | String (Anthropic API key — starts with `sk-ant-`) |
| **Default** | *(empty — Anthropic disabled)* |
| **Seeding behaviour** | Written to admin settings on first startup only. Does not overwrite a key already saved in the database. Can also be set or changed later via **Settings → Integrations**. |

```yaml
BRIEFEN_ANTHROPIC_API_KEY: sk-ant-...
```

**Available Anthropic models (once key is set):**

| Model | Notes |
|---|---|
| `claude-haiku-4-5` | Fastest, lowest cost — recommended for most use cases |
| `claude-sonnet-4-5` | Balanced quality and speed |
| `claude-opus-4-5` | Highest quality |

> No data is sent to Anthropic unless the user explicitly selects a Claude model.

---

## Webhooks

### `BRIEFEN_WEBHOOK_URL`

HTTP(S) URL that receives a `POST` request whenever a summary is saved. Delivery is fire-and-forget on a virtual thread — failures are logged at `WARN` but never surface to the user or affect summarization.

| | |
|---|---|
| **Type** | HTTP/HTTPS URL |
| **Default** | *(empty — webhooks disabled)* |
| **Priority** | The URL set via **Settings → Integrations** in the browser takes precedence over this variable. |
| **Timeout** | 10s connect + 10s read |

```yaml
# ntfy (self-hosted push notifications)
BRIEFEN_WEBHOOK_URL: https://ntfy.example.com/briefen

# Home Assistant
BRIEFEN_WEBHOOK_URL: https://homeassistant.local:8123/api/webhook/briefen-done

# Any HTTP endpoint
BRIEFEN_WEBHOOK_URL: https://n8n.example.com/webhook/abc123
```

**Payload:**

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

---

## Logging

### `BRIEFEN_LOG_LEVEL`

Controls the log verbosity of Briefen's own application code. Increase to `DEBUG` when troubleshooting summarization failures, Ollama connectivity, or unexpected API behaviour. Does not affect Spring Boot framework or third-party library log output.

| | |
|---|---|
| **Type** | Enum |
| **Default** | `INFO` |

| Value | When to use |
|---|---|
| `ERROR` | Only critical failures — very quiet |
| `WARN` | Warnings and errors only |
| `INFO` | Normal operation — **default** |
| `DEBUG` | Request-level detail; shows article fetching, LLM requests, webhook delivery |
| `TRACE` | Full trace — very verbose, avoid in production |

```yaml
BRIEFEN_LOG_LEVEL: DEBUG
```

> To increase verbosity across all code (including Spring Boot internals), use the Spring Boot native variable `LOGGING_LEVEL_ROOT=DEBUG`. See [Advanced / undocumented](#advanced--undocumented).

---

### `BRIEFEN_LOG_FORMAT`

Controls the log output format. Set to `json` for structured JSON logs (one JSON object per line), useful for log aggregation tools like Loki, Grafana, Datadog, or simple `jq` filtering. When unset or any other value, Briefen uses Spring Boot's default human-readable console format.

| | |
|---|---|
| **Type** | String |
| **Default** | *(unset — human-readable)* |

| Value | Output format |
|---|---|
| *(unset)* | Human-readable with timestamps, colours, and thread names — **default** |
| `json` | Structured JSON via [logstash-logback-encoder](https://github.com/logfellow/logstash-logback-encoder). One JSON object per line with `timestamp`, `level`, `logger_name`, `message`, and `stack_trace` fields. |

```yaml
BRIEFEN_LOG_FORMAT: json
```

**Example JSON output:**

```json
{"timestamp":"2026-04-11T10:00:00.000+00:00","level":"INFO","logger_name":"com.briefen.service.SummaryService","message":"Returning cached summary for https://example.com/article","thread_name":"http-nio-8080-exec-1"}
```

> Pairs well with Docker's `json-file` log driver and `jq` for ad-hoc filtering: `docker logs briefen-app | jq 'select(.level == "ERROR")'`

---

## Summarization

### `BRIEFEN_DEFAULT_PROMPT`

A custom system prompt that replaces the built-in summarization instructions for all users who have not configured a personal custom prompt in **Settings → Summarization**. Useful for setting a deployment-wide output language, tone, or format.

| | |
|---|---|
| **Type** | String (free text) |
| **Default** | *(unset — built-in English summarization prompt)* |
| **Priority** | User's personal custom prompt (set via Settings UI) > this variable > built-in prompt |

```yaml
BRIEFEN_DEFAULT_PROMPT: "You are an article summarizer. Write summaries in Spanish. Use 3-5 concise paragraphs. Start with a markdown H1 title."
```

> The built-in prompt instructs the LLM to produce an English summary with a markdown H1 title, 3–6 paragraphs, and a Key Quotes section. Override this when you need a different language, format, or style across your entire instance.

---

## Build-time variables

These are Docker build arguments (`ARG`), not runtime environment variables. They are consumed during `docker build` and baked into the image — they cannot be changed at runtime.

### `APP_BASE_PATH`

The URL base path baked into the compiled frontend assets. Must match `SERVER_CONTEXT_PATH` exactly.

| | |
|---|---|
| **Type** | URL path string |
| **Default** | `/` |
| **Set during** | `docker build --build-arg APP_BASE_PATH=<value>` |
| **Affects** | Vite's `base` option — asset `src` and `href` paths in the compiled HTML |

```bash
# Root deployment (default — pre-built GHCR image always uses this)
docker build -t briefen:local .

# Sub-path deployment
docker build --build-arg APP_BASE_PATH=/briefen/ -t briefen:local .
```

> The pre-built image on GHCR always uses `APP_BASE_PATH=/`. If you need a custom sub-path, build the image locally.

---

## Advanced / undocumented

These variables are not exposed in `.env.example` but are accessible through Spring Boot's [relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables) — any Spring property can be set via an environment variable by uppercasing it and replacing `.` and `-` with `_`.

### `ARTICLE_FETCH_TIMEOUT`

Maximum time Briefen waits for an article page to respond before giving up. Increase if you regularly summarize sites with slow servers.

| | |
|---|---|
| **Type** | Duration string (`10s`, `30s`, `1m`) |
| **Default** | `10s` |
| **Spring property** | `article.fetch-timeout` |

```yaml
ARTICLE_FETCH_TIMEOUT: 30s
```

### `LOGGING_LEVEL_COM_BRIEFEN`

Spring Boot's native relaxed-binding equivalent of [`BRIEFEN_LOG_LEVEL`](#briefen_log_level). Both variables set the same underlying property (`logging.level.com.briefen`). Prefer `BRIEFEN_LOG_LEVEL` — it follows the project's naming convention.

```yaml
# Prefer this:
BRIEFEN_LOG_LEVEL: DEBUG

# Equivalent (Spring Boot relaxed binding):
LOGGING_LEVEL_COM_BRIEFEN: DEBUG
```

### `LOGGING_LEVEL_ROOT`

Controls the log verbosity of all code including Spring Boot and third-party libraries. Only raise this temporarily for deep debugging.

| | |
|---|---|
| **Type** | Enum (same values as above) |
| **Default** | `INFO` |
| **Spring property** | `logging.level.root` |

```yaml
LOGGING_LEVEL_ROOT: DEBUG
```

---

## Variable interactions

Some variables interact with each other in non-obvious ways:

| If you set… | You must also… |
|---|---|
| `SERVER_CONTEXT_PATH=/briefen/` | Build the image locally with `--build-arg APP_BASE_PATH=/briefen/` |
| `SERVER_BIND_ADDRESS=127.0.0.1` | Ensure the reverse proxy connects to `127.0.0.1:8080`, not via a Docker network alias |
| `SERVER_FORWARD_HEADERS_STRATEGY=FRAMEWORK` | Ensure your reverse proxy sends `X-Forwarded-Proto` and `X-Forwarded-Host` headers |
| `BRIEFEN_CORS_ALLOWED_ORIGINS` including `moz-extension://*` | Use the Firefox extension with a remote Briefen instance |
| `OLLAMA_MODEL` (non-default model) | Ensure that model is pulled in Ollama — it is not auto-pulled for custom values |
