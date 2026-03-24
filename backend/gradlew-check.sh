#!/usr/bin/env bash
# Bash-only helper. 실행권한 없이도 아래처럼 직접 bash로 실행하세요.
#   bash ./gradlew-check.sh [bootRun|...]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAVA_HOME_CANDIDATE="${JAVA_HOME_17:-${JAVA_HOME:-}}"
if [ -z "${JAVA_HOME_CANDIDATE}" ]; then
  cat <<'EOF'
[fail] JAVA_HOME 또는 JAVA_HOME_17 환경변수가 필요합니다.
예시(환경별):
  Bash(임시): export JAVA_HOME_17=/usr/lib/jvm/jdk-17
  Windows w/ WSL: export JAVA_HOME_17=/usr/lib/jvm/java-17-openjdk-amd64
  Linux SDKMAN: export JAVA_HOME_17="$HOME/.sdkman/candidates/java/17.0.xx-tem"
  macOS Homebrew: export JAVA_HOME_17="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
  영구 적용(.bashrc): echo 'export JAVA_HOME_17=...' >> ~/.bashrc; source ~/.bashrc
  또는 JAVA_HOME을 JDK 17 경로로 설정해도 동작합니다.
EOF
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_CANDIDATE"
JAVA_BIN="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA_BIN" ]; then
  echo "[fail] JDK 실행파일을 찾지 못했습니다: $JAVA_BIN"
  exit 1
fi

VERSION_LINE="$("$JAVA_BIN" -version 2>&1 | head -n 1 || true)"
VERSION_RAW="$(echo "$VERSION_LINE" | awk -F '"' '{print $2}')"
if [ -z "$VERSION_RAW" ]; then
  echo "[fail] JAVA 버전 파싱 실패: $VERSION_LINE"
  exit 1
fi

PART1="$(echo "$VERSION_RAW" | awk -F '.' '{print $1}')"
PART2="$(echo "$VERSION_RAW" | awk -F '.' '{print $2}')"
MAJOR="$PART1"
if [ "$PART1" = "1" ] && [ -n "$PART2" ]; then
  MAJOR="$PART2"
fi

if ! echo "$MAJOR" | grep -qE '^[0-9]+$'; then
  echo "[fail] JAVA major 버전 판독 실패: $VERSION_RAW"
  exit 1
fi

if [ "$MAJOR" -lt 17 ]; then
  echo "[fail] JDK $MAJOR 감지됨. 이 프로젝트는 Java 17+가 필요합니다. (JAVA_HOME=$JAVA_HOME)"
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

if [ "$#" -gt 0 ]; then
  GRADLE_ARGS=("$@")
else
  GRADLE_ARGS=(bootRun)
fi

echo "[diag] JAVA_HOME=$JAVA_HOME JAVA_VERSION=$VERSION_RAW SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE GRADLE_ARGS=${GRADLE_ARGS[*]}"

WRAPPER="$SCRIPT_DIR/gradlew"
if [ ! -x "$WRAPPER" ]; then
  echo "[fail] gradlew 실행파일을 찾지 못했습니다. 위치: $WRAPPER"
  exit 1
fi

exec "$WRAPPER" "${GRADLE_ARGS[@]}"
