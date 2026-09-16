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
- [ ] 2. Render core in :engine (za.co.nieto.nietocity.render): TileIndex, Viewport, AnimationClock; pure Java 8, no awt/android; JUnit tests (tile coverage, viewport clamp, zoom centre)
- [ ] 3. Android renderer: CityView SurfaceView + render thread, tiles.png from classpath, integer zoom, nearest-neighbour, redraw on MapListener/animation; engine on own thread; default 3x on S22
- [ ] 4. Touch: drag pan, pinch zoom (1x/2x/3x), double-tap centre; MainActivity generates random map, CityView full screen + overlay "pop X, funds Y" each second
- [ ] 5. Desktop renderer (JavaFX 8): Stage + Canvas, same core + atlas, setSmooth(false); drag pan, wheel zoom, arrow keys; title mirrors overlay; :desktop:jar still runnable (JavaFX from runtime, don't bundle jfxrt.jar)
- [ ] 6. Gate: gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug; quote raw tail + test count; report APK/JAR paths
- [ ] 7. Docs: TASKS.md, README (desktop runtime req + compose-tiles), CHANGES.md, THIRD_PARTY.md (tile art origin)

## Later phases (see NietoCity_Project_Plan_rev2.pdf)
- Phase 2: Tile renderer, Android and desktop
- Phase 3: Tool palette, placement, status bar
- Phase 4: Speed, budget, evaluation, graphs, mini map
- Phase 5: Disasters, sprites, sound
- Phase 6: Map generator, save and load, scenarios
- Phase 7: Start screen, educational mode, about and GPL screens
- Phase 8: Polish, signed APK, Windows exe, publish source
