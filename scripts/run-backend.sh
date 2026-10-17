#!/usr/bin/env bash
# Start PostgreSQL (docker) and the backend with the local profile.
# Usage: ./scripts/run-backend.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

docker compose -f "$ROOT/docker/compose.yaml" up -d

echo "Waiting for PostgreSQL to be healthy..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' ite4120-postgres-1 2>/dev/null)" = "healthy" ]; do
  sleep 1
done

cd "$ROOT/backend"
exec ./gradlew bootRun --console=plain --args="--spring.profiles.active=local"
