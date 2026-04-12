# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in Briefen, **please do not open a public issue.**

Instead, report it privately using one of these methods:

1. **GitHub Security Advisories** (preferred) — go to the [Security tab](https://github.com/lucaslra/briefen/security/advisories/new) and create a private advisory
2. **Email** — contact the maintainer directly (see GitHub profile)

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if you have one)

### Response Timeline

- **Acknowledgment** — within 48 hours
- **Initial assessment** — within 1 week
- **Fix or mitigation** — depends on severity, but we aim for:
  - Critical/High: patch within 1 week
  - Medium: patch within 2 weeks
  - Low: addressed in the next regular release

## Scope

The following are in scope:

- The Briefen application code (backend and frontend)
- Docker configuration and deployment setup
- Dependencies with known vulnerabilities

The following are **out of scope**:

- Vulnerabilities in Ollama or other third-party services (report to their respective projects)
- Issues that require physical access to the host machine
- Social engineering attacks

## Security Considerations

Briefen is designed as a **local-first, self-hosted application** with multi-user support. By default:

- All data stays on your machine (SQLite or PostgreSQL, Ollama)
- No external API calls unless you explicitly configure OpenAI or Readeck
- Readeck API keys are stored server-side and never sent to the browser
- OpenAI and Anthropic API keys are stored server-side with masked display in the UI

If you expose Briefen to the internet, ensure it is behind a TLS-terminating reverse proxy so that HTTP Basic Auth credentials are not transmitted in the clear.

### Deployment Threat Model

Briefen's security posture assumes:

- The server runs on `localhost` or a trusted private network, or is behind a TLS-terminating reverse proxy when internet-facing
- The admin account creates and manages user accounts; admin-only endpoints are restricted by role
- **HTTP Basic Auth is always active** — the admin account is created through the browser-based first-run setup flow, which enforces a strong password policy (8+ characters, uppercase, lowercase, digit, special character)
- Additional users can be created via the admin panel (`/api/admin/users`)

**Consequences for network-exposed deployments without TLS:** Basic Auth credentials are transmitted as base64 on every request, which is trivially decoded on unencrypted connections. Always terminate TLS at a reverse proxy (Nginx, Traefik, Caddy) before exposing Briefen to the internet.

### Sensitive Data Stored

| Data | Storage | Notes |
|------|---------|-------|
| OpenAI API key | SQLite/PostgreSQL, plaintext | Acceptable for self-hosted use; use Docker secrets or `_FILE` env var suffix for container deployments |
| Anthropic API key | SQLite/PostgreSQL, plaintext | Same |
| Readeck API key | SQLite/PostgreSQL, plaintext | Never forwarded to the browser |
| Readeck instance URL | SQLite/PostgreSQL | User-controlled; may point to private network services |
| User credentials | SQLite/PostgreSQL, hashed | Passwords are never stored or logged in plaintext |
| Article content & summaries | SQLite/PostgreSQL | May include private or paywalled material |

For SQLite deployments, ensure `./data/briefen.db` has appropriate filesystem permissions (`chmod 600`), especially in shared-host environments. For PostgreSQL, use strong database credentials and restrict network access to the database port.

### Known Accepted Risks

The following are intentional design trade-offs, not vulnerabilities:

| Risk | Rationale |
|------|-----------|
| HTTP Basic Auth (not bearer tokens or sessions) | Stateless, simple, and sufficient for self-hosted use; avoids session storage complexity |
| API keys stored in database plaintext | Local file (SQLite) or database server (PostgreSQL); filesystem-level protection (`chmod 600`) or database access controls are sufficient for the target deployment model |
| Plaintext HTTP to Ollama | Same Docker network / localhost only; Ollama has no authentication to protect |
| Readeck proxy does not block private IPs | Readeck is self-hosted; blocking private IPs would break the integration |
