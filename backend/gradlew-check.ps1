param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs = @("bootRun")
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$javaHome = if ($env:JAVA_HOME_17) { $env:JAVA_HOME_17 } elseif ($env:JAVA_HOME) { $env:JAVA_HOME } else { $null }
if (-not $javaHome) {
    $msg = @"
[fail] JAVA_HOME 또는 JAVA_HOME_17 환경변수가 필요합니다.
예시(환경별):
  PowerShell(임시): `$env:JAVA_HOME_17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot'
  PowerShell(영구): setx JAVA_HOME_17 "C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot"
  Windows 경로: Microsoft: C:\Program Files\Microsoft\jdk-17\
                 Adoptium: C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot
  Linux/macOS도 동일 개념으로 JAVA_HOME_17(또는 JAVA_HOME) 설정 후 재실행
  예: export JAVA_HOME_17=/usr/lib/jvm/jdk-17
"@
    throw $msg
}

$javaExe = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path $javaExe)) {
    throw "[fail] JDK 17 실행파일을 찾지 못했습니다. 경로: $javaExe"
}

$versionLine = & $javaExe -version 2>&1 | Select-Object -First 1
$versionMatch = [regex]::Match($versionLine, 'version "(?<v>[^"]+)"')
if (-not $versionMatch.Success) {
    throw "[fail] JAVA 버전 파싱 실패: $versionLine"
}

$versionRaw = $versionMatch.Groups["v"].Value
$parts = ($versionRaw -split "[^0-9]") | Where-Object { $_ -ne "" }
if ($parts.Count -eq 0) {
    throw "[fail] JAVA 버전 숫자 파싱 실패: $versionRaw"
}

$major = [int]$parts[0]
if ($major -eq 1 -and $parts.Count -ge 2) {
    $major = [int]$parts[1]
}

if ($major -lt 17) {
    throw "[fail] JDK $major 감지됨. 이 프로젝트는 Java 17+가 필요합니다. (현재 JAVA_HOME=$javaHome)"
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "dev"
}
if ($env:SPRING_PROFILES_ACTIVE.Length -eq 0) {
    $env:SPRING_PROFILES_ACTIVE = "dev"
}

$diag = "[diag] JAVA_HOME=$javaHome, JAVA_VERSION=$versionRaw, SPRING_PROFILES_ACTIVE=$env:SPRING_PROFILES_ACTIVE, GRADLE_ARGS=$($GradleArgs -join ' ')"
Write-Output $diag

$wrapper = Join-Path $scriptDir "gradlew.bat"
if (-not (Test-Path $wrapper)) {
    throw "[fail] gradlew.bat를 찾을 수 없습니다. 위치: $wrapper"
}

& $wrapper @GradleArgs
