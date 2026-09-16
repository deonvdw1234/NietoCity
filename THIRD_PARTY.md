# Third-party notices

NietoCity is based on Micropolis, the source code of the original SimCity, released by Electronic Arts under the GNU General Public License v3.0 in 2008.

- Micropolis: Copyright Electronic Arts Inc. Original design by Will Wright (Maxis). Open-source release coordinated by Don Hopkins. https://github.com/SimHacker/micropolis
- MicropolisJ: Java port by Jason Long, GPLv3. https://github.com/jason17055/micropolis-java
  - Source copied from commit `9f6ddb4b5f36a005fe4c4f77488d7969eabf0797` (branch master).
  - Imported into `:engine`: the `micropolisj.engine` package, the single helper
    class `micropolisj.XML_Helper` (a compile dependency of the engine's save/load
    code), and the runtime data files `tiles.rc` and `tiles/aliases.txt`.
  - Not imported: GUI/Swing code, graphics/PNG/sound assets, and build tooling.

SimCity is a trademark of Electronic Arts Inc. NietoCity is not affiliated with or endorsed by Electronic Arts or Maxis. No SimCity trademarks or Maxis artwork are used.

Created by Nieto Software
