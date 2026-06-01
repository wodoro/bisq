---
name: jfoenix-migration
description: Pixel-diff harness for migrating Bisq desktop off jfoenix to pure JavaFX. Launches `desktop-jfx-fixture` in JavaFX headless mode (Monocle Headless + software Prism — no X server, no display, in-JVM PNG snapshots), batch-renders every (component, state, theme, side) combo in a single JVM, diffs JFX baseline vs Bisq replacement with ImageMagick `compare -metric AE` against per-component thresholds, writes a pass/fail matrix. Loop body: edit `bisq-controls.css` / `Bisq*` skin → rerun → assert AE under threshold. Use when working on jfoenix removal. Trigger: `/jfoenix-migration`, "diff component X", "loop until button matches".
---

# jfoenix-migration harness

Drives `desktop-jfx-fixture` in JavaFX headless mode to capture per-component PNGs and verify the pure-JavaFX replacement matches the jfoenix baseline.

## Why Monocle Headless (not Xvfb)

- Diff is **Bisq-side vs JFX-side via the same toolkit** — pixel target is "match jfoenix's render", not "match a user's GPU".
- Both sides run under same SW Prism + same fonts → diff is pure CSS/skin code, never toolkit drift.
- One JVM batches N tuples → no per-snapshot process spawn / X round-trip. Roughly 10× faster than Xvfb+import.
- No window manager, no compositor, no clock-dependent rendering.

Animations (ripple, focus pulse) are time-dependent; we snapshot the deliberate steady-state via pseudo-classes, not mid-transition. If a state can't be expressed via pseudo-class alone, extend `BisqJfxFixtureApp.applyState`.

## Hard rules

1. **Always headless.** `-Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.order=sw -Dprism.lcdtext=false -Dprism.text=t2k`. Set in fixture build.gradle.
2. **Batch when possible.** Use `--batch=<file.tsv>` to render many tuples in one JVM. Per-snapshot JVM spawn is wasteful.
3. **PNG naming canonical.** `out/<component>_<state>_<theme>_<side>.png`. Diff at `out/diff/<component>_<state>_<theme>.png`.
4. **Diff metric: AE.** `compare -metric AE -fuzz <pct>%`. Per-component threshold from `thresholds.json`. Inspect diff PNG before relaxing thresholds.
5. **Both sides identical container.** Same StackPane, same dimensions, same background. Pixel diff = component's own rendering, nothing else.

## Where state lives

`<repo-root>/.jfx-fixture/` — gitignored.
  - `out/<component>_<state>_<theme>_<side>.png`
  - `out/diff/<component>_<state>_<theme>.png`
  - `out/batch.tsv` (auto-generated job list)
  - `report.json` — pass/fail matrix
  - `fixture.log` — stdout/stderr from the JVM

## System dependencies

Fedora:
```bash
sudo dnf install ImageMagick
```
Only need `compare` + `identify`. **No Xvfb, xdotool, scrot.**

## Scripts (`.claude/skills/jfoenix-migration/scripts/`)

| Script | Purpose |
|---|---|
| `build.sh` | `./gradlew :desktop-jfx-fixture:installDist -q`. Idempotent. |
| `render.sh <component> <state> <theme> <side>` | Render a single tuple. Convenience for debugging — for full sweeps use `sweep.sh`. |
| `batch.sh <tsv>` | Run fixture w/ `--batch=<tsv>`. TSV cols: `component\tside\tstate\ttheme\twidth\theight\toutPath`. |
| `diff.sh <component> <state> <theme>` | `compare` JFX vs Bisq for one tuple. Writes diff PNG, returns AE on stdout. |
| `sweep.sh [--component=NAME] [--state=NAME] [--theme=dark\|light]` | Build TSV from filters, render in one JVM, diff each pair, write `report.json`. |
| `loop.sh <component> [<state>] [<theme>]` | Build + render + diff a single tuple. Exit non-zero if over threshold. Wrap in `while ! ...; do edit-css; done`. |

All scripts run from any subdir (each does `cd "$(git rev-parse --show-toplevel)"`).

## thresholds.json

See file in this skill directory. Tighten as migration progresses.

## States covered

| State | Effect in fixture |
|---|---|
| `default` | nothing |
| `hover` | `:hover` pseudo-class |
| `focus` | `requestFocus()` + `:focused` |
| `press` | `:armed` + `:pressed` |
| `disabled` | `setDisable(true)` |
| `selected` | `:selected` |
| `error` | `:error` |
| `readonly` | `setEditable(false)` + `:readonly` |

Add new states in `BisqJfxFixtureApp.applyState`.

## Standard workflow

```bash
SK=.claude/skills/jfoenix-migration/scripts

# Build once (or after Java changes in the fixture).
"$SK/build.sh"

# Single-component iterate-on-CSS loop:
until "$SK/loop.sh" button hover dark; do
  # diff PNG: .jfx-fixture/out/diff/button_hover_dark.png
  # edit:     desktop/src/main/java/bisq/desktop/bisq-controls.css
  echo "retrying after edit..."
done

# Full sweep:
"$SK/sweep.sh"
# inspect .jfx-fixture/report.json
```

For Claude: when AE > 0, `Read` the diff PNG (multimodal vision applies). Localize the delta, propose CSS edit, rerun.

## Determinism notes

- Same JVM, same JavaFX, same fonts both sides.
- Pseudo-class states applied programmatically — no real input events, no clock dependence.
- Two `Platform.runLater` pulses + explicit `applyCss()` + `layout()` before each snapshot.
- `prism.lcdtext=false` + `prism.text=t2k` → consistent text rasterization across machines.
- Each tuple gets a fresh Stage+Scene to prevent state bleed.

## Failure modes

| Symptom | Look at |
|---|---|
| `installDist` fails | `./gradlew :desktop-jfx-fixture:installDist --stacktrace` |
| `Toolkit not found` | Missing JVM flags — verify `applicationDefaultJvmArgs` in fixture build.gradle |
| Black PNG | Prism HW path picked — verify `-Dprism.order=sw` in fixture build.gradle |
| AE huge on text states | Font hinting — bump `fuzz_pct` before `ae_max` |
| AE small but visible drift | Tighten threshold, inspect diff PNG |
| Same PNG both sides | `bisq-controls.css` not on classpath — check `sourceSets.main.resources.srcDirs` in desktop module |
| Animation state (ripple) flaky | Don't snapshot mid-animation. Snapshot steady-state (pseudo-class only). |

## Don't

- Don't reach for Xvfb. Headless Monocle is faster and equally accurate for this task.
- Don't relax thresholds without naming the cause in `report.json` / commit message.
- Don't leave `JFXButton`-style imports in prod code after a component's bisq side ships (Phase 4 cleanup).
- Don't rely on `sleep` between actions — use the two-pulse barrier already in the fixture.

## Output contract per session

End with:

1. PNGs under `.jfx-fixture/out/` (both sides + diff)
2. `report.json` with pass/fail per (component, state, theme)
3. Prose summary of remaining deltas + next CSS edit
