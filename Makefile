.PHONY: up down backend frontend dev logs clean clean-all

ROOT := $(shell pwd)

## Start Docker services (MongoDB + Ollama)
up:
	docker compose up -d

## Stop Docker services
down:
	docker compose down

## Run Spring Boot backend (requires Docker services running)
backend:
	set -a; [ -f "$(ROOT)/.env" ] && . "$(ROOT)/.env"; set +a; cd "$(ROOT)/backend" && ./mvnw spring-boot:run

## Run Vite frontend dev server
frontend:
	cd $(ROOT)/frontend && pnpm dev

## Start Docker services, then launch backend + frontend in parallel
dev: up
	$(ROOT)/dev.sh

## Tail Docker service logs
logs:
	docker compose logs -f

## Stop containers, remove mongo data + build artifacts. Preserves Ollama model weights.
clean:
	docker compose down
	docker volume rm summizer_mongo_data 2>/dev/null || true
	cd $(ROOT)/backend && ./mvnw clean
	cd $(ROOT)/frontend && rm -rf node_modules dist

## Full cleanup including Ollama model weights (triggers re-download on next `make up`)
clean-all:
	docker compose down -v
	cd $(ROOT)/backend && ./mvnw clean
	cd $(ROOT)/frontend && rm -rf node_modules dist
