<#
.SYNOPSIS
    Launches every DebtPulse service in its own window, in dependency order.

.DESCRIPTION
    PowerShell equivalent of start-all.bat.
    Start order matters: Config Server -> Eureka -> Gateway -> business services.

    Prerequisites:
      * MySQL running on localhost:3306 (root/root)
      * Each module built (run build.sh / mvn install first), unless -Build is passed.

.PARAMETER Build
    Run `mvn clean install` (skipping tests) before launching the services.

.PARAMETER ConfigWait
    Seconds to wait after starting config-server before starting eureka-server. Default 25.

.PARAMETER EurekaWait
    Seconds to wait after starting eureka-server before starting the gateway. Default 25.

.EXAMPLE
    .\start-all.ps1

.EXAMPLE
    .\start-all.ps1 -Build

.NOTES
    Run from the repo root. If script execution is blocked, launch with:
      powershell -ExecutionPolicy Bypass -File .\start-all.ps1
#>
[CmdletBinding()]
param(
    [switch]$Build,
    [int]$ConfigWait = 25,
    [int]$EurekaWait = 25
)

$ErrorActionPreference = 'Stop'

# Repo root = folder this script lives in.
$Root = $PSScriptRoot
Set-Location $Root

$Mvn = 'mvn -q -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true spring-boot:run'

function Start-Service {
    param(
        [string]$Title,
        [string]$Module
    )
    $path = Join-Path $Root $Module
    if (-not (Test-Path $path)) {
        Write-Warning "Module directory not found, skipping: $Module"
        return
    }
    # Each service runs in its own persistent window (cmd /k keeps it open on exit).
    Start-Process -FilePath 'cmd.exe' `
        -ArgumentList "/k", "title $Title && cd /d `"$path`" && $Mvn"
    Write-Host "  -> launched $Title" -ForegroundColor Green
}

if ($Build) {
    Write-Host "Building all modules (mvn -U clean install -DskipTests)..." -ForegroundColor Cyan
    # -U forces re-resolution so a previously-cached 'common-lib not found' failure is retried.
    & mvn -U -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true clean install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed (exit $LASTEXITCODE). Aborting launch."
    }
}

Write-Host "Starting config-server (8888)..." -ForegroundColor Cyan
Start-Service -Title 'config-server' -Module 'config-server'
Write-Host "Waiting $ConfigWait s for config-server..." -ForegroundColor DarkGray
Start-Sleep -Seconds $ConfigWait

Write-Host "Starting eureka-server (8761)..." -ForegroundColor Cyan
Start-Service -Title 'eureka-server' -Module 'eureka-server'
Write-Host "Waiting $EurekaWait s for eureka-server..." -ForegroundColor DarkGray
Start-Sleep -Seconds $EurekaWait

Write-Host "Starting api-gateway (9090)..." -ForegroundColor Cyan
Start-Service -Title 'api-gateway' -Module 'api-gateway'

Write-Host "Starting business services..." -ForegroundColor Cyan
Start-Service -Title 'auth-service (8081)'         -Module 'auth-service'
Start-Service -Title 'account-service (8082)'      -Module 'account-service'
Start-Service -Title 'contact-service (8083)'      -Module 'contact-service'
Start-Service -Title 'field-service (8084)'        -Module 'field-service'
Start-Service -Title 'settlement-service (8085)'   -Module 'settlement-service'
Start-Service -Title 'legal-service (8086)'        -Module 'legal-service'
Start-Service -Title 'analytics-service (8087)'    -Module 'analytics-service'
Start-Service -Title 'notification-service (8088)' -Module 'notification-service'

Write-Host ""
Write-Host "All services launching." -ForegroundColor Yellow
Write-Host "  Swagger aggregation: http://localhost:9090/swagger-ui.html"
Write-Host "  Eureka dashboard:    http://localhost:8761"
