# =============================================================================
# Briefen — Multi-stage Docker build
# Produces a single image serving both the React frontend and Spring Boot backend.
# Frontend static assets are embedded into the JAR at build time via
# classpath:/static/, so Spring Boot serves everything — no separate web server.
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build the React frontend
# ---------------------------------------------------------------------------
FROM node:22-alpine AS frontend-build

# APP_BASE_PATH sets the Vite base URL baked into asset paths.
# Must match SERVER_CONTEXT_PATH at runtime.
# Example: --build-arg APP_BASE_PATH=/briefen/
# Default: / (root — correct for the pre-built GHCR image)
ARG APP_BASE_PATH=/
ENV VITE_APP_BASE_PATH=${APP_BASE_PATH}

WORKDIR /app/frontend

# Enable pnpm via corepack
RUN corepack enable && corepack prepare pnpm@latest --activate

# Install dependencies first (layer caching)
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# Build production assets → /app/frontend/dist/
COPY frontend/ ./
RUN pnpm build

# ---------------------------------------------------------------------------
# Stage 2: Build the Spring Boot backend JAR
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS backend-build

# Maven wrapper uses curl to download Maven
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app/backend

# Copy Maven wrapper and config first for dependency caching
COPY backend/mvnw ./
COPY backend/.mvn .mvn
RUN chmod +x mvnw

COPY backend/pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copy backend source
COPY backend/src src

# Copy frontend build output into Spring Boot's static resources directory.
# Spring Boot auto-serves files from classpath:/static/ — no additional config needed.
COPY --from=frontend-build /app/frontend/dist/ src/main/resources/static/

# Package the JAR (skip tests — they run in CI, not during image build)
RUN ./mvnw clean package -DskipTests -B

# ---------------------------------------------------------------------------
# Stage 3: Minimal runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime

# Run as non-root for security
RUN groupadd --system briefen && useradd --system --gid briefen briefen

# Install curl for the HEALTHCHECK (must run as root, before USER switch)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=backend-build /app/backend/target/*.jar app.jar

# Create the data directory for SQLite and set ownership
RUN mkdir -p /data && chown -R briefen:briefen /app /data

# ---------------------------------------------------------------------------
# Environment variables — override at runtime via docker run -e or compose
# ---------------------------------------------------------------------------
# BRIEFEN_DB_PATH  — Path to the SQLite database file (default: ./data/briefen.db)
# OLLAMA_BASE_URL  — Base URL of the Ollama service (default: http://localhost:11434)
# OLLAMA_MODEL     — Ollama model to use for summarization (default: gemma3:4b)
# SERVER_PORT      — Port the app listens on (default: 8080)
# ---------------------------------------------------------------------------

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -sf "http://localhost:8080${SERVER_CONTEXT_PATH:-}/actuator/health" | grep -q '"status":"UP"' || exit 1

USER briefen

ENTRYPOINT ["java", "-jar", "app.jar"]
