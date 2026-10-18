#!/usr/bin/env bash
# Step 99 — diagnostics on failure (never fails itself). Prints the generated tree + sass cache.
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../lib/common.sh"

echo "----- diagnose (gradle user home: ${GRADLE_USER_HOME:-<unset>}) -----"
echo "--- generated tree ---"
find "${WORK:-/nonexistent}/build/generated" -maxdepth 4 2>/dev/null || true
echo "--- dart-sass cache ---"
ls -la "${WORK:-/nonexistent}/build/tokia/dart-sass" 2>/dev/null || true
ls -la "${WORK:-/nonexistent}/build/tokia/dart-sass/dart-sass" 2>/dev/null || true
echo "-----------------------"
