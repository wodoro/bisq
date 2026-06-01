#!/usr/bin/env bash
# Diff JFX vs Bisq for one (component, state, theme).
# Outputs: <ae> <ae_max> <pass|fail>
# Writes diff PNG to .jfx-fixture/out/diff/<component>_<state>_<theme>.png
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")"
source "$HERE/_common.sh"
require_bin compare

[[ $# -eq 3 ]] || { echo "Usage: $0 <component> <state> <theme>"; exit 2; }
COMP="$1"; STATE="$2"; THEME="$3"
JFX="$(png_path "$COMP" "$STATE" "$THEME" jfx)"
BISQ="$(png_path "$COMP" "$STATE" "$THEME" bisq)"
DIFF_PNG="$(diff_path "$COMP" "$STATE" "$THEME")"

[[ -s "$JFX"  ]] || { echo "Missing: $JFX"; exit 3; }
[[ -s "$BISQ" ]] || { echo "Missing: $BISQ"; exit 3; }

read AE_MAX FUZZ_PCT <<<"$(read_threshold "$COMP")"

# compare prints AE on stderr, exits 1 if diff > 0 (we capture both).
AE=$(compare -metric AE -fuzz "${FUZZ_PCT}%" "$JFX" "$BISQ" "$DIFF_PNG" 2>&1 || true)
# Strip any non-numeric junk; if compare can't even read, treat as huge.
AE_NUM=$(echo "$AE" | grep -Eo '^[0-9]+(\.[0-9]+)?' || echo "999999")

STATUS=$([[ "${AE_NUM%.*}" -le "$AE_MAX" ]] && echo pass || echo fail)
echo "$AE_NUM $AE_MAX $STATUS"
