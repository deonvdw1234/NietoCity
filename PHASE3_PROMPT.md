# Phase 3: Tool palette, placement and status bar (rev 1)

NietoCity, Android and Windows 7 port of GPL Micropolis. Follow CLAUDE.md.
Warm, encouraging, concise tone. Never recursive deletes. Commit after each
task; I push via GitHub Desktop. Manual mode only; no /code-review, /workflows
or multi-agent work. Investigate and report the cause before changing anything
when a build fails. Pin this list in TASKS.md and tick as you go. Phase 2 is
committed (HEAD 1b0a7fb) but not yet tested on the S22; if the phone shows a
problem I will report it separately.

Decisions: tool icons = MicropolisJ's GPL ic*.png set (tools\...\resources,
plain and *hi highlighted variants); status bar at the TOP in the classic
order (date, funds, population, current tool and cost); palette = left column
in landscape, collapsible bottom sheet in portrait; desktop = left column.
Reference only, never copy Swing code: gui\MainWindow.java (formatGameDate,
status labels, tool handling), gui\ToolPanel or equivalent, and the engine's
MicropolisTool, ToolStroke (beginStroke, dragTo, getPreview, apply) and
ToolResult (SUCCESS, NONE, UH_OH, INSUFFICIENT_FUNDS).

## Task list (in order; commit after each)
0. Shared game layer in :engine, package za.co.nieto.nietocity.game (pure
   Java 8): GameController owns the engine thread and the selected tool;
   beginStroke/dragTo/apply run on the engine thread; exposes an immutable
   StatusSnapshot (game date string as the original formats it: month and
   year from cityTime, funds, population, tool name, tool cost) and a message
   queue fed by the engine's cityMessage listener. Copy the English
   CityMessages, StatusMessages and GuiStrings .properties from tools\
   strings\ into engine resources (GPL header kept) and load them with
   ResourceBundle. JUnit: a road costs 10 and funds drop by 10; with funds 0
   the result is INSUFFICIENT_FUNDS and the map is unchanged; a residential
   zone on water is UH_OH; date formatting for cityTime 0 and 48.
1. Icons: copy the 32 ic*.png files into engine/src/main/resources/tools/
   named <TOOLNAME>.png and <TOOLNAME>_hi.png matching MicropolisTool names.
   Add the origin to THIRD_PARTY.md.
2. Android status bar (top, classic order) bound to StatusSnapshot once per
   second, plus a one-line message ticker under it showing the latest engine
   message for about 4 s. Retire the Phase 2 overlay.
3. Android tool palette: 16 tools with icons, selected tool shown with the
   _hi icon and its cost; landscape = scrollable left column, portrait =
   bottom sheet that collapses to a thin strip. Tapping the selected tool
   again deselects it (pan mode).
4. Android placement. With a tool selected: one-finger tap places, one-finger
   drag draws a stroke (roads, rail, wire, bulldozer, parks) with the
   ToolPreview drawn as a translucent overlay and applied on release; two
   fingers pan; pinch still steps zoom. With no tool selected one finger
   pans as in Phase 2. UH_OH and INSUFFICIENT_FUNDS show a short message in
   the ticker (the sounds come in Phase 5). Long press = query tool
   regardless of selection.
5. Query dialog (both platforms): a small panel showing the zone name and
   the status lines the original shows (density, land value, crime,
   pollution, growth) from StatusMessages, closed by tap or Esc.
6. Desktop (JavaFX 8): top status bar, ticker, left palette column, same
   GameController. Left-drag places or draws a stroke, right-drag or
   Space+drag pans, wheel zooms, right-click = query. Keep the JAR runnable;
   run it once here and report the console only.
7. Gate. gradlew --rerun-tasks :engine:test :desktop:jar assembleDebug from
   the command line; quote the raw tail and the engine test count from the
   XML report. Report APK and JAR paths with timestamps. No file preview.
8. Docs. TASKS.md final status; CHANGES.md entry at the top; README controls
   section for phone and PC; THIRD_PARTY.md updated (icons, strings).

## Definition of done
CC verifies: builds, tests, icon and string resource presence, artefacts.
Andre verifies (you cannot see, never claim these): palette readable at 3x
on the S22, strokes land where the finger is, status bar values change as
the city grows, query panel shows sensible values.

## When you stop
Terse summary: what was built, what surprised you, the raw gate tail, what I
should test on the phone and PC, what Phase 4 (speed control, budget,
evaluation, graphs, mini map overlays) needs from me, and remind me to push.
