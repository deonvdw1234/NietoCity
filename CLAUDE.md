# CLAUDE.md (NietoCity)
- Tone: warm, encouraging, concise.
- Small phases, pinned task list in TASKS.md; tick tasks as they finish.
- Commit after each task; the developer pushes via GitHub Desktop.
- Never run recursive deletes (no rm -rf, no Remove-Item -Recurse).
- Bug fixes: investigate and report the cause before changing anything.
- After creating a file, say it is written and where; never open a preview.
- Every document produced also gets a PDF copy.
- All documentation carries "Created by Nieto Software".
- Licence: GPLv3 (Micropolis). Keep headers; credit EA, Maxis and Jason Long.
- No SimCity name, logo or Maxis artwork anywhere in the app or repo.
- Kotlin app module, Java engine module, Java desktop module (Java 8 APIs only).
- Android: minSdk 26, targetSdk 34. Desktop: must run on Windows 7 with Java 8.
- Offline only: no INTERNET permission, no analytics, no update checks.
- Test phone: Samsung Galaxy S22.
- Currency: every money figure we format goes through game.CurrencyFormat (South
  African rand, "R" prefix, no space, space-grouped, e.g. R8 833); never a "$".
- Tool kinds: STROKE tools stay selected; ONE_SHOT tools (police, fire, stadium,
  seaport, coal, nuclear, airport) return to Pan after one successful placement
  (GameController.kindOf / maybeAutoClear).
- Engine accessors: Phase 4b added four small read-only per-tile getters to
  Micropolis (getPopulationDensityAt/getPollutionAt/getCrimeAt/getPoliceCoverage)
  for the map overlays; no engine logic changed (see CHANGES.md).
- Sprites and audio (Phase 5) are bundled GPL assets from the MicropolisJ source
  (engine resources /sprites and /sounds); played offline only, best-effort, no
  network (see THIRD_PARTY.md).
- Save/load (Phase 6b) is the classic binary v1 .cty (exactly 27120 bytes, no
  header): micropolisj.engine.CityWriterV1 writes it, the engine's own load_v1
  reads it (golden round-trip test), and it round-trips history and interchanges
  with desktop Micropolis / classic SimCity .cty. Binary (not the engine's XML
  .cty) because Android lacks StAX (javax.xml.stream). The eight scenarios remain
  deferred (no scenario data in our engine source).
- Phase 7 wired the launcher icon (Android adaptive icon generated from
  AppIcon.png), the cold-launch intro splash and the exit splash, the start screen
  (Continue/New City/Load City/How to play/About/Licence/Quit), autosave+Continue,
  and the shared Explain educational registry (game.Education); English only for
  now. Regenerate the icon/splash assets with scripts/prep-assets.cmd.

## Toolchain (verified 2026-09-16)
- JDK: 25.0.3 (Android Studio JBR at %JAVA_HOME%). Runs the Gradle daemon fine;
  no org.gradle.java.home override needed.
- Gradle: 9.5.1 (via wrapper). NOTE: Gradle 9.6+ removed the InternalProblems
  API that AGP 8.13.0 depends on, so we are pinned to 9.5.x. Do not bump the
  wrapper past 9.5.x without also moving to an AGP that no longer uses that API.
- AGP: 8.13.0. Kotlin: 2.4.20. compileSdk 34.
- Java 8 (:engine): compiled with javac --release 8 on JDK 25, which emits major
  version 52. (Only obsolete-option warnings; no JDK 8 install needed.)
- :desktop (from Phase 2): JavaFX 8 UI, so it uses a Gradle toolchain pinned to a
  JDK 8 "Full" that bundles JavaFX. Vendor BellSoft, languageVersion 8.
  Path: C:\Program Files\BellSoft\LibericaJDK-8-Full  (Liberica JDK 8 Full,
  1.8.0_482, jre\lib\ext\jfxrt.jar present). Detected by Gradle from the Windows
  registry; org.gradle.java.installations.auto-download=false (never download a
  JDK). JAVA_HOME is unchanged (still the JBR/JDK 25); only :desktop's toolchain
  differs. The desktop JAR does NOT bundle jfxrt.jar - JavaFX comes from the
  Java 8 Full runtime at run time.

## Project notes (from Phase 1)
- Build output lives in C:\NietoCity-build (outside Dropbox); nothing is written
  to a build\ folder inside the repo.
- POPI not applicable: the app collects and stores no personal data.
- The repo is deliberately public for GPL compliance, so never commit secrets or
  keystores.
- The standing-rules WPF items (themes, nav rail, mascot, RSG radio) do not apply
  to this project.
- Cost guard: no /code-review, no /workflows, no multi-agent work; manual mode
  only.
- Latest builds are copied to the repo root as NietoCity-debug.apk and
  NietoCity-desktop.jar (git-ignored, overwritten each build; *.apk/*.jar ignored).
