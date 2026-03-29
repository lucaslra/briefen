# Briefen

**Article summaries, instantly.** Paste a URL to any online article, and Briefen will fetch, read, and summarize it using a local LLM — no API keys, no cloud dependencies.

## Architecture

```
Browser (React) → Spring Boot API → Jsoup (fetch article) → Ollama (summarize) → MongoDB (persist)
```

| Layer          | Technology                         |
|----------------|------------------------------------|
| Frontend       | React (Vite, pnpm, plain JS)      |
| Backend        | Java 25, Spring Boot 3.4.4, Maven |
| LLM            | Ollama (Docker) with gemma3:4b    |
| Database       | MongoDB 7 (Docker)                |
| Orchestration  | Docker Compose                    |

## Prerequisites

- **Docker** (with Docker Compose)
- **Java 25** (OpenJDK)
- **Node.js 18+** and **pnpm**

## Quick Start

```bash
# 1. Start infrastructure (MongoDB + Ollama)
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

## Changing the Ollama Model

The default model is `gemma3:4b`, chosen for its excellent summarization quality with 128K context window. To change it:

1. Edit `docker-compose.yml` — change the model name in the `ollama` service command
2. Edit `backend/src/main/resources/application.yml` — update `ollama.model`
3. Restart: `make down && make up`

Good alternatives: `gemma2:2b` (faster, lighter), `mistral` (7B, higher quality), `llama3` (8B), `phi3` (3.8B).

## CSS Approach

Plain CSS with CSS custom properties and CSS Modules. Custom properties enable dark/light mode via a single `data-theme` attribute toggle on `<html>`. CSS Modules provide component-scoped styles without build tooling overhead. This keeps the stack minimal and consistent with the plain-JS-no-TypeScript philosophy.

## i18n Readiness

All user-facing strings are centralized in `frontend/src/constants/strings.js`. To add internationalization, replace this module with a library like `react-intl` or `i18next` and swap the constants for translation keys. No string literals appear in component JSX.

## Project Structure

```
summizer/
├── docker-compose.yml
├── Makefile
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/briefen/
│       ├── BriefenApplication.java
│       ├── config/          # Ollama properties, RestClient, health indicator
│       ├── controller/      # REST endpoints, global error handler
│       ├── dto/             # Request/response records
│       ├── exception/       # Custom exceptions
│       ├── model/           # MongoDB document
│       ├── repository/      # Spring Data MongoDB
│       ├── service/         # Article fetcher, Ollama summarizer, orchestrator
│       └── validation/      # URL validator
└── frontend/
    ├── vite.config.js
    └── src/
        ├── App.jsx
        ├── constants/       # Centralized strings
        ├── hooks/           # useTheme, useSummarize, useSummaries, useSettings
        ├── components/      # Header, UrlInput, SummaryDisplay, Settings, etc.
        └── styles/          # CSS variables, global reset
```
