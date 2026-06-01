#!/usr/bin/env bash
# Build 2x2 side-by-side comparison images per (component, state).
# Layout:
#   JFX light | Bisq light
#   ---------+----------
#   JFX dark  | Bisq dark
#
# Output: .jfx-fixture/out/montage/<component>_<state>.png
# Run after sweep.sh.
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")"
source "$HERE/_common.sh"
require_bin montage
require_bin convert

MONTAGE_DIR="$OUT/montage"
mkdir -p "$MONTAGE_DIR"

# Scan existing renders and group by (component, state).
declare -A SEEN
for f in "$OUT"/*.png; do
    [[ -f "$f" ]] || continue
    base="$(basename "$f" .png)"
    # base = component_state_theme_side
    # use last 2 fields as theme + side; everything before = component_state (component may have dashes too)
    side="${base##*_}"
    rest="${base%_*}"
    theme="${rest##*_}"
    cs="${rest%_*}"  # component_state
    SEEN[$cs]=1
done

count=0
for cs in "${!SEEN[@]}"; do
    JFX_L="$OUT/${cs}_light_jfx.png"
    BISQ_L="$OUT/${cs}_light_bisq.png"
    JFX_D="$OUT/${cs}_dark_jfx.png"
    BISQ_D="$OUT/${cs}_dark_bisq.png"

    # Skip if not all 4 sides exist.
    [[ -s "$JFX_L" && -s "$BISQ_L" && -s "$JFX_D" && -s "$BISQ_D" ]] || continue

    OUT_IMG="$MONTAGE_DIR/${cs}.png"

    # Label each tile + arrange 2x2 with dividers + title bar.
    montage \
        -label 'jfoenix (light)' "$JFX_L" \
        -label 'bisq (light)'    "$BISQ_L" \
        -label 'jfoenix (dark)'  "$JFX_D" \
        -label 'bisq (dark)'     "$BISQ_D" \
        -tile 2x2 -geometry +6+6 \
        -background '#cccccc' \
        -title "$cs" \
        "$OUT_IMG"
    count=$((count + 1))
done

echo "Wrote $count montages to $MONTAGE_DIR/"
ls "$MONTAGE_DIR" | sort
