#!/usr/bin/env bash
# Shared helpers for the Tokia test harness that are sourced by the orchestrator and every step.
# Deliberately no `set -e` here: each entry script sets its own shell options.

log() { echo "   $*"; }
die() { echo "FAIL: $*" >&2; exit 1; }

# require_env NAME [NAME...] — fail if any listed variable is unset/empty.
require_env() {
  local v
  for v in "$@"; do
    [ -n "${!v:-}" ] || die "required environment variable '$v' is not set"
  done
}

# render <template> <output> — substitute @TOKIA_TEST_* placeholders (no gettext needed).
render() {
  local tmpl="$1" out="$2" line
  : > "$out"
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line//@TOKIA_TEST_KOTLIN@/${TOKIA_TEST_KOTLIN:-}}"
    line="${line//@TOKIA_TEST_PLUGIN_VERSION@/${TOKIA_TEST_PLUGIN_VERSION:-}}"
    line="${line//@TOKIA_TEST_GRADLE@/${TOKIA_TEST_GRADLE:-}}"
    printf '%s\n' "$line" >> "$out"
  done < "$tmpl"
}
