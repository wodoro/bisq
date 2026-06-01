#!/usr/bin/env bash
# Run the fixture once with a TSV of jobs — all in a single JVM.
# Usage: batch.sh <tsv-path>
set -euo pipefail
HERE="$(dirname "$(readlink -f "$0")")"
source "$HERE/_common.sh"

[[ $# -eq 1 ]] || { echo "Usage: $0 <tsv-path>"; exit 2; }
TSV="$1"
[[ -s "$TSV" ]] || { echo "Missing TSV: $TSV"; exit 2; }

run_fixture "--batch=$TSV" >>"$LOG" 2>&1
echo "Batch done ($(grep -c '^OK ' "$LOG" || true) ok; see $LOG)"
