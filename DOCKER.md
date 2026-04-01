# Docker

Build and run Briefen as a single container. MongoDB and Ollama must be running externally.

## Quick Start (full stack via Compose)

```bash
docker compose --profile app up -d --build
```

This starts MongoDB, Ollama (with model pulls), and the Briefen app. The app is available at `http://localhost:8080`.

## Build the Image

```bash
docker build -t briefen .
```

## Run Standalone

```bash
docker run --rm -p 8080:8080 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/briefen \
  -e OLLAMA_BASE_URL=http://host.docker.internal:11434 \
  briefen
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `MONGODB_URI` | No | `mongodb://localhost:27017/briefen` | MongoDB connection string |
| `OLLAMA_BASE_URL` | No | `http://localhost:11434` | Ollama service URL |
| `OLLAMA_MODEL` | No | `gemma3:4b` | LLM model for summarization |
| `SERVER_PORT` | No | `8080` | HTTP port the app listens on |

## Makefile Targets

```bash
make docker-build   # Build the Docker image
make docker-up      # Start full stack (app + MongoDB + Ollama)
make docker-down    # Stop full stack
```
