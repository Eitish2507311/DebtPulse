<#
.SYNOPSIS
    Stops every DebtPulse service by freeing the ports they listen on.

.DESCRIPTION
    Counterpart to start-all.ps1. Finds the process listening on each known
    DebtPulse port and terminates it, closing the associated service window.

    Ports:
      8888 config-server   8761 eureka-server   9090 api-gateway
      8081 auth   8082 account   8083 contact   8084 field
      8085 settlement   8086 legal   8087 analytics   8088 notification

.PARAMETER Ports
    Override the default port list if you run on non-standard ports.

.EXAMPLE
    .\stop-all.ps1

.NOTES
    If script execution is blocked, launch with:
      powershell -ExecutionPolicy Bypass -File .\stop-all.ps1
#>
[CmdletBinding()]
param(
    [int[]]$Ports = @(8888, 8761, 9090, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088)
)

$ErrorActionPreference = 'Stop'

function Stop-Port {
    param([int]$Port)

    # Find PIDs listening on this port (LISTEN state only).
    $pids = @()
    try {
        $pids = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
                Select-Object -ExpandProperty OwningProcess -Unique
    } catch {
        # Get-NetTCPConnection throws when no connection matches -> nothing to stop.
        $pids = @()
    }

    if (-not $pids -or $pids.Count -eq 0) {
        Write-Host ("  port {0,-4} : nothing listening" -f $Port) -ForegroundColor DarkGray
        return
    }

    foreach ($procId in $pids) {
        try {
            $p = Get-Process -Id $procId -ErrorAction Stop
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host ("  port {0,-4} : stopped PID {1} ({2})" -f $Port, $procId, $p.ProcessName) -ForegroundColor Green
        } catch {
            Write-Warning ("  port {0} : could not stop PID {1} - {2}" -f $Port, $procId, $_.Exception.Message)
        }
    }
}

Write-Host "Stopping DebtPulse services..." -ForegroundColor Cyan
foreach ($port in $Ports) {
    Stop-Port -Port $port
}

Write-Host ""
Write-Host "Done. All known DebtPulse ports have been checked." -ForegroundColor Yellow
