# Phase 8 fix: tool selection desync and status bar wrapping (before release)

Follow CLAUDE.md. Never recursive deletes. Manual mode only. Commit each step
separately; I push via GitHub Desktop. Pin this list in TASKS.md. Release
tasks 4 to 8 stay ON HOLD until I have tested this fix on the S22.

This is the THIRD round on tools and touch (Phases 3, 3b, 4a). Per the standing
rules: STOP, investigate and report the actual cause BEFORE changing anything,
and ship the fix with a test proven RED first.

## What I saw on the S22 (Phase 8 polish build)
A. Tools are unreliable. Sometimes I pick a tool and touching the map just
   pans. Other times a tool will not switch off and the map only pans.
   Screenshot evidence: the open drawer shows POWERPLANT highlighted (yellow
   border, "R3 000") while the bottom bar directly above it still says "Pan".
   So the palette highlight and the bar/touch layer disagree about which tool
   is selected.
B. Top-left status bar values wrap to a second line: "Feb" over "1900" and
   "R20" over "000", since the pause/speed/menu buttons (Phase 4b) take width.

## Tasks (in order; commit after each)
0. Investigate A and B and REPORT the cause before changing code: where the
   selected tool is stored (palette view, GameController, bar, CityView touch
   handler), every path that changes it (palette tap, re-tap to deselect, X,
   one-shot auto-deselect, Back, drawer collapse, rotation/recreate, New City
   or Load swapping the controller), and which path leaves them out of sync.
   Check desktop too. After the report, continue to task 1.
1. Tests FIRST, proven red on the current code, then committed:
   (a) extract the touch decision as a PURE function, e.g. TouchRouter
       .decide(selectedTool, pointerCount, phase) -> BUILD / PAN / QUERY, and
       test it: tool armed + 1 finger = BUILD; any tool + 2 fingers = PAN; no
       tool + 1 finger = PAN; long press = QUERY;
   (b) a GameController selection test: select, re-select (deselects), X,
       one-shot after placement, and controller swap (New City/Load) all leave
       ONE consistent selected-tool value that listeners are notified of.
2. Fix A with ONE source of truth: the selected tool lives only in
   GameController; palette highlight, bottom bar (name, cost, X) and the map's
   touch routing all observe it through one listener and never keep their own
   copy. Rebind listeners whenever the controller is swapped (New City, Load,
   Continue, rotation). Same on desktop.
3. Clearer tool UX (my choice, both platforms):
   - picking a tool auto-collapses the drawer so the whole map is visible;
   - while a tool is armed, draw a thin coloured border around the map view
     (accent colour, not over the status bar), removed in Pan;
   - "Pan" shows in the bar ONLY when nothing is armed; otherwise tool icon,
     name, cost and a 48 dp X;
   - Back with a tool armed returns to Pan first (before the drawer/exit rules).
4. Fix B: every status-bar value stays on ONE line (maxLines 1, no wrapping):
   give the date, funds and population a flexible share of the width and keep
   the buttons fixed; if space is short, shrink the text to fit (autosize down
   to a readable minimum) rather than wrap or clip. Check portrait at 360 dp
   width and landscape. Desktop status bar the same.
5. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug; quote
   the raw tail and the engine test count; confirm the root APK is fresh.
   Update CHANGES.md and TASKS.md.

## Definition of done
CC verifies: the cause report, the tests proven red then green, builds.
Andre verifies on the S22 (never claim these): pick a tool, the drawer closes,
the border and bar show the tool, one finger builds every time; X or Back
returns to Pan and one finger then pans; two fingers always pan; date, funds
and population each sit on one line.

## When you stop
Terse report: the root cause of A, how the tests went red, what changed, the
gate tail, and remind me to push. Then wait; do NOT start release tasks 4 to 8
until I say go.
