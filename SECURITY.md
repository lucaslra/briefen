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

Briefen is designed as a **local-first, single-user application**. By default:

- All data stays on your machine (SQLite, Ollama)
- No external API calls unless you explicitly configure OpenAI or Readeck
- Readeck API keys are stored server-side and never sent to the browser
- OpenAI and Anthropic API keys are stored server-side with masked display in the UI

If you expose Briefen to the internet, you are responsible for adding authentication and TLS — the application does not include these by default.

### Deployment Threat Model

Briefen's security posture assumes:

- The server runs on `localhost` or a trusted private network
- A single trusted user operates all endpoints
- There is **no authentication** — all API endpoints are open by design

**Consequences for network-exposed deployments:** Any client that can reach the server can read/modify all data, configure API keys, and trigger LLM inference. Place an authentication layer (e.g. HTTP Basic Auth via Nginx/Caddy) in front of Briefen before exposing it to untrusted networks.

### Sensitive Data Stored

| Data | Storage | Notes |
|------|---------|-------|
| OpenAI API key | SQLite, plaintext | Acceptable for local single-user use |
| Anthropic API key | SQLite, plaintext | Same |
| Readeck API key | SQLite, plaintext | Never forwarded to the browser |
| Readeck instance URL | SQLite | User-controlled; may point to private network services |
| Article content & summaries | SQLite | May include private or paywalled material |

Ensure `./data/briefen.db` has appropriate filesystem permissions, especially in shared-host environments.

### Known Accepted Risks

The following are intentional design trade-offs, not vulnerabilities:

| Risk | Rationale |
|------|-----------|
| No authentication on any endpoint | Single-user local app |
| API keys stored in plaintext | Local SQLite file; filesystem protection is sufficient |
| Plaintext HTTP to Ollama | Same Docker network / localhost only |
| Readeck proxy does not block private IPs | Readeck is self-hosted; blocking private IPs would break the integration |
