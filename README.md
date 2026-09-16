# NietoCity

An educational Android and Windows 7 port of Micropolis, the open-sourced original SimCity simulation engine.

Created by Nieto Software. Based on Micropolis, GPLv3.

- Android app: Kotlin, minSdk 26, targetSdk 34, offline only
- Windows desktop: Java 8, runs on Windows 7 and later
- Shared simulation: pure Java :engine module

## Licence
This project is licensed under the GNU General Public License v3.0. See LICENSE and THIRD_PARTY.md.

## How to build

Requirements: the Android SDK (platform 34) and a JDK. The Gradle wrapper
(`gradlew`) fetches Gradle 9.5.1, which runs on the Android Studio JBR (JDK 25).
All build output is written outside the repo, to `C:\NietoCity-build`.

Modules: `:engine` (pure-Java simulation, Java 8), `:app` (Android, Kotlin),
`:desktop` (pure-Java, Java 8).

### Android app (APK)
```
gradlew assembleDebug     # builds app-debug.apk
gradlew installDebug      # installs it on a connected device (e.g. Galaxy S22)
```
The APK is written to `C:\NietoCity-build\app\outputs\apk\debug\app-debug.apk`.

### Desktop app (runnable JAR)
```
gradlew :desktop:jar      # builds a self-contained runnable JAR
java -jar C:\NietoCity-build\desktop\libs\desktop.jar
```
Runs on Windows 7 with a Java 8 (or later) runtime. Prints, for example:
`Engine OK: population 20, funds 996845`.

### Tests
```
gradlew :engine:test      # engine simulation smoke test
```

### Full Phase 1 gate
```
gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug
```
