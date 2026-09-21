/*
 * NietoCity - New City map preview thumbnail.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.TerrainConfig
import za.co.nieto.nietocity.render.MiniMap

/**
 * A small preview of a generated map (one pixel per tile via the shared MiniMap /
 * TileColors), shown on the New City screen. It regenerates whenever a terrain
 * control or the seed changes, so the player can see the map before committing.
 */
class MiniMapPreview(context: Context) : View(context) {

    private var bitmap: Bitmap? = null
    private var mapW = 0
    private var mapH = 0

    private val bmpPaint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(200, 255, 255, 255)
    }
    private val bgPaint = Paint().apply { color = Color.argb(200, 0, 0, 0) }
    private val src = Rect()
    private val dst = RectF()

    /** Regenerate the preview for the given level, seed and terrain. */
    fun regenerate(level: Int, seed: Long, cfg: TerrainConfig) {
        val city = GameController.buildCity(level, seed, cfg)
        val w = city.width
        val h = city.height
        val px = synchronized(city) { MiniMap.overviewPixels(city) }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        bitmap = bmp
        mapW = w
        mapH = h
        invalidate()
    }

    /** Release any pending work (no-op for the synchronous version). */
    fun dispose() {}

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val bmp = bitmap ?: return
        val vw = width.toFloat()
        val vh = height.toFloat()
        val scale = minOf(vw / mapW, vh / mapH)
        val w = mapW * scale
        val h = mapH * scale
        val left = (vw - w) / 2f
        val top = (vh - h) / 2f
        src.set(0, 0, mapW, mapH)
        dst.set(left, top, left + w, top + h)
        canvas.drawBitmap(bmp, src, dst, bmpPaint)
        canvas.drawRect(dst, borderPaint)
    }
}
