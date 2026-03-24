param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$runDir = Join-Path $root ".run"
$backendDir = Join-Path $root "backend"
$frontendDir = Join-Path $root "frontend"
$gradleUserHome = Join-Path $root ".gradle-user"
$npmCache = Join-Path $root ".npm-cache"
$backendPidFile = Join-Path $runDir "backend.pid"
$frontendPidFile = Join-Path $runDir "frontend.pid"
$backendOut = Join-Path $runDir "backend.out.log"
$backendErr = Join-Path $runDir "backend.err.log"
$frontendOut = Join-Path $runDir "frontend.out.log"
$frontendErr = Join-Path $runDir "frontend.err.log"
$backendJar = Join-Path $backendDir "build\libs\community-0.0.1-SNAPSHOT.jar"

New-Item -ItemType Directory -Force -Path $runDir, $gradleUserHome, $npmCache | Out-Null

function Get-RunningProcessId {
    param([string]$PidFile)

    if (-not (Test-Path $PidFile)) {
        return $null
    }

    $raw = (Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if (-not $raw) {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return $null
    }

    try {
        $pidValue = [int]$raw
        $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if ($process) {
            return $pidValue
        }
    } catch {
    }

    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    return $null
}

function Wait-HttpReady {
    param(
        [string]$Url,
        [int]$TimeoutSec = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    do {
        try {
            $response = Invoke-WebRequest $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return $true
            }
        } catch {
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    throw "Timed out waiting for $Url"
}

function Resolve-JavaHome17 {
    if ($env:JAVA_HOME_17 -and (Test-Path (Join-Path $env:JAVA_HOME_17 "bin\java.exe"))) {
        return $env:JAVA_HOME_17
    }

    $candidates = @(
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
    )

    $candidates += Get-ChildItem "C:\Program Files\Java" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName
    $candidates += Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (Test-Path (Join-Path $candidate "bin\java.exe")) {
            return $candidate
        }
    }

    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }

    throw "JDK 17 경로를 찾지 못했습니다. JAVA_HOME_17 또는 JAVA_HOME을 설정하세요."
}

function Resolve-GradleBat {
    $wrapperProps = Join-Path $backendDir "gradle\wrapper\gradle-wrapper.properties"
    $distKey = "gradle-8.7-bin"

    if (Test-Path $wrapperProps) {
        $distributionUrl = Select-String -Path $wrapperProps -Pattern "^distributionUrl=(.+)$" |
            ForEach-Object { $_.Matches[0].Groups[1].Value } |
            Select-Object -First 1
        if ($distributionUrl -match "(gradle-[0-9.]+-bin)\.zip") {
            $distKey = $Matches[1]
        }
    }

    $searchRoots = @(
        (Join-Path $env:USERPROFILE ".gradle\wrapper\dists"),
        (Join-Path $gradleUserHome "wrapper\dists")
    )

    foreach ($searchRoot in $searchRoots) {
        if (-not (Test-Path $searchRoot)) {
            continue
        }

        $distRoot = Join-Path $searchRoot $distKey
        if (-not (Test-Path $distRoot)) {
            continue
        }

        $gradleBat = Get-ChildItem $distRoot -Recurse -Filter "gradle.bat" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match "\\gradle-[0-9.]+\\bin\\gradle\.bat$" } |
            Select-Object -First 1
        if ($gradleBat) {
            return $gradleBat.FullName
        }
    }

    throw "Gradle 배포본을 찾지 못했습니다. 먼저 backend 빌드를 한 번 실행해 캐시를 준비하세요."
}

function Start-Backend {
    $existingPid = Get-RunningProcessId $backendPidFile
    if ($existingPid) {
        Write-Host "Backend already running on PID $existingPid"
        return
    }

    $javaHome = Resolve-JavaHome17
    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;$env:Path"
    $env:GRADLE_USER_HOME = $gradleUserHome
    $env:GRADLE_OPTS = "-Dorg.gradle.native=false"

    if (-not $SkipBuild -or -not (Test-Path $backendJar)) {
        $gradleBat = Resolve-GradleBat
        & $gradleBat --no-daemon --console=plain --rerun-tasks bootJar
    }

    if (-not (Test-Path $backendJar)) {
        throw "백엔드 JAR를 찾지 못했습니다: $backendJar"
    }

    Remove-Item $backendOut, $backendErr -Force -ErrorAction SilentlyContinue
    $backend = Start-Process -FilePath (Join-Path $javaHome "bin\java.exe") `
        -ArgumentList "-jar", $backendJar, "--spring.profiles.active=dev" `
        -WorkingDirectory $backendDir `
        -RedirectStandardOutput $backendOut `
        -RedirectStandardError $backendErr `
        -PassThru
    Set-Content -Path $backendPidFile -Value $backend.Id

    try {
        Wait-HttpReady "http://localhost:8080/actuator/health" 90 | Out-Null
    } catch {
        Write-Host "Backend log tail:"
        if (Test-Path $backendOut) {
            Get-Content $backendOut -Tail 80
        }
        if (Test-Path $backendErr) {
            Write-Host "--- STDERR ---"
            Get-Content $backendErr -Tail 40
        }
        throw
    }

    Write-Host "Backend started on http://localhost:8080 (PID $($backend.Id))"
}

function Start-Frontend {
    $existingPid = Get-RunningProcessId $frontendPidFile
    if ($existingPid) {
        Write-Host "Frontend already running on PID $existingPid"
        return
    }

    $env:npm_config_cache = $npmCache

    if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
        Push-Location $frontendDir
        try {
            & npm install
        } finally {
            Pop-Location
        }
    }

    Remove-Item $frontendOut, $frontendErr -Force -ErrorAction SilentlyContinue
    $frontend = Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c", "npm run dev -- --host 0.0.0.0 --strictPort" `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $frontendOut `
        -RedirectStandardError $frontendErr `
        -PassThru
    Set-Content -Path $frontendPidFile -Value $frontend.Id

    try {
        Wait-HttpReady "http://localhost:5173" 60 | Out-Null
    } catch {
        Write-Host "Frontend log tail:"
        if (Test-Path $frontendOut) {
            Get-Content $frontendOut -Tail 80
        }
        if (Test-Path $frontendErr) {
            Write-Host "--- STDERR ---"
            Get-Content $frontendErr -Tail 40
        }
        throw
    }

    Write-Host "Frontend started on http://localhost:5173 (PID $($frontend.Id))"
}

Start-Backend
Start-Frontend

Write-Host ""
Write-Host "Ready:"
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Backend : http://localhost:8080"
Write-Host "  Swagger : http://localhost:8080/swagger-ui/index.html"
