#!/usr/bin/env bash
# Step 30 — run the happy path: generateScssTokens.
# TOKIA_TEST_DEBUG=bootstrap runs only the sass bootstrap and prints the tree (debugging).
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../lib/common.sh"
require_env WORK

cd "$WORK"

if [ "${TOKIA_TEST_DEBUG:-}" = "bootstrap" ]; then
  ./gradlew bootstrapTokiaSass --console=plain --no-daemon || true
  echo "==== build tree ===="
  find "$WORK/build" -maxdepth 6 2>/dev/null || true
  exit 0
fi

./gradlew generateScssTokens --console=plain --no-daemon
