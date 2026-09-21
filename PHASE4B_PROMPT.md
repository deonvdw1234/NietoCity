# Phase 4b: Speed control, mini map with overlays, and the classic dialogs (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review,
/workflows or multi-agent work. Investigate and report the cause before
changing anything when a build fails. Pin this list in TASKS.md, tick as you
go. Phase 4a is committed and tested on the S22. Apply every task to Android
AND desktop unless it says one platform. Money always via CurrencyFormat (R).

## Task list (in order; commit after each)
0. Speed control. Drive GameController's clock from the engine Speed enum
   (PAUSED, SLOW, NORMAL, FAST, SUPER_FAST; use its animationDelay and
   simStepsPerUpdate, do not invent timings). Status bar gets a pause/play
   toggle and a speed label that tap-cycles SLOW->NORMAL->FAST->SUPER_FAST
   (pause is separate, so pausing then playing returns to the chosen speed).
   Desktop: same control plus keys (space=pause, 1..4=speeds). Persist the
   chosen speed in memory for the session. JUnit: setting a speed changes the
   controller's tick interval to the enum value; PAUSED stops ticks.
1. Engine overlay accessors. Several maps the overlays need are package-
   private (powerMap, landValueMem, trfDensity, fireStMap, policeMap). Add
   small READ-ONLY accessors on Micropolis returning copies or values by
   coordinate, keep the GPL header, and record every added method in
   CHANGES.md (most recent first, "Created by Nieto Software"). Do not change
   engine logic. The already-public maps (pollutionMem, crimeMem, popDensity,
   fireRate, policeMapEffect, rateOGMem) are used directly.
2. Mini map panel (shared render core builds the overview bitmap: whole map,
   one pixel per tile, from the tile colours). Draws the current viewport as
   a rectangle; tapping or dragging on it re-centres the main view. Phone:
   a show/hide overview in a corner, toggled from the status bar, never over
   the top bar. Desktop: a docked side panel. JUnit: a tap at overview (x,y)
   maps to the matching map tile within one tile.
3. Overlay layer. An overlay picker with: None, Population density, Pollution,
   Crime, Land value, Traffic, Power grid, Fire coverage, Police coverage.
   The chosen overlay tints the MAIN map (translucent, like the original's
   colour ramp) and the mini map. Power grid shows powered vs unpowered.
   Picker lives in the same menu as the mini map toggle. Off by default.
4. Budget dialog (engine CityBudget / BudgetNumbers). Tax-rate slider
   (0..20), road/fire/police funding percent sliders, and read-outs for tax
   revenue, expenses and cash flow, all in R. Apply writes back to the
   engine. The original shows this automatically once a year: show it on the
   annual tick with a "don't show automatically" checkbox (persisted); always
   reachable from a menu button. JUnit: setting tax rate and percents updates
   the engine's budget fields.
5. Evaluation dialog (engine CityEval): approval cityYes/cityNo, city score
   and delta, population and delta, city class, and the top four problems
   from problemOrder with their votes. Read-only, menu button, closes on
   tap/Esc.
6. Graphs dialog (engine History): line graphs for residential, commercial,
   industrial, crime, pollution and money, with a 10-year / 120-year toggle,
   drawn natively (no chart library). Legend with the series colours.
7. Menu. Add a small overflow menu (phone: a button in the status bar;
   desktop: a top menu or buttons) holding Budget, Evaluation, Graphs, the
   mini map toggle and the overlay picker, so the status bar stays uncluttered.
8. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from
   the command line; quote the raw tail and the engine test count from the
   XML report. Confirm NietoCity-debug.apk and NietoCity-desktop.jar in the
   project root carry fresh timestamps. No file preview.
9. Docs. TASKS.md final status; CHANGES.md at the top; README controls
   (speed, mini map, overlays, dialogs, menu); CLAUDE.md one line noting the
   engine accessors were added for overlays.

## Definition of done
CC verifies: builds, tests (speed, overview mapping, budget write-back),
artefacts. Andre verifies on the S22 (never claim these): speed changes the
sim pace and pause stops it; the mini map shows the city and re-centres on
tap; each overlay tints sensibly; the budget dialog changes funding and the
figures are in R; evaluation and graphs read correctly; the menu is tidy.

## When you stop
Terse summary: what was built, what surprised you, the raw gate tail, what to
test on the phone and PC, what Phase 5 (disasters, sprites, sound) needs from
me, and remind me to push.
