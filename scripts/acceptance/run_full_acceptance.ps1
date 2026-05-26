# Full A-side acceptance: G-06 unit + A-02 LLM-first full game (requires DEEPSEEK_API_KEY for A-02)
# Reports: target/reports/a02-full-game-*.json
$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $Root

Write-Host "=== G-06 + PhaseTimeout unit tests ==="
.\mvnw.cmd -q test "-Dtest=PhaseTimeoutHandlerTest,WolfVoteResolverTest"

if (-not $env:DEEPSEEK_API_KEY) {
    Write-Warning "DEEPSEEK_API_KEY not set — skipping A-02 live LLM full-game acceptance."
    exit 0
}

Write-Host "`n=== A-02 LLM-first full game (Mock fallback only) ==="
.\mvnw.cmd test "-Dtest=A02FullGameLlmAcceptanceTest" "-DfailIfNoTests=false"

$latest = Get-ChildItem target/reports/a02-full-game-*.json -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($latest) {
    Write-Host "`nReport: $($latest.FullName)"
    Get-Content $latest.FullName -Raw | ConvertFrom-Json | ForEach-Object {
        Write-Host "passed: $($_.passed)"
        $_.checks | ForEach-Object { Write-Host "  [$($_.pass)] $($_.name): $($_.detail)" }
    }
}
