# CHANGES

Changes made to the imported Micropolis / MicropolisJ source, most recent first.
Created by Nieto Software.

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
