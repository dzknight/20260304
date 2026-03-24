$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$runDir = Join-Path $root ".run"

function Stop-ProcessTree {
    param(
        [string]$Name,
        [string]$PidFile
    )

    if (-not (Test-Path $PidFile)) {
        Write-Host "$Name is not tracked."
        return
    }

    $raw = (Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if (-not $raw) {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        Write-Host "$Name pid file was empty."
        return
    }

    try {
        $pidValue = [int]$raw
    } catch {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        Write-Host "$Name pid file was invalid."
        return
    }

    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($process) {
        & taskkill /PID $pidValue /T /F | Out-Null
        Write-Host "$Name stopped (PID $pidValue)"
    } else {
        Write-Host "$Name was already stopped."
    }

    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

Stop-ProcessTree "Frontend" (Join-Path $runDir "frontend.pid")
Stop-ProcessTree "Backend" (Join-Path $runDir "backend.pid")
