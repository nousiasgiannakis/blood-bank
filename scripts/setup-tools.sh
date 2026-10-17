#!/usr/bin/env bash
# setup-tools.sh — install the course toolchain on macOS with Homebrew.
#
# Idempotent: re-running on a machine that already has these packages is a
# no-op (each `brew install` exits 0 with an "already installed" notice). Safe
# as the first step on a fresh Mac, or to bring an existing one up to the
# baseline in README.md § Tools. Windows: scripts/setup-tools.ps1.
#
# Usage: ./scripts/setup-tools.sh
set -euo pipefail

# Xcode Command Line Tools — prerequisite for Homebrew itself and for anything
# that compiles native code (some npm packages do). `xcode-select --install`
# opens a GUI dialog and returns immediately; finish it, then re-run.
if ! xcode-select -p >/dev/null 2>&1; then
  echo "Xcode Command Line Tools not found — launching the installer…"
  xcode-select --install || true
  echo "Finish the GUI install, then re-run: $0"
  exit 0
fi

if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew not found — installing…"
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  # Apple Silicon installs Homebrew at /opt/homebrew, which is not on PATH yet.
  if [[ -x /opt/homebrew/bin/brew ]]; then
    eval "$(/opt/homebrew/bin/brew shellenv)"
  fi
fi

brew update

# Required — one line per row of README.md § Tools.
#   git         clone, branch, PR (your contribution is the history under your name)
#   gh          GitHub CLI — pull requests from the terminal (optional, cheap)
#   openjdk@25  the Gradle wrapper runs on it and compiles with it; nothing
#               downloads a JDK for you (no toolchain resolver is configured)
#   node        Node.js + npm for the Vite frontend (any 20+; brew ships current)
#   python      python3 for scripts/check-changesets (the formatted-SQL gate)
# Not installed on purpose: Gradle — backend/gradlew downloads the pinned
# version itself; a second, system-wide Gradle only invites version mix-ups.
brew install \
  git \
  gh \
  openjdk@25 \
  node \
  python

# Optional — renders docs/ as a book (`cd docs && mdbook serve`).
brew install mdbook

# Docker Desktop — PostgreSQL locally, throwaway PostgreSQL in the tests.
brew install --cask docker-desktop

# openjdk@25 is keg-only: Homebrew does not put it on PATH, and macOS's own
# /usr/bin/java only finds JDKs registered under /Library/Java. Register it
# there (one symlink, needs your password) so `java`, `/usr/libexec/java_home`
# and the Gradle wrapper all find it. Idempotent.
JDK="$(brew --prefix openjdk@25)/libexec/openjdk.jdk"
LINK=/Library/Java/JavaVirtualMachines/openjdk-25.jdk
if [[ ! -e "$LINK" ]]; then
  echo "Registering JDK 25 with macOS (sudo — one symlink under /Library/Java)…"
  if ! sudo ln -sfn "$JDK" "$LINK"; then
    echo "Could not create $LINK. Alternative: add this to ~/.zprofile and open a new shell:"
    echo "  export PATH=\"$(brew --prefix openjdk@25)/bin:\$PATH\""
  fi
fi

brew cleanup

echo
echo "Toolchain check:"
for cmd in git gh java node npm python3 docker mdbook; do
  if command -v "$cmd" >/dev/null 2>&1; then
    printf '  %-8s %s\n' "$cmd" "$("$cmd" --version 2>&1 | head -1)"
  else
    printf '  %-8s MISSING\n' "$cmd"
  fi
done
echo
echo "Next: start Docker Desktop once (it asks for permissions the first time),"
echo "then set up the GitHub Packages token — README.md § Tools › GitHub token."
