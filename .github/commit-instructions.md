# Commit Message Instructions

Follow the **Conventional Commits** specification. Every commit message must have a structured subject line; a body is optional but encouraged for non-trivial changes.

---

## Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

---

## Type

Use one of the following types. The type determines the Release Drafter category and version bump.

| Type | When to use | Version bump |
|---|---|---|
| `feat` | New user-facing feature | minor |
| `fix` | Bug fix | patch |
| `refactor` | Code restructure with no behaviour change | patch |
| `perf` | Performance improvement | patch |
| `test` | Add or fix tests | patch |
| `docs` | Documentation only | patch |
| `chore` | Maintenance, build tooling, config | patch |
| `ci` | CI/CD workflow changes | patch |
| `build` | Build system, dependencies | patch |

Use `!` after the type for **breaking changes**: `feat!: rename API endpoint`. Breaking changes trigger a major version bump.

---

## Scope (optional)

Narrow the area of change. Use lowercase, one word.

```
feat(backend): add rate limiting to summarize endpoint
fix(frontend): prevent duplicate summarize requests
chore(deps): bump jsoup to 1.18.4
ci(docker): cache Maven layers in build workflow
```

Common scopes for this project: `backend`, `frontend`, `docker`, `deps`, `ci`, `db`, `auth`, `webhook`.

---

## Subject

- Imperative mood, present tense: **"add"** not "added" or "adds"
- Lowercase first letter
- No period at the end
- 72 characters or fewer
- Describe *what* the change does, not *how*

**Good:** `feat(auth): add optional HTTP Basic Auth via env vars`
**Bad:** `Added authentication to the application.`

---

## Body (optional, recommended for non-trivial changes)

- Separate from the subject with a blank line
- Wrap at 72 characters
- Explain *why* the change is needed, not *what* files were edited
- List multiple related changes as bullet points

```
feat(webhook): make webhook URL configurable via Settings UI

Previously the webhook URL could only be set via BRIEFEN_WEBHOOK_URL.
Self-hosters who manage Briefen through the browser UI had no way to
change the URL without restarting the container.

- Add webhookUrl field to UserSettings domain model and SQLite entity
- Flyway V2 migration adds the webhook_url column
- SettingsController validates the URL before saving
- WebhookService resolves URL from settings first, env var as fallback
```

---

## Footer (optional)

Use for issue references, breaking change descriptions, and co-authors.

```
Closes #42
BREAKING CHANGE: /api/summarize now requires Content-Type: application/json
Co-Authored-By: Name <email>
```

---

## Rules enforced by the Release Drafter

The CI pipeline reads the **subject prefix** to auto-label PRs and categorise release notes:

| Subject starts with | Label applied | Release category |
|---|---|---|
| `feat` | `feature` | 🚀 New Features |
| `fix` | `bug` | 🐛 Bug Fixes |
| `docs` | `docs` | 📖 Documentation |
| `chore`, `ci`, `build`, `test` | `infra` | 🏗️ Infrastructure |
| `deps`, `bump`, `upgrade` | `dependencies` | 📦 Dependencies |

PRs that touch only `backend/**` files are also labelled `backend`; `frontend/**` files get `frontend`.

Add the `skip-changelog` label to a PR to exclude it from release notes entirely.
