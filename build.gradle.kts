// NietoCity root build script.
// Created by Nieto Software. Licensed under the GNU General Public License v3.0.
//
// Build output is redirected entirely outside the repo (which lives in Dropbox)
// so Dropbox never syncs gigabytes of build artefacts. Every module writes to
// C:\NietoCity-build\<module>.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    layout.buildDirectory.set(file("C:/NietoCity-build/${project.name}"))
}
