# TASKS.md (NietoCity)
Created by Nieto Software

## Phase 1: Scaffold and engine import
- [x] 0. Toolchain proof (JDK 25, Gradle 9.5.1, AGP 8.13.0 — recorded in CLAUDE.md)
- [x] 1. Android project scaffold (za.co.nieto.nietocity, minSdk 26, targetSdk 34, Kotlin DSL); build output redirected to C:\NietoCity-build, no build\ in repo
- [ ] 2. :engine module with MicropolisJ engine package and resources, GPL headers kept
- [ ] 3. :engine compiles with no Android or java.awt dependencies
- [ ] 4. JUnit smoke test (200 ticks, population and funds change) passes
- [ ] 5. MainActivity shows "Engine OK: population X, funds Y"
- [ ] 6. :desktop module (Java 8) runs the same ticks and builds a runnable JAR
- [ ] 7. gradlew assembleDebug succeeds; APK path reported
- [ ] 8. TASKS.md and README.md updated

## Later phases (see NietoCity_Project_Plan_rev2.pdf)
- Phase 2: Tile renderer, Android and desktop
- Phase 3: Tool palette, placement, status bar
- Phase 4: Speed, budget, evaluation, graphs, mini map
- Phase 5: Disasters, sprites, sound
- Phase 6: Map generator, save and load, scenarios
- Phase 7: Start screen, educational mode, about and GPL screens
- Phase 8: Polish, signed APK, Windows exe, publish source
