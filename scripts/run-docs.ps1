# Serve the project documentation (mdBook) with live reload on port 18740.
# Install mdBook once: https://rust-lang.github.io/mdBook/guide/installation.html
#   Windows:  winget install mdbook  (or: cargo install mdbook)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command mdbook -ErrorAction SilentlyContinue)) {
    Write-Error @"
mdbook is not installed. Install it first:
  Windows:  winget install mdbook  (or: cargo install mdbook)
  Guide:    https://rust-lang.github.io/mdBook/guide/installation.html
"@
}

Set-Location "$Root/docs"
mdbook serve --port 18740 --open
