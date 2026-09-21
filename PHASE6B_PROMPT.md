# Phase 6b: Save and load (classic binary .cty) with the save/load screens (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review,
/workflows or multi-agent work. Investigate and report the cause before
changing anything. Pin this list in TASKS.md, tick as you go. Phase 6a is
committed and tested. Apply to Android AND desktop unless it says one
platform. Money via CurrencyFormat (R). This is the highest-risk phase in the
project: a wrong writer loses a city, so build the golden test FIRST.

## The format (confirm before writing a byte)
The engine already READS classic binary .cty in load_v1: six history arrays
(res, com, ind, crime, pollution, money) of 240 shorts each, then loadMisc_v1
(120 shorts in that exact field order), then loadMap_v1 (DEFAULT_WIDTH x
DEFAULT_HEIGHT shorts, COLUMN-MAJOR: x outer 0..119, y inner 0..99), then
checkPowerMap(). All big-endian (DataInputStream/DataOutputStream). Total
exactly 27120 bytes, NO 128-byte header (load() only skips a header when the
file is larger than 27120). load_v1 CLEARS ZONEBIT|ANIMBIT|BULLBIT|BURNBIT|
CONDBIT on each tile and rescans power, so a loaded city differs from the
original on those transient bits by design.

## Task list (in order; commit after each)
0. Confirm and record the exact v1 layout in CHANGES.md (field order of
   loadMisc_v1, the six history arrays, column-major map, 27120 bytes, no
   header). No writer code yet. The reader is upstream MicropolisJ code we did
   not write, so it is the independent check for our writer.
1. Golden round-trip test FIRST (engine JUnit), and prove it RED before the
   writer exists or with a deliberately wrong writer. Invariant, chosen to
   respect the bit-clearing: (a) generate a seeded city, run ~200 ticks,
   checkPowerMap; (b) s1 = save(A); (c) B = load(s1); s2 = save(B); C =
   load(s2); s3 = save(C); assert s2 equals s3 BYTE-FOR-BYTE (load fixpoint);
   (d) every tile's low ordinal (z & LOMASK) in A survives into B; (e) funds,
   resPop/comPop/indPop, cityTime, cityTax, gameLevel, evaluation.cityClass/
   cityScore and all six 240-length history arrays are equal in A and B. Prove
   a row-major or misc-dropping writer makes it RED, then revert to green.
2. The writer: game.CityFileV1 (or similar) writing the 27120-byte stream in
   the exact order above via DataOutputStream. Pure Java in :engine, no
   Android/JavaFX. Save runs with the sim PAUSED or on the engine thread so no
   tick tears the snapshot. Load uses the engine's own load(File)/load_v1.
3. Save/load storage. Android: private app storage <filesDir>/saves/<name>.cty
   plus a sidecar <name>.png mini-map thumbnail and a small metadata record
   (name, saved date, population, funds); the .cty stays standard bytes with
   nothing appended. Desktop: a saves folder under the user home AND a file
   chooser for opening/saving a .cty anywhere. Never write saves into the repo
   or Dropbox by default.
4. Save/Load screens from the overflow menu. Load: a named-slot list showing
   each city's thumbnail, name, date and population; tap to load (if a city is
   running, confirm first: "Load a city? Unsaved changes will be lost.").
   Save: name field (defaults to the current name), overwrite confirm, writes
   the .cty and refreshes its thumbnail. Delete a slot behind a confirm.
   Desktop adds "Open .cty..." and "Save as..." via the file chooser.
5. Wire New City + Save/Load together: after Start or Load, the title/city
   name follows; the discard guard from 6a now also offers Save first when a
   city is running.
6. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from
   the command line; quote the raw tail and the engine test count from the XML
   report. Confirm the root APK and JAR carry fresh timestamps. No preview.
7. Docs. TASKS.md; CHANGES.md at the top (format + writer); README (save/load,
   where saves live, .cty interchange with desktop Micropolis); CLAUDE.md one
   line that .cty is classic binary v1, round-trips history, and interchanges
   with the engine's own reader.

## Definition of done
CC verifies: builds, the golden round-trip test (proven red then green),
byte-count 27120, artefacts. Andre verifies (never claim these): save a city,
force-quit, reload it and it is the same city; the slot list shows thumbnails;
delete works; on desktop a saved .cty re-opens; if he has any classic
SimCity/Micropolis .cty, loading it is the real external check.

## When you stop
Terse summary: the confirmed layout, how you proved the golden test red, what
was built, what surprised you, the raw gate tail, what to test, and remind me
to push. Phase 7 (start screen, educational mode, about and GPL screens; the
splash is partly in already) comes next.
