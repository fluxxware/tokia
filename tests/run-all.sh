#!/usr/bin/env bash
#
# Runs every atomic happy-path case under tests/cases/*.nix.
# Each case is self-contained (its own JVM + Gradle + Kotlin) and reports PASS/FAIL independently.
#
# Usage:  tests/run-all.sh [--cached]
#
#   --cached   use ONE shared, persistent Gradle home so the Gradle distributions + dependencies
#              download once and are reused across cases and runs. Location:
#              $TOKIA_TEST_GRADLE_HOME, else ${XDG_CACHE_HOME:-$HOME/.cache}/tokia-tests/gradle-home.
#              Without the flag, each case uses a fresh temp home (clean-room, re-downloads).
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

CACHED=0
for arg in "$@"; do
  case "$arg" in
    --cached) CACHED=1 ;;
    *) echo "run-all: unknown argument '$arg'" >&2; exit 2 ;;
  esac
done

if [ "$CACHED" = 1 ]; then
  export TOKIA_TEST_CACHED=1
  echo "== cached mode: gradle home = ${TOKIA_TEST_GRADLE_HOME:-${XDG_CACHE_HOME:-$HOME/.cache}/tokia-tests/gradle-home}"
fi

echo "== publishing Tokia to mavenLocal (so the cases can resolve io.github.fluxxware.tokia) =="
( cd "$ROOT" && nix-shell --run './gradlew publishToMavenLocal --console=plain' ) || {
  echo "FAIL: could not publish the plugin to mavenLocal"; exit 1;
}

HP="bash \"$ROOT/tests/happy_path.sh\""
[ "$CACHED" = 1 ] && HP="$HP --cached"

fail=0
for case_file in "$ROOT"/tests/cases/*.nix; do
  echo
  echo "################ $(basename "$case_file") ################"
  if nix-shell "$case_file" --run "$HP"; then
    echo "--> PASS $(basename "$case_file")"
  else
    echo "--> FAIL $(basename "$case_file")"
    fail=1
  fi
done

echo
if [ "$fail" -eq 0 ]; then
  echo "ALL CASES PASSED"
else
  echo "SOME CASES FAILED"
  exit 1
fi
