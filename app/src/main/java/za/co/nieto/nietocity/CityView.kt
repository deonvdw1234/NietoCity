/*
 * NietoCity - Android city renderer.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file.
 */
package za.co.nieto.nietocity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import micropolisj.engine.MapListener
import micropolisj.engine.MapState
import micropolisj.engine.Micropolis
import micropolisj.engine.Speed
import micropolisj.engine.Sprite
import micropolisj.engine.TileConstants
import za.co.nieto.nietocity.render.AnimationClock
import za.co.nieto.nietocity.render.TileIndex
import za.co.nieto.nietocity.render.Viewport

/**
 * A SurfaceView that renders the Micropolis map using the shared render core.
 * The simulation runs on its own thread (AnimationClock); a separate render
 * thread redraws only when something changes (animation tick, map change, or a
 * pan/zoom), and idles otherwise. Integer zoom with nearest-neighbour sampling
 * keeps the pixel art crisp.
 */
class CityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val loMask = TileConstants.LOMASK.code
    private val tileIndex = TileIndex.loadDefault()
    private val atlas: Bitmap = loadAtlas()

    private val tilePaint = Paint().apply {
        isFilterBitmap = false   // nearest-neighbour: crisp pixels
        isAntiAlias = false
        isDither = false
    }

    private val overlayPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 15f * resources.displayMetrics.scaledDensity
    }
    private val overlayBgPaint = Paint().apply {
        isAntiAlias = true
        color = Color.argb(140, 0, 0, 0)
    }

    private val src = Rect()
    private val dst = Rect()

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleAccum = 1f

    private var city: Micropolis? = null
    private var viewport: Viewport? = null
    private var clock: AnimationClock? = null

    private val renderLock = Object()
    @Volatile private var dirty = true
    private var renderThread: RenderThread? = null

    // Reused snapshot of the visible tiles so we don't hold the engine lock while drawing.
    private var snapshot = IntArray(0)

    init {
        holder.addCallback(this)
        isClickable = true
        isFocusable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun loadAtlas(): Bitmap {
        val stream = TileIndex::class.java.getResourceAsStream("/16x16/tiles.png")
            ?: error("Missing atlas resource /16x16/tiles.png")
        stream.use {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            return BitmapFactory.decodeStream(it, null, opts)
                ?: error("Could not decode /16x16/tiles.png")
        }
    }

    /** Attach the city to render. Safe to call before or after the surface exists. */
    fun setCity(city: Micropolis) {
        clock?.stop()
        this.city = city
        city.addMapListener(RenderMapListener())

        if (width > 0 && height > 0) {
            initViewport(width, height)
        }
        clock = AnimationClock(city, AnimationClock.Listener { requestRender() }).apply {
            setSpeed(Speed.NORMAL)
        }
        requestRender()
    }

    fun getCity(): Micropolis? = city

    /** Start the simulation clock (call from Activity.onResume). */
    fun resumeEngine() {
        clock?.start()
    }

    /** Stop the simulation clock (call from Activity.onPause). */
    fun pauseEngine() {
        clock?.stop()
    }

    fun getViewport(): Viewport? = viewport

    fun requestRender() {
        synchronized(renderLock) {
            dirty = true
            renderLock.notifyAll()
        }
    }

    private fun initViewport(w: Int, h: Int) {
        val c = city ?: return
        val vp = viewport
        if (vp == null) {
            val fresh = Viewport(c.width, c.height, w, h)
            fresh.setZoom(DEFAULT_ZOOM)
            fresh.centreOnTile(c.width / 2, c.height / 2)
            viewport = fresh
        } else {
            // Rotation / resize: keep the tile under the view centre centred.
            val centreX = vp.tileXAt(vp.viewWidthPx / 2)
            val centreY = vp.tileYAt(vp.viewHeightPx / 2)
            vp.setViewSize(w, h)
            vp.centreOnTile(centreX, centreY)
        }
    }

    // --- SurfaceHolder.Callback ---

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderThread = RenderThread().also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        initViewport(width, height)
        requestRender()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val t = renderThread
        renderThread = null
        t?.let {
            it.stopRendering()
            try {
                it.join(1000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun drawFrame(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        val c = city ?: return
        val vp = viewport ?: return

        val firstCol: Int
        val lastCol: Int
        val firstRow: Int
        val lastRow: Int
        val cycle: Int
        val pop: Int
        val funds: Int

        synchronized(c) {
            firstCol = vp.firstVisibleCol()
            lastCol = vp.lastVisibleCol()
            firstRow = vp.firstVisibleRow()
            lastRow = vp.lastVisibleRow()
            cycle = c.animationCycle
            pop = c.cityPopulation
            funds = c.budget.totalFunds

            val cols = lastCol - firstCol + 1
            val rows = lastRow - firstRow + 1
            if (snapshot.size < cols * rows) {
                snapshot = IntArray(cols * rows)
            }
            var i = 0
            for (row in firstRow..lastRow) {
                for (col in firstCol..lastCol) {
                    snapshot[i++] = c.getTile(col, row).code and loMask
                }
            }
        }

        val tp = vp.tilePx()
        var i = 0
        for (row in firstRow..lastRow) {
            val screenY = vp.tileScreenY(row)
            for (col in firstCol..lastCol) {
                val tile = snapshot[i++]
                if (!tileIndex.hasImage(tile)) continue
                val yOff = tileIndex.frameOffsetY(tile, cycle)
                val screenX = vp.tileScreenX(col)
                src.set(0, yOff, TileIndex.TILE_SIZE, yOff + TileIndex.TILE_SIZE)
                dst.set(screenX, screenY, screenX + tp, screenY + tp)
                canvas.drawBitmap(atlas, src, dst, tilePaint)
            }
        }

        drawOverlay(canvas, pop, funds)
    }

    private fun drawOverlay(canvas: Canvas, pop: Int, funds: Int) {
        val text = "NietoCity Phase 2: pop $pop, funds $funds"
        val pad = 8f * resources.displayMetrics.density
        val textW = overlayPaint.measureText(text)
        val fm = overlayPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        canvas.drawRect(0f, 0f, textW + 2 * pad, textH + 2 * pad, overlayBgPaint)
        canvas.drawText(text, pad, pad - fm.ascent, overlayPaint)
    }

    private inner class RenderThread : Thread("nieto-render") {
        @Volatile private var running = true

        fun stopRendering() {
            synchronized(renderLock) {
                running = false
                renderLock.notifyAll()
            }
        }

        override fun run() {
            while (running) {
                synchronized(renderLock) {
                    while (running && !dirty) {
                        try {
                            renderLock.wait()
                        } catch (e: InterruptedException) {
                            return
                        }
                    }
                    dirty = false
                }
                if (!running) break

                var canvas: Canvas? = null
                try {
                    canvas = holder.lockCanvas()
                    if (canvas != null) {
                        drawFrame(canvas)
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            }
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
        ): Boolean {
            if (scaleDetector.isInProgress) return false
            viewport?.let {
                it.panBy(distanceX.toInt(), distanceY.toInt())
                requestRender()
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val vp = viewport ?: return false
            vp.centreOnTile(vp.tileXAt(e.x.toInt()), vp.tileYAt(e.y.toInt()))
            requestRender()
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scaleAccum = 1f
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val vp = viewport ?: return true
            scaleAccum *= detector.scaleFactor
            // Step the integer zoom once the pinch crosses a threshold, then reset.
            if (scaleAccum >= ZOOM_STEP_IN) {
                vp.setZoom(vp.zoom + 1)
                scaleAccum = 1f
                requestRender()
            } else if (scaleAccum <= ZOOM_STEP_OUT) {
                vp.setZoom(vp.zoom - 1)
                scaleAccum = 1f
                requestRender()
            }
            return true
        }
    }

    private inner class RenderMapListener : MapListener {
        override fun mapAnimation() = requestRender()
        override fun mapOverlayDataChanged(overlayDataType: MapState?) { /* overlays: later phase */ }
        override fun spriteMoved(sprite: Sprite?) = requestRender()
        override fun tileChanged(xpos: Int, ypos: Int) = requestRender()
        override fun wholeMapChanged() = requestRender()
    }

    companion object {
        const val DEFAULT_ZOOM = 3
        private const val ZOOM_STEP_IN = 1.30f
        private const val ZOOM_STEP_OUT = 0.77f
    }
}
