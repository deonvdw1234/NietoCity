# Phase 5: Disasters, sprites and sound (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review,
/workflows or multi-agent work. Investigate and report the cause before
changing anything when a build fails. Pin this list in TASKS.md, tick as you
go. Phase 4b is committed and tested on the S22. Apply every task to Android
AND desktop unless it says one platform. Money via CurrencyFormat (R). All
art and audio from the MicropolisJ GPL source in tools\ (no Maxis trademark
material); keep GPL headers and update THIRD_PARTY.md.

## Task list (in order; commit after each)
0. Sprite frames. Copy the 61 obj<objectId>-<frame>.png files from the
   MicropolisJ resources into engine/src/main/resources/sprites/ (train, car,
   helicopter, aeroplane, ship, monster, tornado, explosion, per SpriteKind
   objectId 1..8 and numFrames). Add a tracked note in scripts\ of where they
   came from. THIRD_PARTY.md: sprite artwork from the Micropolis open-source
   release via MicropolisJ, GPLv3.
1. Sprite rendering. In the shared render core add SpriteImages (loads the
   per-kind frame images from the classpath) and have the renderer draw the
   engine's live sprites each animation tick at their pixel position, integer
   zoom, nearest-neighbour, over the tiles. Engine sprites move on the sim
   thread; read them on the render thread through a snapshot so there is no
   tearing. JUnit: every SpriteKind resolves all its frames.
2. Sound engine. Copy the 14 wav files from the MicropolisJ resources\sounds
   into engine/src/main/resources/sounds\ (GPL, attributed). Define a small
   SoundPlayer interface in :engine used by GameController, fed by the
   engine's citySound listener (Sound enum). Android impl uses SoundPool;
   desktop impl uses javafx.scene.media.AudioClip. Preload all clips once.
   Default ON, with a Mute item in the overflow menu (persisted for the
   session). Never let a missing or failed clip throw; sound is best-effort.
   JUnit: every Sound enum value maps to a bundled wav that exists.
3. Disaster menu. Add a Disasters submenu to the overflow menu that triggers
   the engine's own disasters: Fire (makeFire), Flood (makeFlood), Tornado
   (makeTornado), Earthquake (makeEarthquake), Monster (makeMonster), Nuclear
   meltdown (makeMeltdown). Each shows its engine message in the ticker and
   plays its sound. Plane crash and shipwreck have no direct engine trigger
   (they arise from traffic and air/sea travel); do NOT fake them, note in
   the menu or README that they occur during play, and let their sprites and
   explosions render when the engine spawns them.
4. Random-disasters toggle. Wire the engine's own enable-disasters setting to
   a menu checkbox (default ON, persisted), so random disasters during play
   can be turned off, matching the original's Disasters on/off. Do not change
   engine logic; only set the flag the engine already reads. Investigate and
   report which flag/field this is before wiring it.
5. Earthquake feel (light touch, optional-safe): while an earthquake is
   active, apply a small view shake in each renderer (a few pixels, decaying),
   purely visual, never moving the underlying map coordinates. Keep it subtle.
6. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from
   the command line; quote the raw tail and the engine test count from the
   XML report. Confirm NietoCity-debug.apk and NietoCity-desktop.jar in the
   project root carry fresh timestamps, and note the APK size change from the
   sounds and sprites. No file preview.
7. Docs. TASKS.md final status; CHANGES.md at the top; README (disasters
   menu, sound and mute, which disasters are player-triggered vs natural);
   THIRD_PARTY.md (sprite art and sounds); CLAUDE.md one line that audio and
   sprites are bundled GPL assets, offline only.

## Definition of done
CC verifies: builds, tests (sprite frames resolve, every Sound maps to a
file), artefacts and the new APK size. Andre verifies on the S22 (never claim
these): triggering each disaster plays its sound and shows its sprite/effect;
the monster and vehicles animate and move; mute silences everything; the
earthquake shake is subtle; turning random disasters off stops them.

## When you stop
Terse summary: what was built, the flag you used for the disasters toggle,
what surprised you, the raw gate tail, the APK size before/after, what to
test, what Phase 6 (map generator, save and load .cty, the eight scenarios)
needs from me, and remind me to push.
