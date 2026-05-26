# A-02 acceptance: full-game LLM-first + JSON parse rate (requires DEEPSEEK_API_KEY)
# Report: target/reports/a02-full-game-*.json
$ErrorActionPreference = "Stop"
Set-Location (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent)
if (-not $env:DEEPSEEK_API_KEY) {
    Write-Error "Set DEEPSEEK_API_KEY before running A-02 acceptance."
}
.\mvnw.cmd test "-Dtest=A02FullGameLlmAcceptanceTest" "-DfailIfNoTests=false"
$latest = Get-ChildItem target/reports/a02-full-game-*.json | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Write-Host "`nReport: $($latest.FullName)"
