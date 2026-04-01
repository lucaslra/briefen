# =============================================================================
# Briefen — Multi-stage Docker build
# Produces a single image serving both the React frontend and Spring Boot backend.
# Frontend static assets are embedded into the JAR at build time via
# classpath:/static/, so Spring Boot serves everything — no separate web server.
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build the React frontend
# ---------------------------------------------------------------------------
FROM node:25-alpine AS frontend-build

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

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=backend-build /app/backend/target/*.jar app.jar

RUN chown -R briefen:briefen /app
USER briefen

# ---------------------------------------------------------------------------
# Environment variables — override at runtime via docker run -e or compose
# ---------------------------------------------------------------------------
# MONGODB_URI      — Full MongoDB connection string (default: mongodb://localhost:27017/briefen)
# OLLAMA_BASE_URL  — Base URL of the Ollama service (default: http://localhost:11434)
# OLLAMA_MODEL     — Ollama model to use for summarization (default: gemma3:4b)
# SERVER_PORT      — Port the app listens on (default: 8080)
# ---------------------------------------------------------------------------

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
