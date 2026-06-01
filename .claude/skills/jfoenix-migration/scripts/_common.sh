#!/usr/bin/env bash
# Sourced by other scripts.
set -euo pipefail
REPO="$(git rev-parse --show-toplevel)"
OUT="$REPO/.jfx-fixture/out"
DIFF="$OUT/diff"
LOG="$REPO/.jfx-fixture/fixture.log"
THRESH="$REPO/.claude/skills/jfoenix-migration/thresholds.json"
GRADLEW="$REPO/gradlew"
mkdir -p "$OUT" "$DIFF"

require_bin() {
    command -v "$1" >/dev/null 2>&1 || { echo "Missing: $1"; exit 1; }
}

# Run the fixture via Gradle (it sets up the JavaFX module path correctly).
# Passes any number of --key=value flags directly to BisqJfxFixtureApp.
run_fixture() {
    cd "$REPO"
    local args=""
    for a in "$@"; do
        # Escape single quotes for --args
        args+="'$a' "
    done
    "$GRADLEW" :desktop-jfx-fixture:run --quiet --args="$args"
}

# Reads (ae_max, fuzz_pct) for a component from thresholds.json.
# Output: "$AE_MAX $FUZZ_PCT"
read_threshold() {
    local comp="$1"
    require_bin python3
    python3 - "$THRESH" "$comp" <<'PY'
import json, sys
t = json.load(open(sys.argv[1]))
c = t.get("components", {}).get(sys.argv[2]) or t["default"]
print(f"{c['ae_max']} {c['fuzz_pct']}")
PY
}

png_path() { echo "$OUT/$1_$2_$3_$4.png"; }
diff_path() { echo "$DIFF/$1_$2_$3.png"; }
