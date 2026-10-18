#!/usr/bin/env bash
# Step 10 — install the Gradle wrapper into the test project, pinned to $TOKIA_TEST_GRADLE.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../lib/common.sh"
require_env WORK REPO_ROOT TOKIA_TEST_GRADLE

mkdir -p "$WORK/gradle/wrapper"
cp "$REPO_ROOT/gradlew" "$WORK/gradlew"
chmod +x "$WORK/gradlew"
cp "$REPO_ROOT/gradle/wrapper/gradle-wrapper.jar" "$WORK/gradle/wrapper/gradle-wrapper.jar"

render "$HERE/../fixtures/consumer/gradle/wrapper/gradle-wrapper.properties.tmpl" \
       "$WORK/gradle/wrapper/gradle-wrapper.properties"

log "wrapper pinned to Gradle $TOKIA_TEST_GRADLE"
