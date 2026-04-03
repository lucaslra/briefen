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

Briefen is designed as a **local-first application**. By default:

- All data stays on your machine (SQLite, Ollama)
- No external API calls unless you explicitly configure OpenAI or Readeck
- Readeck API keys are stored server-side and never sent to the browser
- OpenAI API keys are stored server-side with masked display in the UI

If you expose Briefen to the internet, you are responsible for adding authentication and TLS — the application does not include these by default.
