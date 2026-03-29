#!/bin/sh
# Starts both backend and frontend dev servers.
# Ctrl+C kills both processes.

set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

cleanup() {
  echo ""
  echo "Shutting down..."
  kill -- -$BACKEND_PGID -$FRONTEND_PGID 2>/dev/null
  wait $BACKEND_PID $FRONTEND_PID 2>/dev/null
  echo "Done."
}
trap cleanup EXIT INT TERM

echo "Starting backend..."
setsid sh -c "cd '$ROOT/backend' && ./mvnw spring-boot:run" &
BACKEND_PID=$!
BACKEND_PGID=$!

echo "Installing frontend dependencies..."
(cd "$ROOT/frontend" && pnpm install --silent)

echo "Starting frontend..."
setsid sh -c "cd '$ROOT/frontend' && pnpm dev" &
FRONTEND_PID=$!
FRONTEND_PGID=$!

wait
