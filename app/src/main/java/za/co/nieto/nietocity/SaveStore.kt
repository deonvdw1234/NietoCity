/*
 * NietoCity - Android save storage (private app storage).
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import za.co.nieto.nietocity.game.CityFile
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.SaveMeta
import za.co.nieto.nietocity.render.MiniMap
import java.io.File

/** One saved city: its file base name, metadata, and a mini-map thumbnail. */
class SaveSlot(val base: String, val meta: SaveMeta, val thumbnail: Bitmap?)

/**
 * Saves cities in private app storage (<filesDir>/saves): a standard classic
 * <base>.cty, a <base>.png mini-map thumbnail and a <base>.meta record (name,
 * date, population, funds). The .cty stays standard bytes with nothing appended.
 */
class SaveStore(context: Context) {

    private val dir = File(context.filesDir, "saves").apply { mkdirs() }

    fun ctyFile(base: String): File = File(dir, "$base.cty")

    fun exists(displayName: String): Boolean = ctyFile(slug(displayName)).exists()

    /** Save the running city under the given display name (writes .cty, .png, .meta). */
    fun save(displayName: String, controller: GameController) {
        val base = slug(displayName)
        val engine = controller.engine
        CityFile.save(engine, ctyFile(base))

        val pop: Int
        val funds: Int
        val w: Int
        val h: Int
        val px: IntArray
        synchronized(engine) {
            pop = engine.cityPopulation
            funds = engine.budget.totalFunds
            w = engine.width
            h = engine.height
            px = MiniMap.overviewPixels(engine)
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        File(dir, "$base.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        SaveMeta(displayName, System.currentTimeMillis(), pop, funds).write(File(dir, "$base.meta"))
    }

    /** All saved slots, newest first. */
    fun list(): List<SaveSlot> {
        val ctys = dir.listFiles { f -> f.name.endsWith(".cty") } ?: return emptyList()
        return ctys.map { cty ->
            val base = cty.name.removeSuffix(".cty")
            val meta = SaveMeta.read(File(dir, "$base.meta"), base)
            val png = File(dir, "$base.png")
            val thumb = if (png.exists()) BitmapFactory.decodeFile(png.absolutePath) else null
            SaveSlot(base, meta, thumb)
        }.sortedByDescending { it.meta.dateMillis }
    }

    fun delete(base: String) {
        File(dir, "$base.cty").delete()
        File(dir, "$base.png").delete()
        File(dir, "$base.meta").delete()
    }

    /** A safe file base name for a display name (the display name is kept in .meta). */
    private fun slug(name: String): String {
        val trimmed = name.trim().ifEmpty { "city" }
        return trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
