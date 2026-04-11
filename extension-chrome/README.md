# Briefen Chrome Extension

Send the current page to your Briefen instance for summarization with one click.

## Installation (Developer Mode)

1. Open `chrome://extensions/` in Chrome, Edge, or any Chromium-based browser
2. Enable **Developer mode** (top-right toggle)
3. Click **Load unpacked** and select this `extension-chrome/` directory
4. The Briefen icon appears in the toolbar

## Configuration

1. Click the Briefen icon → **Settings** (or right-click the icon → **Options**)
2. Enter your Briefen instance URL (default: `http://localhost:8080`)
3. Enter your username and password
4. Click **Test connection** to verify, then **Save**

## Usage

1. Navigate to any article or web page
2. Click the Briefen toolbar icon
3. Click **Send to Briefen** — the article is queued for background summarization
4. Open your Briefen instance to view the summary in your reading list

## CORS Configuration

When the extension connects to a **remote** Briefen instance (not `localhost`), add the extension's origin to the server's CORS allowlist:

```yaml
BRIEFEN_CORS_ALLOWED_ORIGINS: chrome-extension://*
```

For local development, CORS is not required since `localhost` requests bypass CORS checks.

## API Endpoints Used

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/articles` | Submit a URL for background summarization (202 Accepted) |
| `GET` | `/api/health` | Connection test from the options page |

## Requirements

- Chrome 116+ (or any Chromium-based browser: Edge, Brave, Vivaldi, Arc)
- A running Briefen instance with an admin account

## Differences from the Firefox Extension

| | Firefox (`extension/`) | Chrome (`extension-chrome/`) |
|---|---|---|
| Manifest version | V2 | V3 |
| API namespace | `browser.*` | `chrome.*` |
| Storage API | `browser.storage.local` | `chrome.storage.local` |
| Options page | In-popup panel | Opens in a new tab |
| Unsupported schemes | `moz-extension:` | `chrome-extension:`, `edge:` |
| CORS origin | `moz-extension://*` | `chrome-extension://*` |
