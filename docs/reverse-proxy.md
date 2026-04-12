# Reverse Proxy Configuration

This guide covers configuring the three most common reverse proxies for use with Briefen.

---

## Before you start

Two settings must be correct for every setup:

**1 — Raise timeouts to 310 seconds**

Summarizing a long article with a local Ollama model can take 2–3 minutes. Every reverse proxy ships with a default timeout of 60 seconds. That default will kill the connection mid-request, returning a network error to the browser even though the summary completed fine on the backend. Set all proxy timeouts to **310 s** — 10 seconds more than Briefen's own Ollama client timeout.

**2 — Enable forwarded-header trust**

Set `SERVER_FORWARD_HEADERS_STRATEGY=FRAMEWORK` in Briefen's environment so Spring Boot reads the `X-Forwarded-Proto` and `X-Forwarded-Host` headers that the proxy sends. Without this, any server-generated redirect (e.g. after login) uses `http://` even on an HTTPS deployment.

```yaml
# docker-compose.sample.yml → environment:
SERVER_FORWARD_HEADERS_STRATEGY: FRAMEWORK
```

---

## Nginx

### Minimal HTTPS config

```nginx
server {
    listen 443 ssl;
    server_name briefen.example.com;

    ssl_certificate     /etc/ssl/certs/briefen.crt;
    ssl_certificate_key /etc/ssl/private/briefen.key;

    location / {
        proxy_pass              http://127.0.0.1:8080;

        # ── Required: raise timeouts for long Ollama requests ──────────────
        proxy_read_timeout      310s;
        proxy_send_timeout      310s;
        proxy_connect_timeout   10s;

        # ── Required: forward original host/scheme to Spring Boot ──────────
        proxy_set_header        Host              $host;
        proxy_set_header        X-Real-IP         $remote_addr;
        proxy_set_header        X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header        X-Forwarded-Proto $scheme;

        # ── Recommended: buffer settings for streaming responses ───────────
        proxy_buffering         off;
        proxy_request_buffering off;
    }
}

# Redirect HTTP → HTTPS
server {
    listen 80;
    server_name briefen.example.com;
    return 301 https://$host$request_uri;
}
```

### Nginx Proxy Manager (NPM)

If you use Nginx Proxy Manager, open the proxy host → **Edit → Advanced** and paste into the **Custom Nginx Configuration** box:

```nginx
proxy_read_timeout    310s;
proxy_send_timeout    310s;
proxy_connect_timeout 10s;
proxy_buffering       off;
```

NPM already sets the `X-Forwarded-*` headers automatically — no extra configuration needed.

### Sub-path deployment with Nginx

If Briefen is built locally with a sub-path (`APP_BASE_PATH=/briefen/`), configure the proxy to pass the prefix to the app:

```nginx
location /briefen/ {
    proxy_pass              http://127.0.0.1:8080/briefen/;
    proxy_read_timeout      310s;
    proxy_send_timeout      310s;
    proxy_connect_timeout   10s;
    proxy_set_header        Host              $host;
    proxy_set_header        X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header        X-Forwarded-Proto $scheme;
}
```

Set `SERVER_CONTEXT_PATH=/briefen/` in the Briefen environment to match.

> If you want the proxy to **strip** the prefix before forwarding (so Briefen sees `/`), use `proxy_pass http://127.0.0.1:8080/;` (trailing slash) and leave `SERVER_CONTEXT_PATH` unset.

---

## Caddy

Caddy automatically provisions and renews TLS certificates via Let's Encrypt and has no default proxy timeout, so no explicit timeout configuration is required.

### Minimal HTTPS config

```caddy
briefen.example.com {
    reverse_proxy localhost:8080
}
```

That's it. Caddy handles TLS, sets `X-Forwarded-*` headers, and has no timeout by default.

### With explicit header passthrough (optional)

```caddy
briefen.example.com {
    reverse_proxy localhost:8080 {
        header_up X-Real-IP {remote_host}
    }
}
```

### Sub-path deployment with Caddy

```caddy
myserver.example.com {
    handle_path /briefen/* {
        rewrite * /briefen{uri}
        reverse_proxy localhost:8080
    }
}
```

Set `SERVER_CONTEXT_PATH=/briefen/` and build the image with `APP_BASE_PATH=/briefen/`.

---

## Traefik

### Docker Compose label-based config

Add these labels to the Briefen service in your compose file:

```yaml
services:
  app:
    image: ghcr.io/lucaslra/briefen:latest
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.briefen.rule=Host(`briefen.example.com`)"
      - "traefik.http.routers.briefen.entrypoints=websecure"
      - "traefik.http.routers.briefen.tls.certresolver=letsencrypt"
      - "traefik.http.services.briefen.loadbalancer.server.port=8080"
```

> **Note:** Traefik does not support per-service response timeouts via labels. Timeout configuration is set globally in the static config — see below.

Add to your **Traefik static configuration** (`traefik.yml` or `traefik.toml`):

```yaml
# traefik.yml
serversTransport:
  respondingTimeouts:
    readTimeout: 310s
    writeTimeout: 310s
    idleTimeout: 310s
```

Or with environment variables if you run Traefik in Docker:

```yaml
environment:
  - TRAEFIK_SERVERSTRANSPORT_RESPONDINGTIMEOUTS_READTIMEOUT=310s
  - TRAEFIK_SERVERSTRANSPORT_RESPONDINGTIMEOUTS_WRITETIMEOUT=310s
```

> **Note:** Traefik's default `readTimeout` for backend responses is 0 (unlimited), so if you are not seeing timeouts you may already be fine. Check your static config to be sure.

### Sub-path deployment with Traefik

```yaml
labels:
  - "traefik.http.routers.briefen.rule=Host(`myserver.example.com`) && PathPrefix(`/briefen`)"
  - "traefik.http.middlewares.briefen-strip.stripPrefix.prefixes=/briefen"
  - "traefik.http.routers.briefen.middlewares=briefen-strip"
```

Using `stripPrefix` means the proxy removes `/briefen` before forwarding — leave `SERVER_CONTEXT_PATH` unset in this case (the app sees all requests at `/`).

---

## Protecting the Ollama port

Regardless of which proxy you use, ensure the Ollama API port (`11434`) is **never** exposed to the network. Ollama has no authentication. The default `docker-compose.sample.yml` binds it to `127.0.0.1:11434` — do not change this.

```yaml
# Correct — loopback only
ports:
  - "127.0.0.1:11434:11434"

# WRONG — exposed to all interfaces
ports:
  - "11434:11434"
```

---

## Binding Briefen to localhost

When a reverse proxy runs on the same host as Briefen, you can restrict Briefen to only accept connections from localhost, preventing direct access that would bypass the proxy:

```yaml
# docker-compose.sample.yml → environment:
SERVER_BIND_ADDRESS: 127.0.0.1
```

Then in the proxy, use `proxy_pass http://127.0.0.1:8080` (or equivalent). The Briefen port no longer needs to be published to the host at all — remove or replace the `ports:` entry with a network-only reference.
