# Persist werewolf-engine dev env vars to Windows (User or Machine scope).
# Usage:
#   .\scripts\dev\set-dev-env.ps1 -ApiKey "sk-..."
#   .\scripts\dev\set-dev-env.ps1 -ApiKey "sk-..." -Scope Machine   # requires Administrator

param(
    [Parameter(Mandatory = $true)]
    [string] $ApiKey,
    [ValidateSet('User', 'Machine')]
    [string] $Scope = 'User'
)

if ($Scope -eq 'Machine' -and -not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error 'Machine scope requires running PowerShell as Administrator.'
    exit 1
}

[Environment]::SetEnvironmentVariable('DEEPSEEK_API_KEY', $ApiKey, $Scope)
[Environment]::SetEnvironmentVariable('SPRING_PROFILES_ACTIVE', 'dev', $Scope)
# JVM + console UTF-8 (Chinese prompts/logs on Windows default GBK locale)
[Environment]::SetEnvironmentVariable('JAVA_TOOL_OPTIONS', '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8', $Scope)

Write-Host "Set DEEPSEEK_API_KEY, SPRING_PROFILES_ACTIVE=dev, JAVA_TOOL_OPTIONS=UTF-8 ($Scope scope)."
Write-Host 'Restart IDE / terminal for new processes to pick up the variables.'
