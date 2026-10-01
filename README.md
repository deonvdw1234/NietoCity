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

## Starting the game

- Intro splash: on a cold launch the Nieto intro video plays full-screen on black
  (letterboxed, no controls). Tap (or click) to skip after a second; it also ends
  on its own, with a short safety timer as a backstop. On Android, if the OS is set
  to remove animations, the first frame is shown briefly instead.
- Start screen: **Continue** (shown only when an autosave exists), **New City**,
  **Load City**, **How to play / Educational**, **About**, **Licence (GPL)**,
  **Quit**, and a disabled **Scenarios (coming soon)**. New City and Load City open
  the usual screens.
- Continue and autosave: the current city is autosaved to a hidden slot when you
  leave (Android: when the app is stopped; desktop: on close). Continue resumes it.
  Autosaving is best-effort and never blocks leaving the app.
- Exit splash: leaving the game (Android: press Back three times, then confirm, or
  Quit on the start screen; desktop: close the window or Quit) autosaves, then
  shows the exit splash with "Copyright 2026 Nieto Software. All rights reserved."
  for a moment before the app closes.
- About and Licence: About lists the credits (Micropolis; Electronic Arts; Maxis
  and Will Wright; Don Hopkins; Jason Long / MicropolisJ), the version, and a note
  that NietoCity is offline and based on GPLv3 Micropolis. The Licence screen shows
  the full GPLv3 text and the third-party notices, reachable from About and the
  start screen.

## Educational mode ("Explain")

Turn on **Explain (learn as you play)** in the menu (off by default, remembered).
While it is on, picking a tool, choosing an overlay, querying a tile, or viewing
the city evaluation shows a short plain-language card: what it is and the real
engine mechanic behind it (zones need power, road access and demand to grow;
industry and traffic raise pollution, which lowers land value; and so on). The
**How to play / Educational** screen on the start menu is the full reference,
covering the basics, all 16 tools, the 8 overlays and what citizens complain
about.

## Controls

A top status bar shows the date, funds and population, each on one line (the text
shrinks a little on a narrow screen rather than wrapping). The selected tool, its
cost and an X live in the selected-tool bar (it says "Pan" only when no tool is
armed), and a thin yellow border frames the map while a tool is armed. Money is shown in South African rand (e.g. R8 833). A one-line ticker under
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
  on the PC) holds the Budget, Evaluation and Graphs dialogs, the mini map toggle,
  the overlay picker and the "Explain (learn as you play)" toggle.
- Budget dialog: sliders for the tax rate (0-20%) and road/fire/police funding,
  with tax revenue, expenses and cash flow shown in rand; Apply saves them. It
  opens automatically once a year unless you tick "Don't show automatically".
- Evaluation dialog: the mayor's approval, city score and change, population and
  change, city class, and the worst problems. Read-only.
- Graphs dialog: line graphs of residential, commercial and industrial
  population, crime, pollution and cash flow, with a 10-year / 120-year toggle and
  a colour legend.

### New City

- Open "New City…" from the menu to generate a fresh map. Choose the difficulty
  (Easy/Medium/Hard, which sets the starting funds), and the terrain: Island
  (none/seldom/always) and the Lake, River and Trees amount (auto/none/low/high).
- Every map has a seed. The seed field shows the current city's seed; type a seed
  to reproduce that exact map, or press Reroll for a new random one. A live
  preview thumbnail shows the map as you change the controls.
- Start begins the new city. Since the running city is discarded, it asks first
  ("Start a new city? Your current city will be discarded.") with Save first /
  Start / Cancel - Cancel keeps your current city. (Opening New City from the start
  screen skips this, as there is nothing to keep.)

### Save and Load

- Save and Load are in the menu. Save asks for a name (the current city's name by
  default) and confirms before overwriting an existing save. Load shows your saved
  cities as a list with a thumbnail, name, date and population; tap one to load it
  (you are asked first, and can Save first). Delete a save from its row.
- Where saves live: on Android, in the app's private storage
  (`<app files>/saves/`), so they are removed if you uninstall the app. On the PC,
  in `Documents`-adjacent `NietoCity/saves` under your home folder; "Save as…" and
  "Open .cty…" let you read or write a `.cty` anywhere. Saves are never written
  into the project or Dropbox.
- Interchange: the `.cty` files are the classic binary Micropolis/SimCity v1
  format (exactly 27120 bytes), so a city saved here opens in desktop Micropolis
  and, if you have any classic SimCity/Micropolis `.cty`, it opens here. (Loading
  clears a few transient tile bits and rescans power, as the format intends.)

### Disasters and sound

- Disasters menu: the menu's Disasters submenu triggers Fire, Flood, Tornado,
  Earthquake, Monster and Nuclear meltdown (meltdown needs a nuclear plant). Each
  shows its message in the ticker and plays a sound, and its sprites and effects
  animate on the map. An earthquake gives a brief, gentle screen shake.
- Plane crash and shipwreck are not on the menu: they happen naturally during
  play (a plane or ship colliding), and their sprites and explosions render when
  the engine spawns them.
- Random disasters: on by default; turn them off with the "Random disasters" menu
  checkbox (your choice is remembered).
- Sound: on by default and bundled for offline play (the city's honks, sirens,
  explosions and the monster's roar). Silence everything with the "Mute sound"
  menu item (remembered).

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
  collapses to a thin strip with the "Tools" button. Picking a tool closes the
  sheet so the whole map is visible.
- Back button: closes the query dialog first, then returns an armed tool to Pan,
  then closes the tools drawer; with nothing open, press Back three times within two seconds to be asked before exiting (one
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
- The X in the status area, or Esc, returns to Pan.
- Space: pause/play. Keys 1-4: Slow / Normal / Fast / Ultra speed.
- Palette: a scrollable left column. The "Menu" button opens Budget, Evaluation,
  Graphs, the mini map and the overlay picker.
- An unpowered zone centre blinks a yellow lightning bolt until it has power.
