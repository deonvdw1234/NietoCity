# Phase 6a: Map generator and New City screen (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review,
/workflows or multi-agent work. Investigate and report the cause before
changing anything when a build fails. Pin this list in TASKS.md, tick as you
go. Phase 5 is committed and tested. Apply every task to Android AND desktop
unless it says one platform. Money via CurrencyFormat (R).

Context: save/load is a separate later phase (Phase 6b, classic binary .cty
writer); the eight scenarios are NOT in our engine source and are deferred to
a later phase. This phase is only the map generator and the New City screen.

## Task list (in order; commit after each)
0. Engine generator API. MapGenerator has generateSomeCity(long seed) and the
   package-private terrain fields createIsland (enum NEVER/SELDOM/ALWAYS),
   lakeLevel, curveLevel (river curviness), treeLevel (each -1=auto, 0=none,
   >0=level). Add a minimal PUBLIC config API on MapGenerator to set these and
   then generate with a given seed, keep the GPL header, and record every
   added method in CHANGES.md ("Created by Nieto Software"). Do NOT change the
   generation algorithms, only expose settings the class already has.
1. GameController: add newGame(level, seed, terrainConfig) that generates with
   a chosen seed and remembers the seed and settings; keep the existing
   newGame(level) working (random seed). Expose the current city's seed so the
   UI can show it. JUnit: the SAME seed and settings produce an identical map
   (compare every tile); two different seeds differ. Reuse the shared-PRNG
   seeding fix from Phase 1.
2. New City screen (both platforms), reached from the overflow menu as
   "New City": difficulty Easy/Medium/Hard (ties to the starting funds already
   in GameLevel), terrain controls (Island: none/seldom/always; Lake, River,
   Trees each: auto/none/low/high mapped to the engine's -1/0/level), a seed
   field that shows the current seed and can be typed in to reproduce a map,
   and a Reroll button that picks a new random seed. Lay it out cleanly:
   phone portrait a scrollable form, landscape and desktop a two-column form.
3. Live preview: a mini-map thumbnail (reuse render.MiniMap / TileColors)
   that regenerates whenever the seed or a terrain control changes, so the
   player sees the map before committing. Generate the preview off the UI
   thread; never block the UI.
4. Start and discard guard: a Start button begins the new city. If a city is
   already running, ask first ("Start a new city? Your current city will be
   discarded.", Start / Cancel, Cancel default), since save/load does not
   exist yet and the running city is lost. On confirm, the controller swaps to
   the new city and the renderers reset the viewport to the map centre.
5. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from
   the command line; quote the raw tail and the engine test count from the
   XML report. Confirm NietoCity-debug.apk and NietoCity-desktop.jar in the
   project root carry fresh timestamps. No file preview.
6. Docs. TASKS.md final status; CHANGES.md at the top (the generator API);
   README (New City screen, terrain controls, seeds); CLAUDE.md one line that
   scenarios and save/load are later phases and why (no scenario data in the
   engine source; Android has no StAX so save/load will use classic binary
   .cty).

## Definition of done
CC verifies: builds, tests (map determinism by seed, generator API),
artefacts. Andre verifies on the S22 (never claim these): the New City screen
opens from the menu; terrain controls and reroll change the preview; typing a
seed reproduces the same map; Start replaces the city after the confirm; the
new city plays normally.

## When you stop
Terse summary: what was built, what surprised you, the raw gate tail, what to
test, and remind me to push. Note that Phase 6b (classic binary .cty save and
load with a golden round-trip test, and the named-slot list with thumbnails)
comes next.
