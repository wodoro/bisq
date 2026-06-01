#!/usr/bin/env bash
# Compile the fixture (and its deps). Idempotent; safe to call every iteration.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
./gradlew :desktop-jfx-fixture:classes -q "$@"
echo "Fixture classes ready."
