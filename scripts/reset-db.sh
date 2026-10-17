#!/usr/bin/env bash
# Destroy the database INCLUDING all data, and start it fresh.
# The schema is rebuilt by Liquibase the next time the backend starts —
# the database is disposable by design (session 2).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
docker compose -f "$ROOT/docker/compose.yaml" down -v
docker compose -f "$ROOT/docker/compose.yaml" up -d
echo "Fresh database is starting. Run the backend to apply the migrations."
