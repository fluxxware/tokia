#!/usr/bin/env bash
#
# Orchestrator for the Tokia happy-path integration test.
#
# Scaffolds the throwaway consumer (tests/fixtures/consumer), applies the plugin from mavenLocal on a
# pinned Gradle/Kotlin/JVM, and asserts that `generateScssTokens` produces the expected tokens.
# Each phase is a standalone script in tests/steps/ (see tests/GUIDE.md).
#
# Env:   TOKIA_TEST_GRADLE, TOKIA_TEST_KOTLIN, TOKIA_TEST_PLUGIN_VERSION, REPO_ROOT, JAVA_HOME
# Flags: --cached                reuse a persistent Gradle home (see tests/GUIDE.md)
# Env:   TOKIA_TEST_CACHED=1     same as --cached
#        TOKIA_TEST_GRADLE_HOME  cache path override
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/lib/common.sh"

CACHED="${TOKIA_TEST_CACHED:-0}"
for arg in "$@"; do
  case "$arg" in
    --cached) CACHED=1 ;;
    *) die "unknown argument '$arg'" ;;
  esac
done

require_env REPO_ROOT TOKIA_TEST_GRADLE TOKIA_TEST_KOTLIN TOKIA_TEST_PLUGIN_VERSION JAVA_HOME

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
export WORK CACHED

if [ "$CACHED" = 1 ]; then
  GRADLE_USER_HOME="${TOKIA_TEST_GRADLE_HOME:-${XDG_CACHE_HOME:-$HOME/.cache}/tokia-tests/gradle-home}"
else
  GRADLE_USER_HOME="$WORK/.gradle-home"
fi
export GRADLE_USER_HOME
mkdir -p "$GRADLE_USER_HOME"

echo "== tokia happy-path: gradle=${TOKIA_TEST_GRADLE} kotlin=${TOKIA_TEST_KOTLIN} jdk=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1) =="
echo "   gradle user home: $GRADLE_USER_HOME (cached=$CACHED)"

for step in 10-wrapper 20-project 30-run 40-assert; do
  echo "▶ $step"
  if ! bash "$HERE/steps/$step.sh"; then
    echo "✗ $step failed"
    bash "$HERE/steps/99-diagnose.sh" || true
    exit 1
  fi
done

echo "PASS: gradle=${TOKIA_TEST_GRADLE} kotlin=${TOKIA_TEST_KOTLIN}"
