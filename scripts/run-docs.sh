#!/usr/bin/env bash
# Serve the project documentation (mdBook) with live reload on port 18740.
# Install mdBook once: https://rust-lang.github.io/mdBook/guide/installation.html
#   macOS:  brew install mdbook
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v mdbook >/dev/null 2>&1; then
  echo "mdbook is not installed. Install it first:" >&2
  echo "  macOS:          brew install mdbook" >&2
  echo "  Windows/Linux:  https://rust-lang.github.io/mdBook/guide/installation.html" >&2
  exit 1
fi

cd "$ROOT/docs"
exec mdbook serve --port 18740 --open
