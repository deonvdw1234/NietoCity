// NietoCity Android app module.
// Created by Nieto Software. Licensed under the GNU General Public License v3.0.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "za.co.nieto.nietocity"
    compileSdk = 34

    defaultConfig {
        applicationId = "za.co.nieto.nietocity"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(libs.androidx.core.ktx)
}

// Keep the latest debug APK in the project root as NietoCity-debug.apk, so the
// newest build is always easy to find. Runs after assembleDebug; overwrites.
tasks.register("publishApkToRoot") {
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        val dest = rootProject.layout.projectDirectory.file("NietoCity-debug.apk").asFile
        if (apk.exists()) {
            apk.copyTo(dest, overwrite = true)
            logger.lifecycle("Published APK to ${dest.absolutePath}")
        }
    }
}
// assembleDebug is registered later by AGP, so match it lazily.
tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy("publishApkToRoot")
}
