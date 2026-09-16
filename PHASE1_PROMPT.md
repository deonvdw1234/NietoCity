# Phase 1: Project scaffold and Micropolis engine import (rev 3)

You are building "NietoCity", an Android and Windows 7 port of the GPL-licensed
Micropolis (the open-sourced original SimCity engine). Follow CLAUDE.md in this
repo. Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task with a clear message; I push via GitHub Desktop myself. Stay in normal
manual mode; never start /code-review, /workflows or any multi-agent work.
Investigate and report the cause before changing anything when a build fails.
Lay this prompt out as a pinned task list (TASKS.md) and tick items as you go.

## Environment
- Windows, Android Studio installed. SDK at %LOCALAPPDATA%\Android\Sdk
  (platform 34, build-tools, platform-tools, cmdline-tools, emulator).
  ANDROID_HOME and JAVA_HOME (Android Studio JBR) are user environment
  variables. Never change them. If the JBR is too new for the Gradle/AGP you
  pick, use a Gradle toolchain (foojay resolver) that downloads JDK 17; if the
  Gradle daemon itself needs an older JVM, set org.gradle.java.home in
  %USERPROFILE%\.gradle\gradle.properties (outside the repo), never in the project.
- Test phone: Samsung Galaxy S22. Offline only.
- If GitHub is unreachable from the terminal, stop and tell me; I will download
  the MicropolisJ zip in a browser and put it in tools\ for you.

## Pinned task list (in order; commit after each)
0. Toolchain proof. Run java -version, confirm the SDK folders exist, and after
   task 1 run gradlew --version. Record the JDK, Gradle and AGP versions in
   CLAUDE.md under "Toolchain (verified <date>)".
1. Android project in the current folder: package za.co.nieto.nietocity,
   minSdk 26, targetSdk 34, Kotlin app module, Gradle Kotlin DSL, version
   catalog. Keep the existing CLAUDE.md, TASKS.md, README.md, LICENSE,
   THIRD_PARTY.md, .gitignore, .gitattributes and PHASE1_PROMPT.md. No INTERNET
   permission, no analytics, no update checks, no Google services.
   Redirect all build output outside Dropbox: set every module's build
   directory to C:\NietoCity-build\<module> (layout.buildDirectory) and set
   org.gradle.projectcachedir=C:/NietoCity-build/.gradle in gradle.properties.
   Confirm no build\ folder appears inside the repo after a build.
2. Pure-Java library module :engine. Fetch MicropolisJ (Jason Long,
   github.com/jason17055/micropolis-java, GPLv3) as a zip into tools\ and add
   tools\ to .gitignore. Copy ONLY the micropolisj.engine package plus the data
   files the engine loads at runtime (tiles.rc and any other .rc/.txt it reads;
   no PNGs, no Swing or GUI code) into :engine. Keep every GPL header. Add the
   exact commit hash you copied from to THIRD_PARTY.md.
3. :engine compiles as Java 8 with no Android or java.awt dependency. Use a
   JDK 8 toolchain or options.release = 8, then verify with javap -v that a
   compiled class reports "major version: 52". If any class uses java.awt,
   replace it with a small plain-Java shim and record it in CHANGES.md
   (most recent first, "Created by Nieto Software").
4. JUnit 4 test in :engine: new random map with a fixed seed, one residential
   zone, one road, one coal power plant, 200 sim ticks; assert population and
   funds changed. It must pass; quote the test count from the JUnit XML report.
5. Minimal MainActivity (Kotlin): instantiate the engine, run 100 ticks on a
   background thread, show "Engine OK: population X, funds Y" in a TextView.
   No rendering yet.
6. Pure-Java module :desktop (Java 8 target, same major version 52 check),
   depends on :engine; a Main class runs the same 100 ticks and prints the
   result. gradlew :desktop:jar produces a runnable JAR (Main-Class in the
   manifest, engine classes and data files bundled in). No desktop UI yet.
   No API newer than Java 8 anywhere in :engine or :desktop.
7. Gate. Run gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from
   the command line and quote the raw tail of the output, not a paraphrase.
   Report the APK and JAR paths with their timestamps. Do not open a file
   preview; just say they are written and where.
8. Update TASKS.md with the final status of every task; add "How to build" to
   README.md for the APK (assembleDebug, installDebug) and the JAR (java -jar);
   add these lines to CLAUDE.md: build output lives in C:\NietoCity-build;
   POPI not applicable (no personal data); the repo is deliberately public for
   GPL compliance, so never commit secrets or keystores; the standing-rules
   WPF items (themes, nav rail, mascot, RSG radio) do not apply; cost guard:
   no /code-review, no /workflows, no multi-agent work, manual mode only.

## When you stop
After task 8 give a terse summary: what was built, what surprised you, the raw
gate tail, what I should test on the phone and on the PC, what Phase 2 needs
from me, and remind me to push in GitHub Desktop.
