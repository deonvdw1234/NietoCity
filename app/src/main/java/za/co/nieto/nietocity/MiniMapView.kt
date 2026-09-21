/*
 * NietoCity - Android mini map (overview) panel.
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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.render.MapOverlay
import za.co.nieto.nietocity.render.MiniMap
import za.co.nieto.nietocity.render.Viewport

/**
 * A small overview of the whole city drawn one pixel per tile (the shared render
 * core builds the bitmap). It shows the current viewport as a rectangle; tapping
 * or dragging on it re-centres the main view. It sits in a corner over the map
 * and never covers the top status bar.
 */
class MiniMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var controller: GameController? = null
    private var viewport: Viewport? = null
    private var overlay: MapOverlay = MapOverlay.NONE

    private var bitmap: Bitmap? = null
    private var pixels = IntArray(0)
    private var mapW = 0
    private var mapH = 0

    private val bmpPaint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val framePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.YELLOW
    }
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(200, 255, 255, 255)
    }
    private val bgPaint = Paint().apply { color = Color.argb(200, 0, 0, 0) }

    private val src = Rect()
    private val dst = RectF()

    /** Called after a tap/drag re-centres the main view (so it can repaint). */
    var onRecenter: (() -> Unit)? = null

    fun bind(controller: GameController, viewport: Viewport?) {
        this.controller = controller
        this.viewport = viewport
        rebuild()
    }

    fun setViewport(vp: Viewport?) {
        viewport = vp
        invalidate()
    }

    fun setOverlay(o: MapOverlay) {
        overlay = o
        rebuild()
    }

    /** Rebuild the overview bitmap from the current map, then repaint. */
    fun rebuild() {
        val engine = controller?.getEngine() ?: return
        synchronized(engine) {
            mapW = engine.width
            mapH = engine.height
            pixels = MiniMap.overviewPixels(engine, overlay)
        }
        if (mapW <= 0 || mapH <= 0) return
        var bmp = bitmap
        if (bmp == null || bmp.width != mapW || bmp.height != mapH) {
            bmp = Bitmap.createBitmap(mapW, mapH, Bitmap.Config.ARGB_8888)
            bitmap = bmp
        }
        bmp.setPixels(pixels, 0, mapW, 0, 0, mapW, mapH)
        invalidate()
    }

    // The rectangle (in this view) that the overview bitmap is drawn into, kept
    // aspect-correct and centred so overview<->tile mapping stays exact.
    private fun contentRect(): RectF {
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (mapW <= 0 || mapH <= 0) return RectF(0f, 0f, vw, vh)
        val scale = minOf(vw / mapW, vh / mapH)
        val w = mapW * scale
        val h = mapH * scale
        val left = (vw - w) / 2f
        val top = (vh - h) / 2f
        return RectF(left, top, left + w, top + h)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val bmp = bitmap ?: return
        val rect = contentRect()
        src.set(0, 0, mapW, mapH)
        dst.set(rect)
        canvas.drawBitmap(bmp, src, dst, bmpPaint)
        canvas.drawRect(rect, borderPaint)

        val vp = viewport ?: return
        val sx = rect.width() / mapW
        val sy = rect.height() / mapH
        val fx = rect.left + vp.firstVisibleCol() * sx
        val fy = rect.top + vp.firstVisibleRow() * sy
        val fx2 = rect.left + (vp.lastVisibleCol() + 1) * sx
        val fy2 = rect.top + (vp.lastVisibleRow() + 1) * sy
        canvas.drawRect(fx, fy, fx2, fy2, framePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                recenterAt(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun recenterAt(px: Float, py: Float) {
        val vp = viewport ?: return
        val rect = contentRect()
        val imgW = rect.width().toInt()
        val imgH = rect.height().toInt()
        val tileX = MiniMap.tileXForOverview((px - rect.left).toInt(), imgW, mapW)
        val tileY = MiniMap.tileYForOverview((py - rect.top).toInt(), imgH, mapH)
        vp.centreOnTile(tileX, tileY)
        invalidate()
        onRecenter?.invoke()
    }
}
