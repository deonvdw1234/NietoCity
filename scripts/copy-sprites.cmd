@echo off
REM ===========================================================================
REM copy-sprites.cmd  -  Copy the sprite frame images for NietoCity.
REM Created by Nieto Software.
REM
REM One-off asset step (Phase 5). Copies the 61 mobile-sprite frame images from
REM the MicropolisJ open-source resources into the :engine resources so the
REM renderer can draw the city's live sprites (train, car, helicopter,
REM aeroplane, ship, monster, tornado, explosion):
REM     FROM  tools\micropolis-java-<commit>\resources\obj<id>-<frame>.png
REM     TO    engine\src\main\resources\sprites\obj<id>-<frame>.png
REM
REM The frames are grouped by SpriteKind objectId 1..8 and numbered 0..numFrames-1
REM (SpriteKind: TRA 1/5, COP 2/8, AIR 3/11, SHI 4/8, GOD 5/16, TOR 6/3, EXP 7/6,
REM BUS 8/4 = 61 files). The images are GPLv3 art from the Micropolis open-source
REM release via MicropolisJ (no Maxis trademark material); see THIRD_PARTY.md.
REM
REM tools\ is git-ignored; unzip the MicropolisJ source there first (the commit
REM is recorded in THIRD_PARTY.md). The copied outputs are committed to the repo,
REM so a normal build never needs tools\. Re-run only if the sprite art changes.
REM ===========================================================================
setlocal
set SRC=tools\micropolis-java-9f6ddb4b5f36a005fe4c4f77488d7969eabf0797\resources
set DST=engine\src\main\resources\sprites
if not exist "%DST%" mkdir "%DST%"
copy /y "%SRC%\obj*.png" "%DST%\" >nul
echo Copied sprite frames to %DST%
endlocal
