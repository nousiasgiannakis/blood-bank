#Requires -RunAsAdministrator
# setup-tools.ps1 - install the course toolchain on Windows with Chocolatey.
#
# Idempotent: `choco install` skips packages that are already installed. Safe
# as the first step on a fresh Windows machine, or to bring an existing one up
# to the baseline in README.md, section Tools. macOS: scripts/setup-tools.sh.
#
# Usage (PowerShell *as Administrator* - Chocolatey installs under Program Files):
#   Set-ExecutionPolicy -Scope Process Bypass -Force   # only if scripts are blocked
#   .\scripts\setup-tools.ps1
$ErrorActionPreference = "Stop"

if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "Chocolatey not found - installing (official bootstrap from chocolatey.org)..."
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    $env:Path = "$env:ProgramData\chocolatey\bin;$env:Path"
}

# Required - one line per row of README.md, section Tools.
#   git             clone, branch, PR (your contribution is the history under your name)
#   gh              GitHub CLI - pull requests from the terminal (optional, cheap)
#   temurin25       Eclipse Temurin JDK 25: the Gradle wrapper runs on it and
#                   compiles with it; nothing downloads a JDK for you
#   nodejs-lts      Node.js + npm for the Vite frontend (any 20+)
#   python          python3 for scripts/check-changesets (the formatted-SQL gate)
#   docker-desktop  PostgreSQL locally, throwaway PostgreSQL in the tests
# Not installed on purpose: Gradle - backend\gradlew.bat downloads the pinned
# version itself; a second, system-wide Gradle only invites version mix-ups.
# Optional mdBook (renders docs/) has no Chocolatey package: `cargo install mdbook`
# or the zip from https://github.com/rust-lang/mdBook/releases.
$ErrorActionPreference = "Continue"   # native commands report through exit codes, not exceptions
choco install -y git gh temurin25 nodejs-lts python docker-desktop
if ($LASTEXITCODE -eq 3010) {
    Write-Host "Chocolatey: a reboot is required to finish one of the installs."
} elseif ($LASTEXITCODE -ne 0) {
    Write-Host "Chocolatey exited with code $LASTEXITCODE - check the output above, then re-run this script."
    exit $LASTEXITCODE
}

# Docker Desktop runs on WSL 2. `wsl --install` enables it (one reboot); Docker
# Desktop's first start then completes the setup. Native commands writing to
# stderr would abort a "Stop" session in Windows PowerShell 5.1, hence the guard.
$wslReady = $false
if (Get-Command wsl.exe -ErrorAction SilentlyContinue) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try { & wsl.exe --status *> $null; $wslReady = ($LASTEXITCODE -eq 0) } catch { $wslReady = $false }
    $ErrorActionPreference = $previous
}
if (-not $wslReady) {
    Write-Host ""
    Write-Host "WSL 2 is not ready. Run once, then reboot:  wsl --install"
}

# Make the freshly installed tools visible in THIS shell (new shells get them anyway).
Import-Module "$env:ChocolateyInstall\helpers\chocolateyProfile.psm1" -ErrorAction SilentlyContinue
if (Get-Command refreshenv -ErrorAction SilentlyContinue) { refreshenv | Out-Null }

Write-Host ""
Write-Host "Toolchain check:"
$ErrorActionPreference = "Continue"
foreach ($cmd in "git", "gh", "java", "node", "npm", "python", "docker") {
    # The Microsoft Store ships stub python.exe/python3.exe aliases under
    # WindowsApps that only open the Store - not an interpreter.
    $found = Get-Command $cmd -ErrorAction SilentlyContinue
    if ($found -and $found.Source -notlike "*\WindowsApps\*") {
        try { $version = (& $cmd --version 2>&1 | Select-Object -First 1) } catch { $version = "(installed)" }
        Write-Host ("  {0,-8} {1}" -f $cmd, $version)
    } else {
        Write-Host ("  {0,-8} MISSING (open a new PowerShell window and re-check)" -f $cmd)
    }
}
Write-Host ""
Write-Host "Next: start Docker Desktop once (it finishes the WSL 2 setup on first start),"
Write-Host "then set up the GitHub Packages token - README.md, section Tools > GitHub token."
