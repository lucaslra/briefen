# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Briefen** — a local-first article summarizer. Paste a URL → Jsoup fetches the article → Ollama (local LLM) summarizes it → result is cached in MongoDB. No cloud dependencies by default; everything runs locally via Docker.

## Commands

### Start everything
```bash
make up      # Start Docker services (MongoDB + Ollama)
make dev     # Start Docker services + backend + frontend in parallel
```

### Run services individually
```bash
make backend   # cd backend && ./mvnw spring-boot:run
make frontend  # cd frontend && pnpm dev
```

### Other
```bash
make logs      # Tail Docker service logs
make down      # Stop Docker services
make clean     # Stop containers, remove mongo data + build artifacts (preserves Ollama weights)
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
Browser (React/Vite :5173)
  → /api/* proxied to Spring Boot (:8080)
    → Jsoup (fetch article HTML)
    → Ollama (:11434, local LLM)
    → MongoDB (:27017, persist summary)
```

### Backend (`backend/src/main/java/com/briefen/`)
- **controller/** — REST endpoints; `SummaryController` handles all `/api/` routes, `GlobalExceptionHandler` maps exceptions to HTTP responses
- **service/** — core logic split into fetcher (Jsoup), summarizer (Ollama/OpenAI RestClient), and an orchestration service
- **model/** — MongoDB documents: `Summary` (url, title, summary, modelUsed, createdAt, isRead, savedAt) and `UserSettings`
- **repository/** — Spring Data MongoDB repos; auto-index creation is enabled
- **dto/** — request/response records
- **config/** — `OllamaProperties`, `OpenAIProperties`, RestClient beans

Key `application.yml` settings:
```yaml
spring.data.mongodb.uri: mongodb://localhost:27017/briefen
server.port: 8080
ollama.base-url: http://localhost:11434
ollama.model: gemma3:4b
ollama.timeout: 300s
```

### Frontend (`frontend/src/`)
- **App.jsx** — React Router: `/` (home/summarize), `/reading-list`, `/settings`
- **hooks/** — custom hooks encapsulate all API calls and state (`useSummarize`, `useSettings`, `useReadingList`, etc.)
- **components/** — UI components; no component library, plain CSS + CSS modules
- **constants/strings.js** — all user-facing strings centralized here (i18n-ready)
- **utils/** — shared utilities

All `/api` requests go through Vite's dev proxy to the Spring Boot backend. Fetch calls use `AbortController` for cancellation support.

### Infrastructure
- **MongoDB 7** — `briefen` database, collections auto-created by Spring Data on first write
- **Ollama** — local LLM; Docker Compose pulls `gemma2:2b`, `gemma3:4b`, `llama3.2:3b` on first start
- Optional: OpenAI API key can be configured in browser settings for cloud-based summarization

## Key Conventions

- **No TypeScript** — frontend is plain JavaScript; backend is Java 25
- **CSS approach** — plain CSS + CSS custom properties for dark/light theming; CSS modules for component scoping
- **All UI strings** go in `frontend/src/constants/strings.js`, never hardcoded in components
- **No frontend tests** configured; backend uses JUnit 5 via Spring Boot Test
- **pnpm** is the frontend package manager (not npm/yarn)
