# Phase 7: Start screen, educational mode, splash, about and GPL (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review,
/workflows or multi-agent work. Investigate and report the cause before
changing anything when a build fails. Pin this list in TASKS.md, tick as you
go. Phase 6b is committed and tested. Apply to Android AND desktop unless one
platform is named. English only. Money via CurrencyFormat (R). The splash is
NOT implemented yet (only the asset files are in the repo); build it here.

## Task list (in order; commit after each)
0. Assets (keep root originals untouched; do NOT ship the 10 MB PNG). Copy
   "Nieto Logo Animated.mp4" into Android res/raw as nieto_logo_animated.mp4
   and into desktop resources. Make a compressed splash_exit.jpg (well under
   1 MB) from splash_exit.png. Launcher icon from AppIcon.png as an Android
   adaptive icon (artwork foreground, solid brand background; the black corners
   are masked by the launcher).
1. Start splash on cold launch: full-screen H.264 video on black (Android
   MediaPlayer/VideoView from res/raw; desktop JavaFX MediaView), letterboxed
   contain, no controls, tap to skip after 1 s, a ~5 s safety timer that
   proceeds if playback never ends, then the start screen. If the OS asks to
   remove animations, show the first frame briefly instead.
2. Start screen (both platforms): Continue (only when an autosave exists),
   New City, Load City, How to play / Educational, About, Licence (GPL), Quit.
   Wire Continue/New City/Load to the existing screens. Scenarios are deferred:
   omit or show a disabled "Scenarios (coming soon)".
3. Autosave + Continue. Autosave the current city to a RESERVED slot on
   pause/exit (Android onStop; desktop on window close, before the exit
   splash), hidden from the normal Load list. Continue loads it. Autosave
   failure must never crash or block exit.
4. Educational mode. An Explain toggle in the overflow menu (persisted, off by
   default). When ON, selecting a tool, opening an overlay, and viewing the
   query panel or a city problem each show a short plain-language card: what it
   is and the REAL engine mechanic behind it. Cover all 16 tools, the 8
   overlays and the 7 CityProblem values (CRIME, POLLUTION, HOUSING, TAXES,
   TRAFFIC, UNEMPLOYMENT, FIRE), plus power/road-access/demand basics. Keep the
   text in ONE shared registry used by both platforms; content must be accurate
   to how the engine behaves (zones need power, road access and demand to grow;
   industry and traffic raise pollution, which lowers land value; and so on).
   JUnit: the registry has an entry for every tool, overlay and CityProblem.
5. About screen: credits (Micropolis; Electronic Arts; Maxis and Will Wright;
   Don Hopkins; Jason Long / MicropolisJ), "Created by Nieto Software", the app
   version, and a line that it is offline and based on GPLv3 Micropolis.
6. Licence (GPL) screen: the bundled LICENSE (GPLv3) scrollable plus the
   THIRD_PARTY notices; reachable from About and the start screen (the GPL
   compliance surface for the distributed build).
7. Exit splash: wire the SINGLE exit point (Android 3-press back confirm and
   the desktop close) to fade the app, show splash_exit.jpg with
   "Copyright 2026 Nieto Software. All rights reserved." for ~1.8 s, then exit.
   Ceiling timer + swallow-all so shutdown never blocks; re-entrancy safe.
   Autosave (task 3) happens before the splash.
8. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from the
   command line; quote the raw tail and the engine test count from the XML
   report. Report the new APK size. Confirm the root APK and JAR carry fresh
   timestamps. No file preview.
9. Docs. TASKS.md; CHANGES.md at the top; README (start screen, Continue,
   educational mode, about/GPL, splash); CLAUDE.md one line that the launcher
   icon and splash are wired and English-only for now.

## Definition of done
CC verifies: builds, tests (educational registry completeness), artefacts, APK
size. Andre verifies on the S22 (never claim these): the intro video plays then
the start screen shows; Continue resumes the last city; Explain cards appear
and read accurately; About and the GPL text show; the exit splash plays then
the app closes; the launcher icon looks right.

## When you stop
Terse summary: what was built, what surprised you, the raw gate tail, the APK
size, what to test, and remind me to push. Phase 8 (polish, landscape/tablet
pass, signed APK, Windows .ico + exe via launch4j, publish source) comes next;
the AppIcon.png corners are solid black, so the Windows .ico needs them cut to
transparent.
