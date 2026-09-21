# Third-party notices

NietoCity is based on Micropolis, the source code of the original SimCity, released by Electronic Arts under the GNU General Public License v3.0 in 2008.

- Micropolis: Copyright Electronic Arts Inc. Original design by Will Wright (Maxis). Open-source release coordinated by Don Hopkins. https://github.com/SimHacker/micropolis
- MicropolisJ: Java port by Jason Long, GPLv3. https://github.com/jason17055/micropolis-java
  - Source copied from commit `9f6ddb4b5f36a005fe4c4f77488d7969eabf0797` (branch master).
  - Imported into `:engine`: the `micropolisj.engine` package, the single helper
    class `micropolisj.XML_Helper` (a compile dependency of the engine's save/load
    code), and the runtime data files `tiles.rc` and `tiles/aliases.txt`.
  - Not imported: GUI/Swing code, graphics/PNG/sound assets, and build tooling.

## Tile artwork

The tile atlas shipped in `engine/src/main/resources/16x16/` (`tiles.png`,
`tiles.idx`) is composed from the Micropolis tile artwork included in the
open-source Micropolis release, via MicropolisJ (Jason Long), under GPLv3. It is
built from `graphics/tiles.rc` and the tile art in the MicropolisJ source using
that project's `MakeTiles` tool (see `scripts/compose-tiles.cmd`). This is the
open-sourced Micropolis art, not SimCity/Maxis trademark artwork; no Maxis
trademark art is used.

## Tool icons

The tool palette icons in `engine/src/main/resources/tools/` are MicropolisJ's
`ic*.png` toolbar icons (Jason Long, GPLv3), copied and renamed to the
`MicropolisTool` names (`<NAME>.png` and `<NAME>_hi.png` for the highlighted
selected state). Open-sourced Micropolis artwork; no Maxis trademark art.

## Sprite artwork

The mobile-sprite frame images in `engine/src/main/resources/sprites/`
(`obj<id>-<frame>.png`, 61 files: train, car, helicopter, aeroplane, ship,
monster, tornado and explosion) are the Micropolis sprite artwork from the
open-source Micropolis release, via MicropolisJ (Jason Long), under GPLv3. They
are copied unchanged from the MicropolisJ `resources/` folder (see
`scripts/copy-sprites.cmd`). Open-sourced Micropolis art, not SimCity/Maxis
trademark artwork; no Maxis trademark art is used.

## Sounds

The sound effects in `engine/src/main/resources/sounds/` (14 `*.wav` files:
explosions, sirens, honks, heavy traffic, the zone-laid chime and the monster
roar) are the Micropolis sound effects from the open-source Micropolis release,
via MicropolisJ (Jason Long), under GPLv3, copied unchanged from the MicropolisJ
`resources/sounds/` folder. They are played best-effort by the app (Android
SoundPool, desktop JavaFX AudioClip) and are bundled for fully offline use.
Open-sourced Micropolis audio; no Maxis trademark material.

## Strings

The English string bundles in `engine/src/main/resources/micropolisj/`
(`CityMessages.properties`, `GuiStrings.properties`, `StatusMessages.properties`)
are from MicropolisJ (Jason Long), GPLv3, with their headers preserved. They
provide engine message text, the date/funds/tool labels, and the zone/status
words used by the status bar and the query panel.

SimCity is a trademark of Electronic Arts Inc. NietoCity is not affiliated with or endorsed by Electronic Arts or Maxis. No SimCity trademarks or Maxis artwork are used.

Created by Nieto Software
