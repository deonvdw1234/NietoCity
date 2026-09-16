# Phase 2: Tile renderer for Android and desktop (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each task;
I push via GitHub Desktop. Manual mode only; no /code-review, /workflows or
multi-agent work. Investigate and report the cause before changing anything
when a build fails. Pin this list in TASKS.md and tick as you go. Phase 1 is
committed (HEAD 5ee453b); Phase 1 was not yet tested on the phone, so if the
S22 shows a problem I will report it separately.

Decisions for this phase: tile art = MicropolisJ's GPL tile pieces; desktop
UI = JavaFX 8 (my choice, aware it needs a Java 8 runtime that bundles JavaFX).
Reference only, do not copy Swing code: tools\micropolis-java-9f6ddb4...\src\
micropolisj\gui\TileImages.java and MicropolisDrawingArea.java show how the
original reads tiles.idx, cycles animation frames and calls city.animate().

## Task list (in order; commit after each)
0. JavaFX 8 toolchain proof. Find a JDK 8 that bundles JavaFX (jre\lib\ext\
   jfxrt.jar present), e.g. Liberica JDK 8 Full. If none is installed, STOP,
   tell me exactly what to install (free Liberica JDK 8 Full, no Oracle), and
   wait. Then give :desktop a Gradle toolchain (languageVersion 8, that
   vendor) and record the path and version in CLAUDE.md under Toolchain.
   :engine stays as it is (javac --release 8, major version 52).
1. Compose the tile atlas once. From the tools\ source, compile and run
   micropolisj.build_tool.MakeTiles (java.awt.headless=true) with
   graphics\tiles.rc for 16x16 into engine\src\main\resources\16x16\
   (tiles.png + tiles.idx). Commit the outputs. Put the exact commands in a
   tracked scripts\compose-tiles.cmd with a comment; do not add awt code to
   any module. Confirm tiles.idx names the same tile numbers tiles.rc does.
2. Platform-neutral render core in :engine, package za.co.nieto.nietocity.render
   (pure Java 8, no awt, no android): TileIndex (parses tiles.idx with the
   existing XML_Helper into per-tile frame lists: y offset in the strip per
   frame); Viewport (scroll offset in pixels, integer zoom 1x/2x/3x, visible
   tile range, clamping to the map, centre-on-tile); AnimationClock (calls
   city.animate() at the original cadence and advances the frame counter).
   JUnit tests: every tile number 0..(tile count-1) resolves to a frame; the
   viewport never exposes tiles outside the map; zoom keeps the centre tile.
3. Android renderer. A SurfaceView (CityView) with its own render thread:
   loads 16x16/tiles.png as a Bitmap from the classpath resource, draws the
   visible tiles via the render core with integer zoom and nearest-neighbour
   (no filtering, no anti-aliasing) so pixels stay crisp; redraws when the
   engine fires MapListener changes or the animation clock ticks, otherwise
   idles. Engine runs on its own thread at the original speed. Default zoom 3x
   on the S22, landscape and portrait both supported.
4. Touch: one-finger drag pans; pinch steps the zoom (1x/2x/3x, no fractional
   zoom); double tap centres on that tile. MainActivity now generates a new
   random map (MapGenerator) and shows CityView full screen with a small
   overlay text "NietoCity Phase 2: pop X, funds Y" updated each second.
5. Desktop renderer in JavaFX 8: a Stage with a Canvas using the same render
   core and the same atlas (javafx Image, setSmooth(false)); mouse drag pans,
   wheel steps zoom, arrow keys pan, a title bar text mirrors the overlay from
   task 4. Same engine thread model; UI updates via Platform.runLater.
   gradlew :desktop:jar still produces the runnable JAR (JavaFX comes from the
   runtime, do not bundle jfxrt.jar). Run it once here and report what you
   saw in the console, not what the window looked like.
6. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from the
   command line; quote the raw tail and the engine test count from the XML
   report. Report APK and JAR paths with timestamps. No file preview.
7. Docs. TASKS.md final status; README "How to build" now also states the
   desktop runtime requirement (Java 8 with JavaFX, e.g. Liberica JRE 8 Full)
   and the compose-tiles script; CHANGES.md entry at the top; THIRD_PARTY.md
   notes the tile artwork origin (Micropolis open-source release, via
   MicropolisJ, GPLv3, no Maxis trademark art).

## Definition of done
CC verifies: builds, tests, tiles.idx coverage, artefact paths.
Andre verifies (you cannot see, never claim these): map renders on the S22
and the PC, pan and zoom feel right, water and coast animate, pixels are crisp.

## When you stop
Terse summary: what was built, what surprised you, the raw gate tail, what I
should test on the phone and PC, what Phase 3 (tool palette, placement,
status bar) needs from me, and remind me to push in GitHub Desktop.
