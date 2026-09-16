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

## Toolchain (verified 2026-09-16)
- JDK: 25.0.3 (Android Studio JBR at %JAVA_HOME%). Runs the Gradle daemon fine;
  no org.gradle.java.home override needed.
- Gradle: 9.5.1 (via wrapper). NOTE: Gradle 9.6+ removed the InternalProblems
  API that AGP 8.13.0 depends on, so we are pinned to 9.5.x. Do not bump the
  wrapper past 9.5.x without also moving to an AGP that no longer uses that API.
- AGP: 8.13.0. Kotlin: 2.4.20. compileSdk 34.
- Java 8 (:engine, :desktop): compiled with javac --release 8 on JDK 25, which
  emits major version 52. (Only obsolete-option warnings; no JDK 8 install needed.)
