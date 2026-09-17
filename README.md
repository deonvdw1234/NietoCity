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
All build output is written outside the repo, to `C:\NietoCity-build`. For
convenience the latest builds are also copied to the project root as
`NietoCity-debug.apk` and `NietoCity-desktop.jar` (git-ignored, overwritten each
build).

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

## Controls

A top status bar shows the date, funds, population, and the selected tool and its
cost. Money is shown in South African rand (e.g. R8 833). A one-line ticker under
it shows the latest city message for a few seconds. The tool palette lists the 16
tools; the selected tool is highlighted. Selecting the highlighted tool again
returns to pan mode.

A selected-tool bar always shows the current tool's icon, name and cost with a
large X that returns to Pan (it shows "Pan" when nothing is selected).

Tools come in two kinds:
- Stroke tools (bulldozer, wire, road, rail, park, and the R/C/I zones) stay
  selected so you can keep drawing. Roads, rail and wire follow your drag path, so
  a bent drag lays a connected bend with no gaps.
- One-shot tools (police, fire, stadium, seaport, coal and nuclear plants, airport)
  return to Pan automatically after one successful placement.

### Phone (Android)
- No tool selected: one finger pans, pinch zooms (1x/2x/3x), double tap centres.
- Tool selected: tap to place, or drag to draw a stroke (a translucent preview
  shows where it lands - green if buildable, red if not) applied on release; two
  fingers always pan; pinch zooms.
- Long press: query the tile (a small panel; tap outside or Back to close).
- The X on the selected-tool bar returns to Pan.
- Palette: a scrollable left column in landscape; a bottom sheet in portrait that
  collapses to a thin strip with the "Tools" button.
- Back button: closes the query dialog first, then the tools drawer; with nothing
  open, press Back three times within two seconds to be asked before exiting (one
  or two presses show a hint).
- An unpowered zone centre blinks a yellow lightning bolt until you wire it to
  power.

### PC (desktop)
- Left-drag: place a tile or draw a stroke (with preview) when a tool is
  selected; pans when no tool is selected. Roads/rail/wire follow the drag path.
- Right-drag or Space+drag: pan. Mouse wheel: zoom. Arrow keys: pan.
- Right-click: query the tile (a dialog; Esc or OK to close).
- The X in the status area returns to Pan.
- Palette: a scrollable left column.
- An unpowered zone centre blinks a yellow lightning bolt until it has power.
