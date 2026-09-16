/*
 * NietoCity - Android city renderer.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
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
import micropolisj.engine.TileConstants
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.render.TileIndex
import za.co.nieto.nietocity.render.Viewport

/**
 * A SurfaceView that renders the Micropolis map through the shared GameController
 * using the render core. The simulation runs on the GameController's engine
 * thread; a separate render thread redraws only when something changes.
 * Integer zoom with nearest-neighbour sampling keeps the pixel art crisp.
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

    private val src = Rect()
    private val dst = Rect()

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleAccum = 1f

    private var controller: GameController? = null
    private var viewport: Viewport? = null

    private val renderLock = Object()
    @Volatile private var dirty = true
    private var renderThread: RenderThread? = null

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

    /** Attach the shared controller and start rendering its city. */
    fun setController(controller: GameController) {
        this.controller = controller
        controller.setFrameCallback { requestRender() }
        if (width > 0 && height > 0) {
            initViewport(width, height)
        }
        requestRender()
    }

    fun getViewport(): Viewport? = viewport

    fun requestRender() {
        synchronized(renderLock) {
            dirty = true
            renderLock.notifyAll()
        }
    }

    private fun initViewport(w: Int, h: Int) {
        val c = controller?.getEngine() ?: return
        val vp = viewport
        if (vp == null) {
            val fresh = Viewport(c.width, c.height, w, h)
            fresh.setZoom(DEFAULT_ZOOM)
            fresh.centreOnTile(c.width / 2, c.height / 2)
            viewport = fresh
        } else {
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
        val gc = controller ?: return
        val c = gc.getEngine()
        val vp = viewport ?: return

        val firstCol: Int
        val lastCol: Int
        val firstRow: Int
        val lastRow: Int
        val cycle: Int

        synchronized(c) {
            firstCol = vp.firstVisibleCol()
            lastCol = vp.lastVisibleCol()
            firstRow = vp.firstVisibleRow()
            lastRow = vp.lastVisibleRow()
            cycle = gc.animationCycle()

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

    companion object {
        const val DEFAULT_ZOOM = 3
        private const val ZOOM_STEP_IN = 1.30f
        private const val ZOOM_STEP_OUT = 0.77f
    }
}
