# Start PostgreSQL (docker) and the backend with the local profile.
# Usage: .\scripts\run-backend.ps1
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

docker compose -f "$Root/docker/compose.yaml" up -d

Write-Host "Waiting for PostgreSQL to be healthy..."
do {
    Start-Sleep -Seconds 1
    $status = docker inspect -f '{{.State.Health.Status}}' ite4120-postgres-1 2>$null
} until ($status -eq "healthy")

Set-Location "$Root/backend"
./gradlew.bat bootRun --console=plain --args="--spring.profiles.active=local"
