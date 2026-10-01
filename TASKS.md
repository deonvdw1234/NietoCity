# TASKS.md (NietoCity)
Created by Nieto Software

## Phase 1: Scaffold and engine import
- [x] 0. Toolchain proof (JDK 25, Gradle 9.5.1, AGP 8.13.0 — recorded in CLAUDE.md)
- [x] 1. Android project scaffold (za.co.nieto.nietocity, minSdk 26, targetSdk 34, Kotlin DSL); build output redirected to C:\NietoCity-build, no build\ in repo
- [x] 2. :engine module with MicropolisJ engine package and resources, GPL headers kept (commit 9f6ddb4; +micropolisj.XML_Helper, a required engine compile dependency)
- [x] 3. :engine compiles as Java 8 (javap: major version 52), no Android/java.awt deps; no shim needed (CHANGES.md)
- [x] 4. JUnit smoke test (seeded map, res zone + road + coal plant + power line, 200 ticks) passes: pop 0->20, funds changed. JUnit XML: tests=1, failures=0, errors=0
- [x] 5. MainActivity builds the demo city, runs 100 ticks on a background thread, shows "Engine OK: population 20, funds 996845"; tiles.rc bundled in APK (verify on S22)
- [x] 6. :desktop module (Java 8, major version 52) runs the same 100 ticks; runnable fat JAR prints "Engine OK: population 20, funds 996845" (java -jar desktop.jar)
- [x] 7. Gate green: gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug BUILD SUCCESSFUL (41 tasks). APK + JAR paths reported in README/summary. Fixed an implicit-dependency error the gate surfaced in :desktop:jar
- [x] 8. TASKS.md finalised; README.md "How to build" added (APK: assembleDebug/installDebug; JAR: java -jar); CLAUDE.md project notes added

## Phase 2: Tile renderer (Android and desktop)
- [x] 0. JavaFX 8 toolchain: Liberica JDK 8 Full 1.8.0_482 (jfxrt.jar present); :desktop uses BellSoft lang-8 Gradle toolchain, auto-download off; recorded in CLAUDE.md
- [x] 1. Composed 16x16 atlas via MakeTiles (headless): engine/src/main/resources/16x16/tiles.png (16x15056) + tiles.idx (786 named tiles, all integer names matching tiles.rc; 174 numbers are animation-reserved, no art, as upstream); scripts/compose-tiles.cmd tracked
- [x] 2. Render core in :engine (za.co.nieto.nietocity.render): TileIndex (regex parse, no StAX - Android lacks javax.xml.stream), Viewport, AnimationClock; 11 render tests pass (12 engine tests total)
- [x] 3. Android renderer: CityView SurfaceView + render thread, atlas from classpath, nearest-neighbour integer zoom (default 3x), redraw on MapListener/animation clock, engine on own thread; fullscreen + rotation without restart (verify on S22)
- [x] 4. Touch: drag pans, pinch steps zoom 1x/2x/3x, double-tap centres; MainActivity generates random map, CityView full screen + overlay "NietoCity Phase 2: pop X, funds Y" (redraws each animation tick)
- [x] 5. Desktop renderer (JavaFX 8): Stage+Canvas, shared core+atlas, per-tile nearest-neighbour cache (crisp); mouse drag pan, wheel zoom, arrow keys; title mirrors overlay; engine own thread + Platform.runLater; runnable jar (no jfxrt bundled). Ran 10s: clean console, no exceptions, renders (verify visually)
- [x] 6. Gate green: BUILD SUCCESSFUL (41 tasks). Engine tests: 12 total (1 smoke + 4 TileIndex + 4 Viewport + 3 AnimationClock), 0 failures/errors. APK + JAR paths/timestamps in summary
- [x] 7. Docs: TASKS.md final; README updated (JavaFX 8 desktop runtime req, e.g. Liberica JRE 8 Full; compose-tiles script); CHANGES.md Phase 2 entry; THIRD_PARTY.md tile-art origin (open-source Micropolis via MicropolisJ, GPLv3, no Maxis art)

## Phase 3: Tool palette, placement and status bar
- [x] 0. Shared game layer in :engine (za.co.nieto.nietocity.game): GameController (owns engine thread via AnimationClock task queue, selected tool, apply on engine thread), StatusSnapshot, GameStrings (English bundles copied to engine resources); JUnit 4 pass (road=10, funds0->INSUFFICIENT_FUNDS+unchanged, res-on-water->UH_OH, date cityTime 0/48). 16 engine tests total
- [x] 1. Copied 32 tool icons (16 tools x plain/_hi) to engine/src/main/resources/tools/<TOOLNAME>.png; THIRD_PARTY.md updated
- [x] 2. Android status bar (top: date, funds, pop, tool, cost) bound to StatusSnapshot ~1/s + message ticker (~4s); CityView renders via GameController (retained across rotation); Phase 2 overlay retired
- [x] 3. Android tool palette: 16 tools (classic order) with icons; selected shows _hi; landscape left scroll column, portrait bottom sheet with collapse toggle; tap selected again deselects; status bar reflects tool immediately
- [x] 4. Android placement: tap places, drag draws stroke with translucent ToolPreview (green ok / red bad) applied on release; two-finger pan; pinch zoom; no tool = one-finger pan; INSUFFICIENT_FUNDS/UH_OH -> ticker; long press = query (zone name in ticker for now)
- [x] 5. Query: shared QueryReport (header + zone/density/value/crime/pollution/growth from Status/GuiStrings); Android shows it as a dismissable dialog on long press. Desktop query wired with desktop UI in task 6
- [x] 6. Desktop (JavaFX 8): top status bar + ticker, left palette column, same GameController; left-drag place/stroke (with preview), right/Space+drag pan, wheel zoom, arrow keys, right-click query; runnable jar (no jfxrt). Ran 10s: clean console, no exceptions
- [x] 7. Gate green: BUILD SUCCESSFUL (41 tasks). Engine tests: 16 total (1 smoke + 4 game + 3 AnimationClock + 4 TileIndex + 4 Viewport), 0 failures/errors. APK + JAR paths/timestamps in summary
- [x] 8. Docs: TASKS.md final; CHANGES.md Phase 3 entry; README Controls section (phone + PC); THIRD_PARTY.md strings note (icons already noted)

## Phase 3b: S22 fixes
- [x] A. Palette rebuilt as finger-sized grid cells (>=56dp, 3x nearest-neighbour icons, name + cost, highlight border); portrait bottom sheet (4-col grid + Tools bar), landscape 72dp left column; desktop icons 3x. Never covers the status bar.
- [x] B. New cities start with the level's funds (easy 20000 default) via GameController.newGame(); JUnit added (game tests now 5). Gate: 17 engine tests, 0 fail/err.

## Phase 4a: Play feedback from the first real city
- [x] 0. Road join. CAUSE: the engine already joins orthogonally-adjacent roads
      in separate strokes (its fixZone fixes each laid tile AND its 4 neighbours,
      reaching into the previous stroke); proven green by RoadJoinTest (H->66/66,
      V->67/67). The engine is NOT modified. The real gap came from our layer
      applying a drag as ONE axis-snapped stroke, so a bent drag ended away from
      the finger. FIX (per Andre, option 1): GameController now keeps the finger's
      tile waypoints and lays the road along them as axis-aligned engine strokes on
      release (previewPath/applyPath); the engine charges each tile once (re-laying
      on an existing road is a no-op). JUnit: L-drag 40,40->43,40->43,42 lays 6
      connected tiles for 60; gap-proof documents the old single-stroke behaviour.
      Both renderers (Android + desktop) record the path and use previewPath/applyPath.
- [x] 1. Power indicator: shared render.PowerOverlay blinks the LIGHTNINGBOLT (tile 827)
      over unpowered zone centres. Blink is driven from the engine animation cycle
      (4 on / 4 off, ~0.5s at NORMAL) so it holds steady while paused. Both renderers
      snapshot a parallel bolt flag under the engine lock and draw the bolt over the
      tile. JUnit (PowerOverlayTest): blink phases + unpowered res-zone shows/hides bolt.
- [x] 2. Currency: new game.CurrencyFormat (R prefix, no space, space-grouped, e.g.
      R8 833) is the one place we format money. GameStrings.formatFunds delegates to
      it (status bar funds + palette cost); Android status-bar cost and desktop cost
      label now use it instead of a hardcoded "$". Engine bundle strings are unchanged.
      JUnit (CurrencyFormatTest): grouping, negatives, no "$", formatFunds returns rand.
- [x] 3. Tool guard: GameController.ToolKind/kindOf classifies tools; STROKE tools
      (bulldozer, wire, road, rail, park, R/C/I) stay selected, ONE_SHOT tools
      (police, fire, stadium, seaport, coal, nuclear, airport) auto-return to Pan
      after a successful placement (maybeAutoClear in all apply paths). Selected-tool
      bar shows icon + name + cost + a 48dp X that returns to Pan (portrait bottom bar
      + new landscape bottom bar; desktop status area with icon + X); "Pan" when
      nothing is selected. Two-finger pan and long-press query unchanged. JUnit
      (ToolGuardTest): one-shot clears on success, stays on failure, stroke stays, kinds.
- [x] 4. Android back button (MainActivity.onBackPressed): closes the query dialog
      first, then the open tools drawer (portrait only; landscape palette is
      permanent). With nothing open, three presses within 2s show a cancelable
      "Exit NietoCity?" dialog (Exit / Stay, Stay focused as default); one or two
      presses show a "Press back 3 times to exit" toast. Never exits without the
      dialog; exitApp() is the single exit point (Phase 7 splash hook).
- [x] 5. Gate green: gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug ->
      "BUILD SUCCESSFUL in 8s / 43 actionable tasks: 43 executed". Engine tests: 32
      total, 0 failures / 0 errors (EngineSmoke 1 + Currency 4 + GameController 5 +
      RoadJoin 5 + ToolGuard 4 + AnimationClock 3 + PowerOverlay 2 + TileIndex 4 +
      Viewport 4). NietoCity-debug.apk and NietoCity-desktop.jar refreshed in the
      project root (published by the build's publishApkToRoot / jar-to-root tasks).
- [x] 6. Docs: TASKS.md final; CHANGES.md Phase 4a entry at the top; README Controls
      updated (rand, selected-tool bar + X, tool kinds, path-following, back button,
      power bolt); CLAUDE.md gained the currency rule and the tool-kind rule.

## Phase 4b: Speed control, mini map with overlays, and the classic dialogs
- [x] 0. Speed control: GameController owns a chosen run speed (SLOW/NORMAL/FAST/
      SUPER_FAST) + a separate pause; the clock runs at PAUSED while paused else
      the chosen speed (uses Speed.animationDelay). Status bar: pause/play toggle
      + tap-cycle speed label. Desktop: same + keys (space=pause, 1..4=speeds;
      Space+drag pan dropped). Chosen speed persists on the retained controller
      for the session. JUnit SpeedTest (5): interval per speed, cycle order,
      pause separate, PAUSED stops ticks, null/PAUSED ignored by setChosenSpeed.
- [x] 1. Engine overlay accessors: added 4 small read-only per-tile getters to
      Micropolis (getPopulationDensityAt, getPollutionAt, getCrimeAt,
      getPoliceCoverage); GPL header kept; no logic change; recorded in
      CHANGES.md with a note that land value/traffic/fire/power were already
      publicly accessible so only these four were missing.
- [x] 2. Mini map panel: shared render core (render.MiniMap builds the overview
      ARGB pixels one-per-tile via render.TileColors; render.MapOverlay added for
      task 3). Phone: MiniMapView corner overlay toggled from the status-bar ⋮
      button, never over the top bar. Desktop: docked right panel toggled by the
      Map button. Both draw the viewport rectangle and re-centre on tap/drag.
      JUnit MiniMapTest (4): overview tap maps to the matching tile within one.
- [x] 3. Overlay layer: render.MapOverlay (None + Population/Pollution/Crime/
      Land value/Traffic/Power grid/Fire/Police) tints the MAIN map translucently
      and replaces the mini map colours; ramp mirrors OverlayMapView (getCI);
      power grid shows powered zones red, unpowered blue, lines grey. Phone: an
      Overlay submenu in the status-bar overflow (with the mini map toggle).
      Desktop: an Overlay MenuButton beside the Map toggle. Off by default.
- [x] 4. Budget dialog: shared game.BudgetControl (current/preview/apply over the
      engine's generateBudget + public budget fields; preview restores fields so
      read-outs don't disturb the sim). Tax slider (0..20), road/fire/police
      funding sliders, live R read-outs (revenue, expenses, cash flow); Apply
      writes back. Auto-shows on the annual tick unless the persisted "Don't show
      automatically" checkbox is set (Android SharedPreferences, desktop
      Preferences); always reachable from the menu/Budget button. JUnit
      BudgetControlTest (4): apply writes + clamps the fields; preview is
      side-effect-free and reflects the proposed tax rate.
- [x] 5. Evaluation dialog: shared game.EvaluationReport reads CityEval under the
      lock (approval yes/no, score+delta, population+delta, city class, top four
      problems + votes). Read-only dialog on both platforms (menu/Evaluation
      button); closes on OK/Back/Esc. Class and problem names are provided in
      GameStrings (the original CityStrings bundle was not imported).
- [x] 6. Graphs dialog: shared game.GraphData extracts the six history series
      (res/com/ind/crime/pollution/cash flow) for the 10-year (0..119) or 120-year
      (120..239) window, newest first, each auto-scaled to 0..1. Drawn natively
      (Android GraphView, desktop Canvas): axes, six colour-coded polylines, a
      legend, and a 10y/120y toggle. No chart library.
- [x] 7. Menu: phone has a status-bar ⋮ overflow (PopupMenu) holding Budget,
      Evaluation, Graphs, the mini map toggle and an Overlay submenu. Desktop's
      loose buttons are consolidated into one "Menu" MenuButton with the same
      items (Budget/Evaluation/Graphs, mini map toggle, Overlay submenu), leaving
      only the pause/play and speed controls beside it, so the bar stays tidy.
- [x] 8. Gate green. `gradlew --rerun-tasks :engine:test :desktop:jar
      assembleDebug` raw tail:
        "BUILD SUCCESSFUL in 5s
         43 actionable tasks: 43 executed"
      Engine tests (from the XML reports): 45 total, 0 failures, 0 errors
      (EngineSmoke 1 + Currency 4 + GameController 5 + RoadJoin 5 + Speed 5 +
      ToolGuard 4 + Budget 4 + AnimationClock 3 + MiniMap 4 + PowerOverlay 2 +
      TileIndex 4 + Viewport 4). NietoCity-debug.apk and NietoCity-desktop.jar in
      the project root were refreshed by the build's publish tasks (both stamped
      2026-09-21 09:21).
- [x] 9. Docs: TASKS.md finalised; CHANGES.md Phase 4b entry at the top; README
      Controls section gained speed, mini map, overlays, dialogs and the menu (and
      the space=pause/1..4 keys); CLAUDE.md notes the overlay accessors.

## Phase 5: Disasters, sprites and sound
- [x] 0. Sprite frames: copied the 61 obj<id>-<frame>.png into
      engine/src/main/resources/sprites/ (per-kind counts match SpriteKind:
      1/5,2/8,3/11,4/8,5/16,6/3,7/6,8/4); scripts/copy-sprites.cmd records the
      provenance; THIRD_PARTY.md gained a sprite-artwork entry.
- [x] 1. Sprite rendering: shared render.SpriteImages resolves frame classpath
      paths and snapshots visible sprites (Frame: objectId, frameIndex, x/y/off/
      size) under the engine lock. Both renderers preload the frame PNGs and draw
      each sprite over the tiles at (x+offx)*zoom-scroll, integer zoom, nearest-
      neighbour. JUnit SpriteImagesTest (2): every SpriteKind resolves all frames.
- [x] 2. Sound engine: copied the 14 wavs to engine resources/sounds/. New
      game.SoundPlayer interface (play/setMuted/isMuted/release); GameController
      routes engine citySound events and playSound() to it. Android
      AndroidSoundPlayer (SoundPool; wavs extracted from the classpath to cache
      files, since SoundPool can't read a jar entry), desktop DesktopSoundPlayer
      (AudioClip). Both preload all clips, default ON, best-effort (never throw),
      with a persisted Mute item in the overflow menu. JUnit SoundTest (1): every
      Sound maps to a bundled wav (BULLDOZE is intentionally silent).
- [x] 3. Disaster menu: a Disasters submenu triggers the engine's own make* on
      the engine thread - Fire/Flood/Tornado/Earthquake/Monster/Nuclear meltdown.
      Each engine call fires its *_REPORT message (shown in the ticker); a
      representative sound plays for the ones the engine doesn't voice at trigger
      (fire=siren, flood/tornado=explosion, monster=roar; earthquake and meltdown
      use the engine's own explosions). Meltdown with no nuclear plant shows a
      note. Plane crash & shipwreck are natural-only and labelled as such, not
      faked - their sprites/explosions still render when the engine spawns them.
- [x] 4. Random-disasters toggle: a persisted menu checkbox ("Random disasters",
      default ON) wired via GameController.set/isRandomDisastersEnabled to the
      engine's own noDisasters field - the flag doDisasters() checks (enabled ==
      noDisasters false). No engine logic changed; only the flag is set.
- [x] 5. Earthquake shake: GameController listens for the engine's
      earthquakeStarted() and exposes shakeIntensity() (1.0 decaying to 0 over
      ~1.2s). Each renderer offsets the whole drawing by a few decaying random
      pixels while active (translate only; map coordinates untouched; the black
      fill hides the edges). Driven by the sim's per-tick frames, so no busy loop.
- [x] 6. Gate green. `gradlew --rerun-tasks :engine:test :desktop:jar
      assembleDebug` raw tail:
        "BUILD SUCCESSFUL in 4s
         43 actionable tasks: 43 executed"
      Engine tests (XML reports): 48 total, 0 failures, 0 errors (Phase 4b's 45 +
      SpriteImages 2 + Sound 1). NietoCity-debug.apk and NietoCity-desktop.jar
      refreshed in the root (stamped 2026-09-21 10:08). APK size: 2 401 168 B
      before Phase 5 -> 3 247 832 B after (+846 664 B, ~0.81 MiB) from the 61
      sprite PNGs and 14 wavs.
- [x] 7. Docs: TASKS.md finalised; CHANGES.md Phase 5 entry at the top; README
      gained a Disasters and sound section (menu, mute, player-triggered vs
      natural plane crash/shipwreck); THIRD_PARTY.md has the sprite-art and sounds
      entries (tasks 0 and 2); CLAUDE.md notes sprites/audio are bundled GPL
      assets, offline only.

## Phase 6a: Map generator and New City screen
- [x] 0. Engine generator API: added public setters to MapGenerator
      (setCreateIslandMode 0/1/2, setLakeLevel, setCurveLevel, setTreeLevel) that
      only expose existing fields; generation algorithms unchanged; GPL header
      kept; recorded in CHANGES.md.
- [x] 1. GameController.newGame(level, seed, terrain) + buildCity() (deterministic
      by seed), remembers seed/terrain; newGame(level) uses a random seed;
      getSeed/getTerrainConfig/getGameLevel; game.TerrainConfig (Island +
      Auto/None/Low/High levels). JUnit MapGenTest (4): same seed+settings ->
      identical map, different seeds differ, seed remembered, trees-off removes
      trees.
- [x] 2. New City screen (overflow menu "New City…"): difficulty Easy/Medium/Hard,
      terrain controls (Island none/seldom/always; Lake/River/Trees auto/none/
      low/high), a seed field showing the current seed (type to reproduce) and a
      Reroll button, plus a preview thumbnail. Phone portrait a scrollable column,
      landscape and desktop a two-column form. Start swaps to the new city and
      recentres the view (the discard confirm comes in task 4).
- [x] 3. Live preview: the mini-map thumbnail regenerates on any control/seed
      change on a background thread (Android MiniMapPreview single-thread
      executor; desktop a daemon executor + Platform.runLater), with a generation
      counter dropping stale results so the UI never blocks and only the newest
      map is painted.
- [x] 4. Start + discard guard: Start now asks "Start a new city? Your current
      city will be discarded." (Start / Cancel, Cancel default) before swapping,
      since there is no save/load yet. On confirm the platform builds the new
      GameController, carries over the session settings, swaps it in and recentres
      the view; Cancel keeps the running city.
- [x] 5. Gate green. `gradlew --rerun-tasks :engine:test :desktop:jar
      assembleDebug` raw tail:
        "BUILD SUCCESSFUL in 5s
         43 actionable tasks: 43 executed"
      Engine tests (XML reports): 52 total, 0 failures, 0 errors (Phase 5's 48 +
      MapGenTest 4). NietoCity-debug.apk (3 259 252 B) and NietoCity-desktop.jar
      refreshed in the root (stamped 2026-09-21 13:18).
- [x] 6. Docs: TASKS.md finalised; CHANGES.md Phase 6a entry at the top (the
      generator API + the New City screen); README gained a New City section
      (screen, terrain controls, seeds, discard confirm); CLAUDE.md notes the
      scenarios and save/load are later phases and why (no scenario data in the
      engine source; Android lacks StAX so save/load will use classic binary .cty).

## Phase 6b: Save and load (classic binary .cty)
- [x] 0. Confirm + record the exact v1 layout in CHANGES.md (history x6, misc
      field order, column-major map, 27120 bytes, no header). Reader is upstream.
- [x] 1. Golden round-trip test (micropolisj.engine.CityFileV1RoundTripTest, 2
      tests): 27120-byte count; load-fixpoint (s2==s3 byte-for-byte); tile-ordinal
      survival A->B; funds/resPop/comPop/indPop/cityTime/cityTax/gameLevel/
      cityClass/cityScore and all six history arrays equal A->B. Proven RED first
      - row-major writer: "fixpoint ... first differed at element [3125]"; drop
      resPop: "resPop expected:<1> but was:<0>" - then GREEN with the correct
      writer. Engine tests: 54 total, 0 failures.
- [x] 2. Writer landed in task 1 (micropolisj.engine.CityWriterV1, 27120 bytes,
      package-private access, no upstream file changed). game.CityFile facade:
      save() snapshots under the engine lock (no tick tears it) and writes to a
      .tmp then renames (atomic), load() delegates to the engine's load();
      GameController.loadGame(File) builds a fresh controller from a .cty.
- [x] 3. Storage: game.SaveMeta (name/date/pop/funds .properties sidecar).
      Android SaveStore writes <filesDir>/saves/<base>.cty + <base>.png thumbnail
      + <base>.meta; list() newest-first with thumbnails; delete removes all
      three. Desktop DesktopSaveStore writes <user.home>/NietoCity/saves/<base>.cty
      + .meta (thumbnails rendered live; file chooser is task 4). Never writes
      into the repo or Dropbox.
- [x] 4. Save/Load screens from the overflow menu. Load: a named-slot list with
      thumbnail/name/date/pop/funds; tap loads behind a "Load a city? Unsaved
      changes will be lost." confirm; per-slot Delete behind a confirm. Save: a
      name field (defaults to the current name) with an overwrite confirm. Desktop
      adds "Open .cty…" and "Save as…" via a FileChooser; its slot thumbnails are
      rendered live from each .cty.
- [x] 5. Wired together: the city name follows after Start or Load (desktop title
      shows it; used as the Save default). The discard guard (New City and Load)
      now offers Save first / Proceed / Cancel (Cancel default); Save first quick-
      saves under the current name and only proceeds if the save succeeds.
- [x] 6. Gate green. `gradlew --rerun-tasks :engine:test :desktop:jar
      assembleDebug` raw tail:
        "BUILD SUCCESSFUL in 5s
         43 actionable tasks: 43 executed"
      Engine tests (XML reports): 54 total, 0 failures, 0 errors (Phase 6a's 52 +
      CityFileV1RoundTripTest 2, incl. the 27120-byte count + golden fixpoint).
      NietoCity-debug.apk (3 271 276 B) and NietoCity-desktop.jar refreshed in the
      root (stamped 2026-09-21 13:44).
- [x] 7. Docs: TASKS.md finalised; CHANGES.md Phase 6b entry at the top (format +
      writer + golden test + facade/storage/screens); README gained a Save and
      Load section (menu, where saves live, .cty interchange with desktop
      Micropolis); CLAUDE.md notes .cty is classic binary v1, round-trips history
      and interchanges with the engine's own reader.

## Phase 7: Start screen, educational mode, splash, about and GPL
- [x] 0. Assets: intro mp4 copied verbatim to app res/raw and desktop resources;
      splash_exit.png -> splash_exit.jpg (~300 KB, 1280x1280) for both; adaptive
      launcher icon from AppIcon.png (foreground with the black corners flood-
      filled transparent, solid brand-blue #0B3986 background, all densities +
      legacy fallbacks). Root originals untouched; the 10 MB PNG not shipped.
      Repeatable via scripts/prep-assets.cmd (scripts/AssetPrep.java).
- [x] 1. Start splash on cold launch: full-screen video on black (letterboxed
      contain, no controls, tap-skip after 1s, ~5s safety), then the start
      screen. Android SplashActivity is the launcher (VideoView; shows the first
      frame when OS animations are off). Desktop plays it via JavaFX MediaView
      (mp4 extracted to a temp file). Intro audio muted (logo animation).
- [x] 2. Start screen (both): Continue (only when an autosave exists), New City,
      Load City, How to play / Educational, About, Licence (GPL), Quit, and a
      disabled "Scenarios (coming soon)". Continue/New/Load wired to the existing
      screens (New/Load from the menu skip the discard guard).
- [x] 3. Autosave to a reserved, hidden slot (Android onStop; desktop on close)
      and Continue reloads it. Best-effort: a failure never crashes or blocks
      exit. The slot is excluded from the Load list.
- [x] 4. Educational mode: persisted "Explain" toggle (off by default) in the
      overflow menu; when on, picking a tool, choosing an overlay, querying a
      tile (footer) and viewing the evaluation (per-problem) show a plain-language
      card. ONE shared game.Education registry covers all 16 tools, 8 overlays and
      7 CityProblem values, plus power/road-access/demand basics and the How-to-
      play reference. JUnit EducationTest (4) proves completeness.
- [x] 5. About screen (shared game.AppInfo): credits (Micropolis; Electronic
      Arts; Maxis and Will Wright; Don Hopkins; Jason Long / MicropolisJ),
      "Created by Nieto Software", the app version (Android reads its real
      versionName), and the offline + GPLv3-Micropolis line.
- [x] 6. Licence (GPL) screen: bundled GPLv3 LICENSE + THIRD_PARTY notices
      (engine resources), scrollable, reachable from About and the start screen
      via shared game.LicenseText.
- [x] 7. Exit splash: the single exit point (Android 3-press back confirm + start
      Quit; desktop window close + menu Quit) fades to splash_exit.jpg with
      "Copyright 2026 Nieto Software. All rights reserved." for ~1.8s, then exits.
      Autosave happens first; ceiling timer + swallow-all; re-entrancy safe.
- [x] 8. Gate green. `gradlew --rerun-tasks :engine:test :desktop:jar
      assembleDebug` raw tail:
        "BUILD SUCCESSFUL in 11s
         44 actionable tasks: 44 executed"
      Engine tests (XML reports): 58 total, 0 failures, 0 errors (Phase 6b's 54 +
      EducationTest 4). NietoCity-debug.apk refreshed to 5 710 129 B (was
      3 271 276 B; +2 438 853 B, ~2.33 MiB, from the intro mp4, the splash jpg and
      the launcher icons) and NietoCity-desktop.jar refreshed, both stamped
      2026-09-22 09:57.
- [x] 9. Docs: TASKS.md finalised; CHANGES.md Phase 7 entry at the top; README
      gained "Starting the game" (splash, start screen, Continue, About/GPL, exit
      splash) and an "Educational mode" section (and the New City / menu notes
      updated); CLAUDE.md notes the launcher icon and splash are wired and the app
      is English-only for now.

## Phase 8: Polish, landscape/tablet, and the 1.0.0 release
- [x] 0. Prerequisites and version: versionCode 2 / versionName "1.0.0" (and
      AppInfo.VERSION for the desktop About). Toolchain check on this machine:
      keytool present (Android Studio JBR); Liberica JDK 8 Full jre present
      (jre/bin/java.exe + jre/lib/ext/jfxrt.jar); apksigner present (SDK
      build-tools 34/35/36). MISSING: launch4j (needed for the Windows exe, task
      5). STOP for Andre to confirm the tools are ready before proceeding.
- [x] 1. Polish pass (newer screens + in-game HUD): spacing, type, dialog sizing
      + scroll, no clipped text at 3x, tidy menu. Investigate before changing.
- [x] 2. Landscape and tablet pass: left tool column in landscape, centred
      scrollable dialogs, mini map/status bar placement, start + New City scaling;
      rotation without restart.
- [x] 3. Intro audio ON (both platforms); keep tap-skip after 1s + safety timer.
      *** STOP: polish + landscape ready to test on the S22 before release. ***
- [ ] 4. (ON HOLD until the Phase 8 fix is tested on the S22) Android release signing: keystore via keytool OUTSIDE the repo; wire
      signingConfigs.release from gitignored keystore.properties; assembleRelease;
      verify with apksigner; copy to root as NietoCity-1.0.0.apk. .gitignore
      *.zip/*.exe/keystore.properties.
- [ ] 5. Windows icon + exe: NietoCity.ico from AppIcon.png (black corners cut to
      transparent, 16..256); launch4j wraps desktop.jar into NietoCity.exe with a
      bundled relative jre\ and the .ico.
- [ ] 6. Bundle + package under C:\NietoCity-build: Liberica JRE 8 Full beside the
      exe as jre\; include LICENSE + THIRD_PARTY; zip to root as
      NietoCity-1.0.0-windows.zip; report size; delete the scratch (named).
- [ ] 7. GPL publish check: full source + LICENSE + THIRD_PARTY present; grep for
      secrets/keystores/passwords (none); suggest tagging v1.0.0.
- [ ] 8. Gate + docs: gradlew --rerun-tasks :engine:test :desktop:jar
      assembleDebug AND assembleRelease; raw tail + test count; update TASKS.md,
      CHANGES.md, README, CLAUDE.md; confirm root artefacts + timestamps.

## Phase 8 fix: tool selection desync and status bar wrapping
(Release tasks 4 to 8 above stay ON HOLD until Andre has tested this on the S22.)
- [x] 0. Investigate + report. CAUSE A: the selected tool lives in
      GameController.tool, but ToolPaletteView keeps its OWN controller reference
      (set once by palette.setup in onCreate). MainActivity.swapController (New
      City / Load, including the start screen's New City and Load City, which open
      over a placeholder city) rebinds CityView and the mini map but NOT the
      palette, so palette taps toggle the discarded controller: palette highlights
      POWERPLANT, while the bar and CityView touch read the live controller (no
      tool) and show/do Pan. Re-tap then "deselects" only the old controller, and X
      clears a tool the live one never had. Continue is fine (controller built
      before setup); rotation re-runs setup, which hides the bug. Touch routing
      itself was correct. Desktop always reads its current controller (no desync)
      but pulls state by hand. CAUSE B: StatusBarView splits the width equally
      across FIVE weighted cells (date, funds, pop, tool, cost; tool + cost
      duplicate the bottom bar); with the fixed pause/speed/menu buttons (~130dp)
      each gets ~43dp at 360dp, so "Feb 1900" / "R20 000" wrap. Desktop labels
      ellipsize when narrow.
- [ ] 1. Tests first, proven red: TouchRouter.decide (pure) + GameController
      selection/listener test (select, re-select, X, one-shot, controller swap).
- [ ] 2. Fix A: one source of truth (GameController + tool listener); palette,
      bottom bar and touch observe it; rebind on every swap. Desktop too.
- [ ] 3. Tool UX: picking collapses the drawer; accent border round the map while
      armed; "Pan" only when nothing armed; Back with a tool armed returns to Pan.
- [ ] 4. Fix B: status values one line, flexible width, autosize down; desktop too.
- [ ] 5. Gate + CHANGES.md + TASKS.md.

## Later phases (see NietoCity_Project_Plan_rev2.pdf)
- Phase 3: Tool palette, placement, status bar
- Phase 4: Speed, budget, evaluation, graphs, mini map
- Phase 5: Disasters, sprites, sound
- Phase 6: Map generator, save and load, scenarios
- Phase 7: Start screen, educational mode, about and GPL screens
- Phase 8: Polish, signed APK, Windows exe, publish source
