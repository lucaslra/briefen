# Contributing to Briefen

Thanks for your interest in contributing! This guide will help you get set up and understand how the project works.

## Prerequisites

- **Docker** (with Docker Compose)
- **Java 25** (Eclipse Temurin recommended)
- **Node.js 22+** and **pnpm**

## Getting Started

```bash
# 1. Fork and clone the repo
git clone https://github.com/<your-username>/briefen.git
cd briefen

# 2. Start infrastructure
make up

# 3. Start backend and frontend
make dev
```

Frontend runs at http://localhost:5173, backend at http://localhost:8080.

## Project Structure

This is a monorepo with three main areas:

- **`backend/`** — Java 25, Spring Boot 4.0.x, Maven
- **`frontend/`** — React 19, Vite, pnpm, plain JavaScript (no TypeScript)
- **`extension/`** — Firefox browser extension (Manifest V2, vanilla JS)
- **`docs/`** — User-facing documentation (getting started, env vars, reverse proxy)
- **`e2e/`** — Playwright end-to-end tests

See [CLAUDE.md](CLAUDE.md) for detailed architecture documentation.

## Development Workflow

### Making Changes

1. Create a branch from `main` — use a descriptive prefix:
   - `feat/` for new features
   - `fix/` for bug fixes
   - `docs/` for documentation
   - `chore/` or `ci/` for infrastructure
2. Make your changes
3. Test your changes (see below)
4. Open a pull request against `main`

### Running Tests

```bash
# Backend
cd backend
./mvnw test              # Unit tests
./mvnw verify            # Full verification including integration tests

# Frontend
cd frontend
pnpm lint                # ESLint
pnpm build               # Catch build errors

# E2E (requires running app — start with `make docker-up` first)
make e2e
```

### Code Style

- **Backend** — standard Spring Boot conventions; no specific formatter enforced
- **Frontend** — ESLint is configured and runs in CI; follow existing patterns
- **CSS** — plain CSS with CSS custom properties and CSS Modules; no CSS-in-JS or preprocessors
- **Strings** — all user-facing strings go in `frontend/src/constants/strings.js`, never hardcoded in components
- **No TypeScript** — the frontend is intentionally plain JavaScript

### Commit Messages

Use concise, descriptive commit messages. Prefix with the area of change when helpful:

```
feat: add batch export to reading list
fix: handle timeout when Ollama is unreachable
docs: update API endpoint table in README
```

## Pull Request Guidelines

- Keep PRs focused — one feature or fix per PR
- Fill out the PR template
- Ensure CI passes (lint, build, tests)
- Add a brief description of what changed and why
- If adding a new feature, update the relevant documentation (README, `docs/`, and `.env.example` for any new env vars)

## Reporting Issues

Use the [issue templates](https://github.com/lucaslra/briefen/issues/new/choose) when available. Include:

- Steps to reproduce (for bugs)
- Expected vs actual behavior
- Browser/OS/Docker versions if relevant

## Documentation

The `docs/` directory contains user-facing guides:

| File | Purpose |
|---|---|
| `docs/getting-started.md` | Step-by-step deployment guide (Docker, local build, dev) |
| `docs/environment-variables.md` | Complete reference for all env vars and accepted values |
| `docs/reverse-proxy.md` | Nginx, Caddy, and Traefik configuration examples |

When adding or changing a feature that affects deployment or configuration, update the relevant docs file and `.env.example`. If a new environment variable is introduced, add it to all three: `application.yml`, `.env.example`, and `docs/environment-variables.md`.

## What Not to Commit

Before opening a PR, verify you have not accidentally staged any of the following:

- **`.env` files** — gitignored by design; use `.env.example` for documentation
- **API keys, tokens, or passwords** — never hardcode credentials in source files; use environment variables
- **SQLite database files** (`*.db`, `data/`) — gitignored; contain user data and potentially stored API keys
- **IDE or OS files** (`.DS_Store`, `.idea/`) — use your global gitignore for these
- **Large binary files** — check with the maintainer before adding assets

CI includes secret scanning via Gitleaks. PRs that introduce credentials will fail the check.

To catch secrets locally before pushing, run Gitleaks against the full git history:

```bash
# Using Docker (no install required)
docker run --rm -v "$(pwd):/repo" zricethezav/gitleaks:latest detect --source /repo --redact

# Using the Gitleaks binary (https://github.com/gitleaks/gitleaks#installing)
gitleaks detect --redact
```

## Security

If you discover a security vulnerability, **do not open a public issue**. See [SECURITY.md](SECURITY.md) for responsible disclosure instructions.

## License

By contributing, you agree that your contributions will be licensed under the [AGPL-3.0 License](LICENSE).
