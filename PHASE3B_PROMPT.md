# Phase 3b: Palette usable on the phone, starting funds (bug fix)

Follow CLAUDE.md. Two bugs from the first S22 run. For EACH: investigate and
report the cause before changing anything, then fix, then commit separately.
Never recursive deletes. Manual mode only.

Observed on the Galaxy S22 (1080x2340, about 425 dpi), portrait:
A. The tool palette strip at the bottom left shows the icons at their native
   16 px, roughly 1 mm on screen, and the "TOOLS" handle is a small dark box.
   The buttons cannot be hit with a finger.
B. The status bar shows "$0" on a brand-new city. A new city must start with
   the original's starting funds by game level (20000 easy, 10000 medium,
   5000 hard; MicropolisJ applies this in the GUI via setFunds when creating
   a city, not in the engine constructor). Default NietoCity to easy for now.

1. Report the cause of A and B (which code, why it happened, how the desktop
   is affected). No changes yet.
2. Fix A. Tool cells: 56 dp square minimum, icon scaled 3x with nearest
   neighbour (no filtering) so it stays crisp, tool name in a small label
   under the icon, cost shown on the selected cell. Portrait: the bottom sheet
   opens to a 4-column grid of all 16 tools with a drag handle and a visible
   "Tools" bar at least 48 dp tall that shows the selected tool's icon and
   name when collapsed. Landscape: a 72 dp wide scrollable left column with
   the same cells. Selected cell uses the _hi icon and a highlight border.
   Apply the same 3x icon scaling to the desktop palette. Never let the
   sheet cover the status bar.
3. Fix B in GameController (new city applies the level's starting funds via
   the engine's setFunds; expose a GameLevel setting with easy as default).
   Add a JUnit test: a new easy city reports funds 20000. Update the Phase 3
   game tests if they assumed 0.
4. Gate: gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug; quote
   the raw tail and the test count. Confirm NietoCity-debug.apk in the root
   has a fresh timestamp. Update CHANGES.md (top) and TASKS.md. Commit.

Report tersely: cause of A, cause of B, what changed, gate tail, and remind
me to push. Andre verifies on the phone: cells hit with a finger, icons
crisp, sheet opens and collapses, funds show $20,000 on a new city.
