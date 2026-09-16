// NietoCity :desktop module - the Windows / Java 8 desktop entry point.
// Created by Nieto Software. Licensed under the GNU General Public License v3.0.
//
// Pure Java, targeting Java 8 (class-file major version 52) so it runs on
// Windows 7 with a Java 8 runtime. Depends on :engine. `gradlew :desktop:jar`
// produces a single runnable JAR with the engine classes and data files bundled
// (but NOT jfxrt.jar - JavaFX comes from the Java 8 Full runtime).
//
// Uses a JDK 8 "Full" toolchain (BellSoft Liberica) so that (a) it compiles to
// Java 8 bytecode natively and (b) JavaFX 8 (jfxrt.jar, on the JDK 8 boot
// classpath) is available at compile time without an external dependency.

plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.BELLSOFT)
    }
}

application {
    mainClass.set("za.co.nieto.nietocity.desktop.Main")
}

dependencies {
    implementation(project(":engine"))
}

// Build a self-contained, runnable JAR: our classes + the engine's classes and
// data files (tiles.rc, tiles/aliases.txt), with the Main-Class in the manifest.
tasks.jar {
    // Building the runtime classpath also builds :engine:jar, whose classes and
    // data files we unpack into this JAR. Declaring the dependency keeps task
    // ordering correct (e.g. under --rerun-tasks).
    dependsOn(configurations.runtimeClasspath)
    manifest {
        attributes["Main-Class"] = "za.co.nieto.nietocity.desktop.Main"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
