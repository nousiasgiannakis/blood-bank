# Check every Liquibase changelog against the formatted-SQL grammar and layout
# rules. Run before any PR that touches backend/src/main/resources/**/db/changelog.
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

# Windows installs of Python differ in what they put on PATH: `python3`
# (some), `python` (the official installer, Chocolatey), or only the `py`
# launcher. The Microsoft Store also plants python.exe/python3.exe stubs under
# WindowsApps that merely open the Store - skip those.
$python = $null
foreach ($name in "python3", "python") {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd -and $cmd.Source -notlike "*\WindowsApps\*") { $python = @($cmd.Source); break }
}
if (-not $python -and (Get-Command py -ErrorAction SilentlyContinue)) { $python = @("py", "-3") }
if (-not $python) {
    Write-Error "Python 3 not found. Install it (README.md, section Tools: scripts\setup-tools.ps1) and open a new shell."
}

$exe = $python[0]
$pyArgs = @()
if ($python.Length -gt 1) { $pyArgs = @($python[1..($python.Length - 1)]) }
& $exe @pyArgs scripts/check_changesets.py
exit $LASTEXITCODE
