@echo off
REM ===========================================================================
REM compose-tiles.cmd  -  Compose the 16x16 tile atlas for NietoCity.
REM Created by Nieto Software.
REM
REM One-off tool. It compiles MicropolisJ's build_tool (which uses java.awt) from
REM the tools\ source tree OUTSIDE the app modules, then runs it headless to
REM produce the tile atlas that :engine ships:
REM     engine\src\main\resources\16x16\tiles.png
REM     engine\src\main\resources\16x16\tiles.idx
REM
REM No AWT code is added to :engine, :app or :desktop. Only re-run this if the
REM tile recipe (graphics\tiles.rc) or the source art changes; the outputs are
REM committed to the repo so a normal build never needs java.awt.
REM
REM The tools\ source tree must match the MicropolisJ commit recorded in
REM THIRD_PARTY.md. tools\ is git-ignored; unzip MicropolisJ there first.
REM Any JDK works for this step (java.awt is only used here, headless).
REM ===========================================================================
setlocal
set REPO=%~dp0..
set SRCROOT=%REPO%\tools\micropolis-java-9f6ddb4b5f36a005fe4c4f77488d7969eabf0797
set OUTDIR=%REPO%\engine\src\main\resources\16x16
set CLASSES=%TEMP%\nietocity-maketiles

if not exist "%SRCROOT%\src\micropolisj\build_tool\MakeTiles.java" (
  echo ERROR: MicropolisJ source not found under tools\.
  echo Expected: "%SRCROOT%"
  echo Unzip MicropolisJ into tools\ first (see THIRD_PARTY.md^).
  exit /b 1
)

REM Compile MakeTiles and only its transitive dependencies (engine, graphics,
REM XML_Helper) via -sourcepath; this avoids compiling the Swing gui package.
if not exist "%CLASSES%" mkdir "%CLASSES%"
javac -sourcepath "%SRCROOT%\src" -d "%CLASSES%" "%SRCROOT%\src\micropolisj\build_tool\MakeTiles.java"
if errorlevel 1 ( echo ERROR: javac failed & exit /b 1 )

REM Run headless. Working dir = graphics so the image paths in tiles.rc resolve.
REM Default tile size is 16 (no -Dtile_size). Args: <recipe> <output dir>.
if not exist "%OUTDIR%" mkdir "%OUTDIR%"
pushd "%SRCROOT%\graphics"
java -Djava.awt.headless=true -ea -cp "%CLASSES%" micropolisj.build_tool.MakeTiles tiles.rc "%OUTDIR%"
set RC=%errorlevel%
popd
if not "%RC%"=="0" ( echo ERROR: MakeTiles failed with code %RC% & exit /b 1 )

echo.
echo Done. Wrote tiles.png and tiles.idx to:
echo   %OUTDIR%
endlocal
