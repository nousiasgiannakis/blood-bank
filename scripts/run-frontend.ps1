# Start the frontend dev server (Vite, port 18640). Run `npm ci` in frontend/ first.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location "$Root/frontend"
npm run dev
