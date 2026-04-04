# Briefen Firefox Extension

Send the current browser tab to your self-hosted [Briefen](https://github.com/your-org/briefen) instance for summarization — one click, fire-and-forget.

## How it works

1. You click the Briefen toolbar button on any article page.
2. The extension POSTs the tab URL to your Briefen backend (`POST /api/articles`).
3. Briefen acknowledges receipt (202 Accepted) immediately.
4. Summarization runs in the background on the server — no waiting, no polling.
5. The article appears in your Briefen reading list once processing is done.

## Installation (temporary add-on)

Firefox does not require extensions to be signed when loaded via `about:debugging`.

1. Open Firefox and navigate to `about:debugging#/runtime/this-firefox`.
2. Click **Load Temporary Add-on…**.
3. Navigate to this `extension/` directory and select `manifest.json`.
4. The Briefen icon appears in your toolbar.

> **Note:** Temporary add-ons are removed when Firefox restarts. For a persistent installation, the extension would need to be submitted to addons.mozilla.org or signed with a Developer Edition / Nightly build.

## Configuration

Before using the extension, tell it where your Briefen instance is running:

1. Right-click the Briefen toolbar icon → **Manage Extension** → **Preferences**, or click **Settings** at the bottom of the popup.
2. Enter your Briefen base URL (e.g. `http://localhost:8080` or `https://briefen.example.com`).
3. Click **Test connection** to verify the backend is reachable.
4. Click **Save**.

The default URL is `http://localhost:8080` if nothing is configured.

## Backend requirements

### CORS

The extension runs from a `moz-extension://` origin. The Briefen backend must allow this origin explicitly.

Set the `BRIEFEN_CORS_ALLOWED_ORIGINS` environment variable on your Briefen server:

```bash
# Local development
BRIEFEN_CORS_ALLOWED_ORIGINS=moz-extension://*,http://localhost:5173

# Production (replace with your actual extension origin if you want to lock it down)
BRIEFEN_CORS_ALLOWED_ORIGINS=moz-extension://*
```

Or in your `.env` file:
```
BRIEFEN_CORS_ALLOWED_ORIGINS=moz-extension://*
```

`moz-extension://*` allows any Firefox extension to call your Briefen instance. Since Briefen has no authentication, only run this on a trusted local network or behind a VPN.

### API endpoints used

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/articles` | Submit a URL for summarization |
| `GET` | `/api/health` | Connection test from the options page |

#### POST /api/articles

```http
POST /api/articles
Content-Type: application/json

{ "url": "https://example.com/some-article" }
```

```json
// 202 Accepted
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "QUEUED",
  "message": "Article received and queued for summarization."
}
```

#### GET /api/health

```http
GET /api/health
```

```json
// 200 OK
{ "status": "UP" }
```

## Error handling

| Scenario | Popup message |
|----------|---------------|
| Backend unreachable | "Could not reach Briefen. Check the URL in Settings." |
| Request times out (10 s) | "Request timed out. Is Briefen running?" |
| Server returns 4xx/5xx | The error message from the server response |
| Unsupported page (about:, file:, …) | "This page can't be summarized." |

## File structure

```
extension/
├── manifest.json       # Extension manifest (Manifest V2)
├── popup.html          # Toolbar popup UI
├── popup.js            # Popup logic
├── popup.css           # Popup styles
├── options.html        # Settings page
├── options.js          # Settings logic
├── options.css         # Settings styles
└── icons/
    ├── icon-48.svg     # Toolbar icon
    └── icon-96.svg     # High-DPI toolbar icon
```
