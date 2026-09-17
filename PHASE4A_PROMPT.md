# Phase 4a: Play feedback from the first real city (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review,
/workflows or multi-agent work. Investigate and report the cause before
changing anything for task 0. Pin this list in TASKS.md and tick as you go.
Phase 3b is committed and was tested on the S22: a city with R, C, I zones,
coal plant, stadium, police and fire grew to Pop 300, so the engine, renderer,
palette and placement all work on the phone. Tasks 1 to 5 are Andre's play
feedback; apply each to Android AND desktop unless it says phone only.

## Task list (in order; commit after each)
0. Road join bug. Andre reports that two roads placed in SEPARATE strokes
   sometimes do not join, although junctions form fine inside one stroke and
   in most places. Investigate first and report the cause (candidates: the
   neighbour fix-up after a stroke only covering the stroke's own bounds,
   diagonal adjacency, bridges over water, the touch-to-tile mapping on the
   first MOVE event). Write a JUnit test that places two single road tiles
   orthogonally adjacent in two separate strokes and asserts the tiles become
   connected road (TileConstants road connectivity); prove it red on the
   current code if the cause is in our layer, then fix. If the cause is in
   the original engine behaviour, say so and stop; do not "fix" the engine.
1. Power indicator (faithful to the original): unpowered zone centres blink
   a yellow lightning bolt (the LIGHTNINGBOLT tile, as MicropolisJ's drawing
   area does, toggling about every 0.5 s while the sim runs). Add it to the
   shared render core so both renderers get it.
2. Currency: all money figures render as South African rand, "R" prefix, no
   space, locale grouping (R8 833 on Andre's phone). One CurrencyFormat
   helper in :engine used by the status bar, palette costs, ticker messages
   and any dialog; never a "$" from our code. Engine message strings from the
   bundles keep their wording; only figures we format change. JUnit for the
   formatter.
3. Tool guard. Tools are of two kinds: STROKE tools (bulldozer, road, rail,
   wire, park, residential, commercial, industrial) stay selected until
   cleared; ONE-SHOT tools (police, fire, stadium, seaport, airport, coal,
   nuclear) return to Pan after one successful placement. The bottom bar
   (phone) and the status area (desktop) always show the selected tool's
   icon, name and cost with a large X (at least 48 dp) that returns to Pan;
   "Pan" shows only when nothing is selected. Two-finger drag always pans.
   Query stays a long press. JUnit for the one-shot rule in GameController.
4. Android back button: closes, in order, the query dialog, then the open
   tools drawer; when nothing is open, three presses within 2 s show a
   confirm dialog "Exit NietoCity?" (Exit / Stay, Stay is the default);
   one or two presses show a short toast "Press back 3 times to exit". Never
   exit without the confirmation. (The exit splash comes in Phase 7; leave a
   single exit point for it.)
5. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from
   the command line; quote the raw tail and the engine test count from the
   XML report. Confirm NietoCity-debug.apk and NietoCity-desktop.jar in the
   project root carry fresh timestamps. No file preview.
6. Docs. TASKS.md final status; CHANGES.md entry at the top; README controls
   updated (tool kinds, X, back button); CLAUDE.md gains one line each for
   the currency rule and the tool-kind rule.

## Definition of done
CC verifies: builds, tests (including the road-join and one-shot tests),
artefacts. Andre verifies on the S22 (never claim these): bolt blinks on an
unpowered zone and stops when wired; figures show R; a stadium deselects
after placing, a road does not; the X returns to Pan; back closes the drawer
first and three backs ask before exiting; two roads placed separately join.

## When you stop
Terse summary: cause of the road issue, what was built, what surprised you,
the raw gate tail, what to test, and remind me to push. Phase 4b (speed
control in the status bar, budget, evaluation, graphs, mini map with
overlays) follows once Andre has tested this.
