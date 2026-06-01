#!/usr/bin/env bash
# Single iteration: build, render both sides via batch (one JVM), diff.
# Exit 0 if pass, non-zero if fail. Wrap: until loop.sh ...; do edit-css; done
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")"
source "$HERE/_common.sh"

COMP="${1:?Usage: $0 <component> [<state>] [<theme>]}"
STATE="${2:-default}"
THEME="${3:-dark}"

"$HERE/build.sh"

W=260; H=80
case "$COMP" in
    spinner)        W=80;  H=80;;
    progress-bar)   W=260; H=60;;
    check-box|radio-button|toggle-button) W=160; H=60;;
    text-area)      W=260; H=120;;
    button)         W=200; H=80;;
    tab-pane)       W=560; H=100;;
esac

TMP_TSV="$OUT/_loop.tsv"
{
    printf "%s\tjfx\t%s\t%s\t%s\t%s\t%s\n"  "$COMP" "$STATE" "$THEME" "$W" "$H" "$(png_path "$COMP" "$STATE" "$THEME" jfx)"
    printf "%s\tbisq\t%s\t%s\t%s\t%s\t%s\n" "$COMP" "$STATE" "$THEME" "$W" "$H" "$(png_path "$COMP" "$STATE" "$THEME" bisq)"
} > "$TMP_TSV"
: > "$LOG"
run_fixture "--batch=$TMP_TSV" >>"$LOG" 2>&1

read AE AE_MAX STATUS <<<"$("$HERE/diff.sh" "$COMP" "$STATE" "$THEME")"
echo "$COMP $STATE $THEME: AE=$AE (max=$AE_MAX) → $STATUS"
echo "  diff:  $(diff_path "$COMP" "$STATE" "$THEME")"
echo "  jfx:   $(png_path "$COMP" "$STATE" "$THEME" jfx)"
echo "  bisq:  $(png_path "$COMP" "$STATE" "$THEME" bisq)"

[[ "$STATUS" == pass ]]
