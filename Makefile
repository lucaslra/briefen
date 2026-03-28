.PHONY: up down backend frontend dev logs clean

ROOT := $(shell pwd)

## Start Docker services (MongoDB + Ollama)
up:
	docker compose up -d

## Stop Docker services
down:
	docker compose down

## Run Spring Boot backend (requires Docker services running)
backend:
	cd $(ROOT)/backend && ./mvnw spring-boot:run

## Run Vite frontend dev server
frontend:
	cd $(ROOT)/frontend && pnpm dev

## Start Docker services, then launch backend + frontend in parallel
dev: up
	$(ROOT)/dev.sh

## Tail Docker service logs
logs:
	docker compose logs -f

## Full cleanup: stop containers, remove volumes, clean builds
clean:
	docker compose down -v
	cd $(ROOT)/backend && ./mvnw clean
	cd $(ROOT)/frontend && rm -rf node_modules dist
