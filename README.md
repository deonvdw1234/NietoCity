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

### Speed, mini map, overlays and menu

- Speed: the status bar has a pause/play toggle and a speed label you tap to
  cycle Slow -> Normal -> Fast -> Ultra. Pause is separate, so resuming returns to
  the chosen speed. On the PC, space pauses/plays and keys 1..4 pick the speed.
- Mini map: an overview of the whole city (one pixel per tile) that shows the
  current view as a rectangle; tap or drag on it to re-centre the main map. It is
  a corner overlay on the phone and a docked side panel on the PC, toggled from
  the menu.
- Overlays: tint the map (and mini map) by population density, pollution, crime,
  land value, traffic, the power grid (powered zones red, unpowered blue), or fire
  and police coverage. Choose one - or None - from the menu. Off by default.
- Menu: an overflow menu (the ⋮ button in the phone status bar; the "Menu" button
  on the PC) holds the Budget, Evaluation and Graphs dialogs, the mini map toggle
  and the overlay picker.
- Budget dialog: sliders for the tax rate (0-20%) and road/fire/police funding,
  with tax revenue, expenses and cash flow shown in rand; Apply saves them. It
  opens automatically once a year unless you tick "Don't show automatically".
- Evaluation dialog: the mayor's approval, city score and change, population and
  change, city class, and the worst problems. Read-only.
- Graphs dialog: line graphs of residential, commercial and industrial
  population, crime, pollution and cash flow, with a 10-year / 120-year toggle and
  a colour legend.

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
- Status bar: the pause/play button and the speed label (tap to cycle), and the ⋮
  button that opens the menu (Budget, Evaluation, Graphs, mini map, overlays).
- An unpowered zone centre blinks a yellow lightning bolt until you wire it to
  power.

### PC (desktop)
- Left-drag: place a tile or draw a stroke (with preview) when a tool is
  selected; pans when no tool is selected. Roads/rail/wire follow the drag path.
- Right-drag: pan. Mouse wheel: zoom. Arrow keys: pan.
- Right-click: query the tile (a dialog; Esc or OK to close).
- The X in the status area returns to Pan.
- Space: pause/play. Keys 1-4: Slow / Normal / Fast / Ultra speed.
- Palette: a scrollable left column. The "Menu" button opens Budget, Evaluation,
  Graphs, the mini map and the overlay picker.
- An unpowered zone centre blinks a yellow lightning bolt until it has power.
