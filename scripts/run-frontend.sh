#!/usr/bin/env bash
# Start the frontend dev server (Vite, port 18640). Run `npm ci` in frontend/ first.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/frontend"
exec npm run dev
