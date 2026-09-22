/*
 * NietoCity - text for the About / Licence / How-to-play screens.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.content.Context
import za.co.nieto.nietocity.game.AppInfo
import za.co.nieto.nietocity.game.Education

/**
 * Builds the text shown on the information screens. About and Licence get their
 * full content in tasks 5 and 6; How-to-play in task 4.
 */
object InfoText {

    fun about(context: Context): CharSequence {
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            null
        } ?: AppInfo.VERSION
        return AppInfo.about(version)
    }

    fun licence(context: Context): CharSequence =
        loadResource("/license/LICENSE.txt")
            ?: "Licence text is unavailable in this build."

    fun howToPlay(): CharSequence = Education.fullReference()

    /** Read a bundled UTF-8 text resource from the classpath, or null if absent. */
    private fun loadResource(path: String): String? = try {
        InfoText::class.java.getResourceAsStream(path)?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
    } catch (_: Exception) {
        null
    }
}
