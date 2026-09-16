# CHANGES

Changes made to the imported Micropolis / MicropolisJ source, most recent first.
Created by Nieto Software.

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
