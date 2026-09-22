@echo off
rem NietoCity - Phase 7 asset preparation (offline, headless).
rem Created by Nieto Software. Licensed under the GNU General Public License v3.0.
rem
rem Regenerates the shippable assets from the repo-root originals WITHOUT
rem touching them:
rem   * splash_exit.png -> compressed splash_exit.jpg (app + desktop),
rem   * AppIcon.png      -> the Android adaptive launcher icon (all densities).
rem The originals (Nieto Logo Animated.mp4, splash_exit.png, AppIcon.png) stay
rem as-is; the .mp4 is copied verbatim into res/raw and desktop resources.
rem
rem Run from the repo root with the Android Studio JBR (JDK) on JAVA_HOME.

setlocal
set ROOT=%~dp0..
set OUT=%TEMP%\nieto-assetprep
if not exist "%OUT%" mkdir "%OUT%"

copy /y "%ROOT%\Nieto Logo Animated.mp4" "%ROOT%\app\src\main\res\raw\nieto_logo_animated.mp4"
copy /y "%ROOT%\Nieto Logo Animated.mp4" "%ROOT%\desktop\src\main\resources\nieto_logo_animated.mp4"

"%JAVA_HOME%\bin\javac" -d "%OUT%" "%ROOT%\scripts\AssetPrep.java"
"%JAVA_HOME%\bin\java" -cp "%OUT%" AssetPrep "%ROOT%"

endlocal
