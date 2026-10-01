# Phase 8: Polish, landscape/tablet, and the 1.0.0 release (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review, /workflows
or multi-agent work. Investigate and report the cause before changing anything.
Pin this list in TASKS.md, tick as you go. Phase 7 is committed and tested.
Apply to both platforms unless one is named. Money via CurrencyFormat (R). This
phase ends at the first public release, so it STOPS twice for me: a prerequisite
check, and a break after polish before the irreversible release steps.

## Task list (in order; commit after each)
0. Prerequisites and version. Set versionCode 2, versionName "1.0.0". Check the
   release toolchain on THIS machine and STOP with a list if anything is
   missing: keytool (in the JDK); launch4j for the Windows exe (if absent, tell
   me to install it, free, and where); and the Liberica JDK 8 Full jre folder to
   bundle (default C:\Program Files\BellSoft\LibericaJDK-8-Full\jre) - confirm it
   exists. Do not proceed past task 0 until I confirm the tools are ready.
1. Polish pass. Go through the newer screens (start, New City, Save/Load, About,
   Licence, Explain cards, dialogs) and the in-game HUD: consistent spacing and
   type, dialogs sized to content and scrollable, no clipped text at 3x on the
   phone, a tidy menu. Investigate and report anything wrong before changing it.
2. Landscape and tablet pass. Verify and fix layouts in landscape and on larger
   screens: classic left tool column in landscape, dialogs centred and
   scrollable, mini map and status bar placed sensibly, start and New City
   scaling without clipping. Keep rotation working without a restart.
3. Intro audio ON. Play the intro logo's sound (currently muted); keep
   tap-to-skip after 1 s and the safety timer. Desktop plays it too.

   *** STOP HERE. Tell me polish + landscape are ready to test on the S22
   before the release tasks below. Wait for my go-ahead. ***

4. Android release signing. Generate a release keystore with keytool OUTSIDE
   the repo (e.g. C:\NietoCity-build\nietocity-release.jks); wire
   signingConfigs.release from a gitignored keystore.properties (path +
   passwords there, never in build.gradle). minifyEnabled false (resources are
   resolved by name). Build assembleRelease, verify with apksigner, copy the
   signed APK to the project root as NietoCity-1.0.0.apk. Print the keystore
   path and passwords ONCE so I can back them up; never commit them. Add *.zip,
   *.exe and keystore.properties to .gitignore.
5. Windows icon and exe. Make NietoCity.ico from AppIcon.png with the solid
   black corners cut to TRANSPARENT (multi-size 16..256). launch4j wraps
   desktop.jar into NietoCity.exe using a BUNDLED runtime (a relative jre\
   folder beside the exe), with the .ico as the exe icon.
6. Bundle and package (build under C:\NietoCity-build, never Dropbox). Copy the
   Liberica JDK 8 Full jre beside the exe as jre\ so it runs on a bare Windows 7;
   include LICENSE and THIRD_PARTY. Zip the folder to the project root as
   NietoCity-1.0.0-windows.zip; report the size. Delete the out-of-Dropbox
   scratch afterwards, naming what you delete.
7. GPL publish check. Confirm the tree carries full source, LICENSE (GPLv3) and
   THIRD_PARTY, and grep it for any secret, keystore or password before I push
   (there must be none). The repo is already public. Suggest I tag v1.0.0.
8. Gate and docs. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug
   AND assembleRelease; quote the raw tail and the engine test count. Update
   TASKS.md, CHANGES.md (top), README (install the signed APK, run the Windows
   zip), CLAUDE.md (released 1.0.0; keystore gitignored and I hold it; Windows
   bundles a Liberica JRE 8 Full). Confirm the root holds NietoCity-1.0.0.apk and
   NietoCity-1.0.0-windows.zip with fresh timestamps.

## Definition of done
CC verifies: builds, tests, APK signed, zip built, no secrets in the tree.
Andre verifies (never claim these): the signed APK installs and runs on the S22
with the right icon and intro sound; the Windows zip runs on a PC with no Java;
polish and landscape look right. Ask before deleting anything beyond the named
scratch.

## When you stop
Terse summary: what was built, the tools you needed, what surprised you, the raw
gate tail, the APK and zip sizes, the keystore location (remind me to BACK IT UP
- losing it means no future updates under this identity), what to test, tag
v1.0.0, and remind me to push. Note what is left for a future update (the
deferred scenarios; bilingual if I ever want it).
