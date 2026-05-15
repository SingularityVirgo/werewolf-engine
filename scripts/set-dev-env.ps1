# Persist werewolf-engine dev env vars to Windows (User or Machine scope).
# Usage:
#   .\scripts\set-dev-env.ps1 -ApiKey "sk-..."
#   .\scripts\set-dev-env.ps1 -ApiKey "sk-..." -Scope Machine   # requires Administrator

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

Write-Host "Set DEEPSEEK_API_KEY and SPRING_PROFILES_ACTIVE=dev ($Scope scope)."
Write-Host 'Restart IDE / terminal for new processes to pick up the variables.'
