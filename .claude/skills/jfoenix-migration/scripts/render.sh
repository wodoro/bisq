#!/usr/bin/env bash
# Render a single (component, state, theme, side) tuple via Gradle run.
# Usage: render.sh <component> <state> <theme> <side>
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")"
source "$HERE/_common.sh"

[[ $# -eq 4 ]] || { echo "Usage: $0 <component> <state> <theme> <side>"; exit 2; }
COMP="$1"; STATE="$2"; THEME="$3"; SIDE="$4"
OUT_PNG="$(png_path "$COMP" "$STATE" "$THEME" "$SIDE")"

run_fixture \
    "--component=$COMP" \
    "--state=$STATE" \
    "--theme=$THEME" \
    "--side=$SIDE" \
    "--out=$OUT_PNG" \
    >>"$LOG" 2>&1

[[ -s "$OUT_PNG" ]] || { echo "Empty PNG: $OUT_PNG (see $LOG)"; exit 3; }
echo "$OUT_PNG"
