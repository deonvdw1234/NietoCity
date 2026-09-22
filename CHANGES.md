# CHANGES

Changes made to the imported Micropolis / MicropolisJ source, most recent first.
Created by Nieto Software.

## 2026-09-22 - Phase 7 start screen, educational mode, splash, about and GPL

No `micropolisj.*` engine source was changed this phase; the one small engine-
adjacent addition is a field on our own `game.EvaluationReport` (the top problems
exposed as `CityProblem` enums, so the Explain cards can be looked up). Everything
else is new app/desktop UI, new shared `game`/resource files, and assets.

- Assets (build-time, originals untouched): the intro `Nieto Logo Animated.mp4` is
  copied verbatim to `app/src/main/res/raw/nieto_logo_animated.mp4` and to
  `desktop/src/main/resources/`; `splash_exit.png` (10 MB) is compressed to a
  ~300 KB `splash_exit.jpg` (1280x1280) for both platforms; and `AppIcon.png`
  becomes an Android adaptive launcher icon - a foreground with the black corners
  flood-filled transparent over a solid brand-blue (#0B3986) background, at every
  mipmap density with legacy square/round fallbacks. Regenerate with
  `scripts/prep-assets.cmd` (`scripts/AssetPrep.java`, headless javax.imageio).
  The 10 MB PNG is not shipped.
- Start splash: Android `SplashActivity` (the launcher) plays the video with a
  `VideoView` (first frame shown when OS animations are off); desktop plays it with
  a JavaFX `MediaView` (the mp4 extracted from the jar to a temp file, since
  JavaFX media wants a file URI). Letterboxed "contain", no controls, tap to skip
  after 1s, ~5s safety timer. Intro audio is muted.
- Start screen: Continue (only when an autosave exists), New City, Load City, How
  to play / Educational, About, Licence (GPL), Quit, and a disabled "Scenarios
  (coming soon)". Android uses a `StartActivity` that launches `MainActivity` with
  an action; desktop swaps the JavaFX scene (the game scene is built in a new
  `enterGame()`).
- Autosave + Continue: both platforms write a hidden reserved slot (Android in
  `onStop`, desktop on close) that Continue reloads; the slot is excluded from the
  Load list; failures are swallowed so leaving is never blocked.
- Educational "Explain" mode: a persisted menu toggle (off by default). ONE shared
  `game.Education` registry maps every tool (16), overlay (8) and city problem (7),
  plus power/road-access/demand basics, to a short accurate card, and builds the
  How-to-play reference. When on, selecting a tool, choosing an overlay, querying a
  tile (footer) and viewing the evaluation (per-problem) show the matching card.
  JUnit `EducationTest` (4) proves the registry is complete.
- About / Licence: shared `game.AppInfo` (About text + version) and
  `game.LicenseText` (reads the bundled GPLv3 `LICENSE` + `THIRD_PARTY` from engine
  resources). The Licence screen is the GPL-compliance surface for the build,
  reachable from About and the start screen.
- Exit splash: the single exit point on each platform fades to `splash_exit.jpg`
  with "Copyright 2026 Nieto Software. All rights reserved." for ~1.8s, then quits.
  Autosave runs first; re-entrancy safe with a hard ceiling timer and swallow-all.

## 2026-09-21 - Phase 6b save and load (classic binary .cty)

Confirmed the exact classic binary v1 `.cty` layout by reading the engine's own
upstream reader (`load_v1` / `loadHistoryArray_v1` / `loadMisc_v1` / `loadMap_v1`),
which is our independent check for the writer we add. The stream is big-endian
(Data{Input,Output}Stream), exactly 27120 bytes, with NO 128-byte header
(`load()` only skips a header when the file is larger than 27120):

1. Six history arrays, in this order: res, com, ind, crime, pollution, money -
   each 240 shorts (2 bytes each) = 2880 bytes.
2. misc: 120 shorts (240 bytes) in `loadMisc_v1` field order:
   [0] unused, [1] externalMarket(unused), [2] resPop, [3] comPop, [4] indPop,
   [5] resValve, [6] comValve, [7] indValve, [8-9] cityTime (int),
   [10] crimeRamp, [11] polluteRamp, [12] landValueAverage, [13] crimeAverage,
   [14] pollutionAverage, [15] gameLevel, [16] evaluation.cityClass,
   [17] evaluation.cityScore, [18-49] unused (32 shorts), [50-51] totalFunds
   (int), [52] autoBulldoze, [53] autoBudget, [54] autoGo, [55] userSoundOn
   (unused), [56] cityTax, [57] simSpeed (ordinal), [58-59] policePercent*65536
   (int), [60-61] firePercent*65536 (int), [62-63] roadPercent*65536 (int),
   [64-119] unused (56 shorts).
3. map: DEFAULT_WIDTH(120) x DEFAULT_HEIGHT(100) shorts, COLUMN-MAJOR (x outer
   0..119, y inner 0..99) = 24000 bytes. The full 16-bit tile char is written;
   on load the reader clears ZONEBIT|ANIMBIT|BULLBIT|BURNBIT|CONDBIT and rescans
   power (`checkPowerMap`), so a loaded city differs from the original on those
   transient bits by design - the tile's low ordinal (`z & LOMASK`) is preserved.

Writer and save/load (built golden-test-first):

- `micropolisj.engine.CityWriterV1` writes the 27120-byte stream in exactly that
  order. It is a new Nieto-authored file in the engine package (so it can read the
  same package-private fields the reader writes); no upstream file was changed.
- `micropolisj.engine.CityFileV1RoundTripTest` is the golden test: it saves a
  seeded, developed, 200-tick city through our writer and reloads it through the
  engine's own upstream `load_v1` (the independent check), asserting the 27120
  byte count, a byte-for-byte load fixpoint, tile-ordinal survival, and equal
  funds/pop/time/tax/level/evaluation/history. Proven RED first with a row-major
  and a misc-dropping writer, then GREEN.
- `game.CityFile` is the platform-neutral facade: `save` snapshots under the
  engine lock (no tick tears it) and writes to a `.tmp` then renames it (atomic);
  `load` delegates to the engine's `load`. `GameController.loadGame(File)` builds
  a fresh controller from a `.cty`. `game.SaveMeta` is a `.properties` sidecar.
- Storage: Android keeps `<filesDir>/saves/<base>.cty` + a `.png` thumbnail +
  `.meta`; desktop keeps `<user.home>/NietoCity/saves/<base>.cty` + `.meta` and a
  file chooser for arbitrary locations. Saves never go into the repo or Dropbox.
- Save/Load screens live in the overflow menu (slot list with thumbnail/name/
  date/population, load and delete behind confirms; overwrite confirm on save).
  The discard guard offers Save first / Proceed / Cancel.

## 2026-09-21 - Phase 6a map generator and New City screen

Engine change: added a small public terrain-config API to
`micropolisj/engine/MapGenerator.java` so the New City screen can configure a
generate. These only set fields the class already has; the generation algorithms
are unchanged and the GPL header is kept. Methods added (most recent first):

- `setTreeLevel(int)` - tree level (-1 auto / 0 none / >0 amount).
- `setCurveLevel(int)` - river curviness (-1 auto / 0 none / >0 level).
- `setLakeLevel(int)` - lake level (-1 auto / 0 none / >0 amount).
- `setCreateIslandMode(int)` - island mode (0 never / 1 seldom / 2 always); maps
  to the existing package-private CreateIsland enum without exposing it.

The New City flow lives in the game layer: `game.TerrainConfig` (island +
Auto/None/Low/High lake/river/tree levels) maps these to the engine values, and
`GameController.newGame(level, seed, terrain)` / `buildCity(...)` generate
deterministically from a seed (the same seed and settings reproduce the map).

- New City screen: reached from the overflow menu on both platforms - difficulty,
  the terrain controls, a seed field (shows the current seed; type one to
  reproduce a map) and a Reroll button, with a live mini-map preview (shared
  render.MiniMap / TileColors) regenerated off the UI thread on every change.
  Start asks before discarding the running city (no save/load yet), then swaps the
  controller to the new city and recentres the view.

## 2026-09-21 - Phase 5 disasters, sprites and sound

- Sprite artwork: copied the 61 MicropolisJ sprite frame images (obj<id>-<frame>
  .png) into `engine/src/main/resources/sprites/` (see `scripts/copy-sprites.cmd`
  and THIRD_PARTY.md).
- Sprite rendering: shared `render.SpriteImages` resolves the per-kind frame
  images on the classpath (never decoding them, so the engine module stays free of
  awt/android) and snapshots the engine's visible sprites under the engine lock.
  Both renderers preload the frames and draw the live sprites over the tiles at
  integer zoom, nearest-neighbour, no tearing.
- Sound: copied the 14 MicropolisJ wavs into `engine/src/main/resources/sounds/`.
  New `game.SoundPlayer` interface, fed by the engine's `citySound` listener via
  GameController; Android uses SoundPool (wavs extracted from the classpath to
  cache files), desktop uses JavaFX AudioClip. All clips preload, sound is on by
  default and best-effort (never throws), with a persisted Mute item in the menu.
- Disasters menu: a Disasters submenu triggers the engine's own make* methods
  (Fire/Flood/Tornado/Earthquake/Monster/Nuclear meltdown) on the engine thread;
  each shows its engine message in the ticker and plays a sound. Plane crash and
  shipwreck are natural-only (they arise from traffic and air/sea travel) and are
  not faked; their sprites and explosions render when the engine spawns them.
- Random disasters: a persisted menu checkbox toggles the engine's own
  `noDisasters` flag (the field its `doDisasters()` checks), default on. No engine
  logic changed - only the flag is set.
- Earthquake shake: GameController notes the engine's `earthquakeStarted()` and
  exposes a decaying `shakeIntensity()`; each renderer applies a small, decaying,
  purely-visual view shake (translate only; map coordinates untouched).
- No `micropolisj.*` source files were changed this phase (the sprite/sound assets
  are added resources, and the disaster/flag wiring uses existing public API).

## 2026-09-21 - Phase 4b speed, mini map, overlays and dialogs

- Speed control: `GameController` owns a chosen run speed (SLOW/NORMAL/FAST/
  SUPER_FAST) and a separate pause; the engine clock runs at PAUSED while paused
  else the chosen speed, using the `Speed` enum's animationDelay/simStepsPerUpdate
  (no invented timings). Status bar gains a pause/play toggle and a tap-cycle
  speed label; desktop adds keys (space = pause, 1..4 = speeds; Space+drag pan
  dropped). The chosen speed persists on the retained controller for the session.
- Mini map: shared `render.MiniMap` builds the whole-city overview one ARGB pixel
  per tile from `render.TileColors` (a tile->colour classifier), and maps an
  overview point back to a tile. Phone shows it as a corner overlay (never over
  the status bar) toggled from the overflow menu; desktop docks it as a right-hand
  panel. Both draw the viewport rectangle and re-centre the main view on tap/drag.
- Overlays: shared `render.MapOverlay` (None + population, pollution, crime, land
  value, traffic, power grid, fire and police coverage) tints the main map
  translucently and recolours the mini map; the ramp mirrors MicropolisJ's
  OverlayMapView. Picker lives in the overflow menu; off by default.
- Budget dialog: shared `game.BudgetControl` reads (`generateBudget`) and writes
  the engine's public budget fields; `preview` restores the fields so live
  read-outs don't disturb the sim. Tax and funding sliders, R read-outs, Apply
  writes back; auto-shows once a year unless a persisted "Don't show" checkbox is
  set.
- Evaluation dialog: shared `game.EvaluationReport` reads `CityEval` (approval,
  score+delta, population+delta, class, top four problems + votes). Read-only.
- Graphs dialog: shared `game.GraphData` extracts the six history series for the
  10-year / 120-year windows, each auto-scaled; drawn natively (no chart library)
  with a legend and a range toggle.
- Menu: an overflow menu (phone status-bar button, desktop "Menu" MenuButton)
  gathers the dialogs, the mini map toggle and the overlay picker.
- City-class and problem display names, and the six graph series colours/labels,
  are provided in the shared game layer (the original CityStrings bundle was not
  part of the imported source; the graph colours match GuiStrings graph_color.*).

Engine change (first time an imported `micropolisj.*` file is touched): added
small READ-ONLY accessors to `micropolisj/engine/Micropolis.java` so the
platform-neutral overlay / mini map render core can read the coarse overlay maps
without reaching into engine arrays. The GPL header is unchanged and no
simulation logic is altered. Accessors added (most recent first):

- `getPopulationDensityAt(int x, int y)` - popDensity for the 2x2 block (0 out of
  bounds).
- `getPollutionAt(int x, int y)` - pollutionMem for the 2x2 block.
- `getCrimeAt(int x, int y)` - crimeMem for the 2x2 block.
- `getPoliceCoverage(int x, int y)` - policeMapEffect (reach) for the 8x8 block.

Investigation note: the phase brief listed powerMap, landValueMem, trfDensity,
fireStMap and policeMap as the package-private maps needing accessors, but in
this engine land value, traffic and fire-station coverage already have public
accessors (`getLandValue`, `getTrafficDensity`, `getFireStationCoverage`), the
power grid is read through the existing public `isTilePowered`, and pollution,
crime, population density, fire reach (fireRate) and police reach
(policeMapEffect) are already public fields. So only the four bounds-checked
per-tile getters above were genuinely missing; they wrap the already-public
`popDensity`, `pollutionMem`, `crimeMem` and `policeMapEffect`.

## 2026-09-17 - Phase 4a play feedback (first real city)

- Road join: investigated first. The engine already joins orthogonally-adjacent
  roads placed in separate strokes (its `fixZone` fixes each laid tile and its
  four neighbours), proven by `RoadJoinTest`; the engine is unchanged. The gap
  Andre saw came from our layer applying a drag as one axis-snapped stroke.
  GameController now keeps the finger's tile waypoints and lays the road along
  them as axis-aligned engine strokes on release (`previewPath`/`applyPath`); the
  engine charges each tile once. Both renderers follow the drag path.
- Power indicator: shared `render.PowerOverlay` blinks the LIGHTNINGBOLT tile over
  unpowered zone centres, driven from the engine animation cycle (~0.5s on/off,
  steady while paused). Both renderers draw it.
- Currency: new `game.CurrencyFormat` renders all money we format as South African
  rand ("R", no space, space-grouped, e.g. R8 833). `GameStrings.formatFunds`
  delegates to it; the Android and desktop cost labels use it. No "$" from our
  code; engine bundle strings keep their wording.
- Tool guard: `GameController.ToolKind`/`kindOf` split tools into STROKE (stay
  selected) and ONE_SHOT (police, fire, stadium, seaport, coal, nuclear, airport;
  return to Pan after one successful placement). A selected-tool bar shows the
  tool's icon, name and cost with a 48dp X that returns to Pan; "Pan" shows when
  nothing is selected. Two-finger pan and long-press query unchanged.
- Android back button: closes the query dialog, then the tools drawer; with
  nothing open, three presses within 2s ask "Exit NietoCity?" (Exit / Stay, Stay
  default); one or two presses toast a hint. Single exit point for the Phase 7
  splash.
- No changes to the imported `micropolisj.*` source files this phase.

## 2026-09-16 - Phase 3b fixes (S22)

- Tool palette rebuilt as a grid of finger-sized cells (>=56dp, 3x
  nearest-neighbour icons, name + cost, highlight border): portrait bottom sheet
  (4-column grid with a "Tools" bar) and landscape 72dp left column; desktop
  icons scaled 3x too. Fixes the tiny, unhittable palette on the S22.
- New cities now start with the game level's funds (easy 20000 by default) via
  GameController.newGame(), matching how the original applies funds when a city
  is created. Fixes the "$0" status bar on a new city.

## 2026-09-16 - Phase 3 tools, placement and status bar

- Added a shared game layer to `:engine` (`za.co.nieto.nietocity.game`):
  GameController (owns the engine thread, selected tool, tool apply/preview/query,
  message queue), StatusSnapshot, QueryReport, GameStrings.
- Copied the English MicropolisJ string bundles (CityMessages, GuiStrings,
  StatusMessages) into engine resources and load them via ResourceBundle. They
  are parsed by java.util.ResourceBundle (works on Android), unlike the tile
  index which needs the StAX-free scanner.
- Copied MicropolisJ's 16 `ic*.png` tool icons (plain + highlighted) into engine
  resources, renamed to the MicropolisTool names.
- No changes to the imported `micropolisj.*` source files themselves this phase.

## 2026-09-16 - Phase 2 tile renderer

- Composed the 16x16 tile atlas (`engine/src/main/resources/16x16/tiles.png` and
  `tiles.idx`) from the MicropolisJ source art using its `MakeTiles` build tool,
  run headless (see `scripts/compose-tiles.cmd`). No java.awt code was added to
  any shipped module.
- Added a platform-neutral render core to `:engine`
  (`za.co.nieto.nietocity.render`: TileIndex, Viewport, AnimationClock). The atlas
  index is parsed with a small dependency-free scanner rather than the engine's
  `XML_Helper`, because `XML_Helper` uses `javax.xml.stream` (StAX), which is not
  present in the Android runtime and would crash on the phone.
- No changes to the imported `micropolisj.*` source files themselves this phase.

## 2026-09-16 - Phase 1 engine import

- Imported the `micropolisj.engine` package from MicropolisJ commit
  `9f6ddb4b5f36a005fe4c4f77488d7969eabf0797` into the `:engine` module, with all
  GPL headers preserved unchanged.
- Also copied `micropolisj.XML_Helper` (from `src/micropolisj/XML_Helper.java`).
  The engine package imports it (`Micropolis.java`) for XML save/load, so the
  engine will not compile without it. It is a small, pure-Java StAX helper with
  no GUI, AWT, or Android dependency. Copied verbatim; no code changes.
- Copied the runtime data files the engine reads via `getResourceAsStream`:
  `tiles.rc` (from the upstream `graphics/` folder) to the resources root, and
  `tiles/aliases.txt` to `resources/tiles/`.
- java.awt shim: NONE REQUIRED. The imported engine code contains no `java.awt`
  import or reference, so no shim was needed for Task 3.
- No engine source files were otherwise modified; the engine compiles to Java 8
  bytecode (class-file major version 52) via `javac --release 8`.
