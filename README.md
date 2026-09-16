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

Modules: `:engine` (pure-Java simulation + platform-neutral render core, Java 8),
`:app` (Android, Kotlin), `:desktop` (Java 8 + JavaFX 8).

The `:desktop` module builds with a JDK 8 "Full" toolchain (BellSoft Liberica
JDK 8 Full) so JavaFX 8 is available at compile time. Gradle finds it
automatically and never downloads a JDK. `:engine` still compiles to Java 8
bytecode with `javac --release 8` on the JBR.

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
The desktop app uses **JavaFX 8**, which is not bundled in the JAR. Run it with a
**Java 8 runtime that includes JavaFX**, e.g. BellSoft **Liberica JRE 8 Full**
(free, no Oracle). For example:
```
"C:\Program Files\BellSoft\LibericaJDK-8-Full\bin\java.exe" -jar C:\NietoCity-build\desktop\libs\desktop.jar
```
It runs on Windows 7 with such a runtime. A window opens showing the map;
drag to pan, mouse wheel to zoom (1x/2x/3x), arrow keys to pan. The title bar
shows the population and funds.

### Tests
```
gradlew :engine:test      # engine + render-core tests
```

### Full gate
```
gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug
```

### Tile atlas (one-off)
The tile artwork is composed into `engine/src/main/resources/16x16/`
(`tiles.png` + `tiles.idx`) and committed, so normal builds need nothing extra.
To regenerate it from the MicropolisJ source art (after unzipping MicropolisJ
into `tools\`), run:
```
scripts\compose-tiles.cmd
```
