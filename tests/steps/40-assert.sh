#!/usr/bin/env bash
# Step 40 — assert the expected tokens were generated.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../lib/common.sh"
require_env WORK

GEN="$WORK/build/generated/tokia/ui/styles/GeneratedThemeTokens.kt"

[ -f "$GEN" ] || die "generated file not found: $GEN"
grep -q "val ControlHeight = 36.dp" "$GEN" || die "expected token 'ControlHeight = 36.dp' missing in $GEN"

log "asserted $(basename "$GEN") contains ControlHeight"
