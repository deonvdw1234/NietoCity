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

## Later phases (see NietoCity_Project_Plan_rev2.pdf)
- Phase 2: Tile renderer, Android and desktop
- Phase 3: Tool palette, placement, status bar
- Phase 4: Speed, budget, evaluation, graphs, mini map
- Phase 5: Disasters, sprites, sound
- Phase 6: Map generator, save and load, scenarios
- Phase 7: Start screen, educational mode, about and GPL screens
- Phase 8: Polish, signed APK, Windows exe, publish source
