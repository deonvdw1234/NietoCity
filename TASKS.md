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
- [ ] 3. Tool guard: STROKE vs ONE-SHOT tools; selected-tool bar with large X to Pan; two-finger pan; query = long press.
- [ ] 4. Android back button: query dialog -> tools drawer -> three-press exit confirm.
- [ ] 5. Gate: gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug; quote tail + test count; fresh artefacts.
- [ ] 6. Docs: TASKS/CHANGES/README/CLAUDE updates.

## Later phases (see NietoCity_Project_Plan_rev2.pdf)
- Phase 3: Tool palette, placement, status bar
- Phase 4: Speed, budget, evaluation, graphs, mini map
- Phase 5: Disasters, sprites, sound
- Phase 6: Map generator, save and load, scenarios
- Phase 7: Start screen, educational mode, about and GPL screens
- Phase 8: Polish, signed APK, Windows exe, publish source
