# Destroy the database INCLUDING all data, and start it fresh.
# The schema is rebuilt by Liquibase the next time the backend starts —
# the database is disposable by design (session 2).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
docker compose -f "$Root/docker/compose.yaml" down -v
docker compose -f "$Root/docker/compose.yaml" up -d
Write-Host "Fresh database is starting. Run the backend to apply the migrations."
