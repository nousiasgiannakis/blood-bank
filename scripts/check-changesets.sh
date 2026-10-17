#!/usr/bin/env bash
# Check every Liquibase changelog against the formatted-SQL grammar and layout
# rules. Run before any PR that touches backend/src/main/resources/**/db/changelog.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
exec python3 scripts/check_changesets.py
