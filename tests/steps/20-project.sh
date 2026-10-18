#!/usr/bin/env bash
# Step 20 — scaffold the throwaway consumer project from tests/fixtures/consumer.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../lib/common.sh"
require_env WORK TOKIA_TEST_KOTLIN TOKIA_TEST_PLUGIN_VERSION

FIX="$HERE/../fixtures/consumer"

mkdir -p "$WORK/src/commonMain/kotlin/ui/styles"
cp "$FIX/settings.gradle.kts" "$WORK/settings.gradle.kts"
render "$FIX/build.gradle.kts.tmpl" "$WORK/build.gradle.kts"
cp -r "$FIX/src/." "$WORK/src/"

log "project scaffolded (kotlin=$TOKIA_TEST_KOTLIN, plugin=$TOKIA_TEST_PLUGIN_VERSION)"
