// NietoCity :engine module - the pure-Java Micropolis simulation.
// Created by Nieto Software. Licensed under the GNU General Public License v3.0.
//
// This module contains the micropolisj.engine package (Jason Long's MicropolisJ,
// GPLv3) plus the data files the engine reads at runtime. It must stay free of
// any Android or java.awt dependency and must compile to Java 8 bytecode
// (class-file major version 52) so the identical engine runs on both Android
// (minSdk 26) and desktop Java 8 (Windows 7).

plugins {
    `java-library`
}

tasks.withType<JavaCompile>().configureEach {
    // Target Java 8 without needing a separate JDK 8 install: --release 8 on the
    // JBR's modern javac emits major version 52 and enforces the Java 8 API.
    options.release.set(8)
    // Silence only the "source/target value 8 is obsolete" notes.
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    testImplementation(libs.junit)
}
