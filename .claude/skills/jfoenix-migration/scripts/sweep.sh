#!/usr/bin/env bash
# Full matrix sweep: render every (component, state, theme) for both sides in one JVM,
# diff each pair, write report.json.
# Filters: --component=NAME --state=NAME --theme=dark|light  (multiple OK; empty = all)
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")"
source "$HERE/_common.sh"
FILT_COMP=""
FILT_STATE=""
FILT_THEME=""
for arg in "$@"; do
    case "$arg" in
        --component=*) FILT_COMP="${arg#*=}";;
        --state=*)     FILT_STATE="${arg#*=}";;
        --theme=*)     FILT_THEME="${arg#*=}";;
        *) echo "Unknown arg: $arg"; exit 2;;
    esac
done

# Component → list of states + canonical size.
# (component  states  width  height)
ROWS=(
    "button           default,hover,focus,press,disabled                 200 80"
    "text-field       default,focus,error,readonly,disabled              260 80"
    "text-area        default,focus,disabled                             260 120"
    "password-field   default,focus,error                                260 80"
    "check-box        default,selected,hover,focus,disabled              160 60"
    "radio-button     default,selected,hover,focus,disabled              160 60"
    "toggle-button    default,selected,hover,disabled                    160 60"
    "progress-bar     default                                            260 60"
    "spinner          default                                            80 80"
)

TSV="$OUT/batch.tsv"
: > "$TSV"

for row in "${ROWS[@]}"; do
    set -- $row
    COMP="$1"; STATES="$2"; W="$3"; H="$4"
    [[ -n "$FILT_COMP" && "$COMP" != "$FILT_COMP" ]] && continue
    IFS=',' read -ra ST_LIST <<<"$STATES"
    for STATE in "${ST_LIST[@]}"; do
        [[ -n "$FILT_STATE" && "$STATE" != "$FILT_STATE" ]] && continue
        for THEME in dark light; do
            [[ -n "$FILT_THEME" && "$THEME" != "$FILT_THEME" ]] && continue
            for SIDE in jfx bisq; do
                OUTPNG="$(png_path "$COMP" "$STATE" "$THEME" "$SIDE")"
                printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
                    "$COMP" "$SIDE" "$STATE" "$THEME" "$W" "$H" "$OUTPNG" >>"$TSV"
            done
        done
    done
done

JOB_COUNT=$(wc -l <"$TSV")
echo "Rendering $JOB_COUNT tuples in one JVM..."
: > "$LOG"
run_fixture "--batch=$TSV" >>"$LOG" 2>&1

REPORT="$REPO/.jfx-fixture/report.json"
python3 - "$TSV" "$REPORT" "$HERE/diff.sh" <<'PY'
import json, os, subprocess, sys
tsv, report_path, diff_sh = sys.argv[1], sys.argv[2], sys.argv[3]
pairs = {}
for line in open(tsv):
    line = line.rstrip("\n")
    if not line or line.startswith("#"): continue
    c, side, state, theme, *_ = line.split("\t")
    pairs.setdefault((c, state, theme), set()).add(side)

results = []
for (c, state, theme), sides in sorted(pairs.items()):
    if sides != {"jfx", "bisq"}: continue
    out = subprocess.run([diff_sh, c, state, theme],
                         capture_output=True, text=True)
    line = out.stdout.strip().splitlines()[-1] if out.stdout else ""
    parts = line.split()
    if len(parts) == 3:
        ae, ae_max, status = parts
        results.append({"component": c, "state": state, "theme": theme,
                        "ae": float(ae), "ae_max": int(ae_max),
                        "status": status})
    else:
        results.append({"component": c, "state": state, "theme": theme,
                        "ae": None, "ae_max": None, "status": "error",
                        "stderr": out.stderr.strip()})

passed = sum(1 for r in results if r["status"] == "pass")
report = {"summary": {"total": len(results), "passed": passed,
                      "failed": len(results) - passed},
          "results": results}
json.dump(report, open(report_path, "w"), indent=2)
print(f"Wrote {report_path}: {passed}/{len(results)} passed")
PY
