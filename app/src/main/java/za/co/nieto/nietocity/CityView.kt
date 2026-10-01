/*
 * NietoCity - Android city renderer and input.
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
import micropolisj.engine.ToolPreview
import micropolisj.engine.ToolResult
import micropolisj.engine.SpriteKind
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.TouchRouter
import za.co.nieto.nietocity.render.MapOverlay
import za.co.nieto.nietocity.render.PowerOverlay
import za.co.nieto.nietocity.render.SpriteImages
import za.co.nieto.nietocity.render.TileIndex
import za.co.nieto.nietocity.render.Viewport

/**
 * Renders the map through the shared GameController and handles input:
 *  - no tool selected: one finger pans, pinch zooms, double tap centres.
 *  - a tool selected: one finger taps to place or drags to draw a stroke (with a
 *    translucent preview applied on release); two fingers pan; pinch zooms.
 *  - long press queries the tile regardless of the selected tool.
 * Integer zoom with nearest-neighbour sampling keeps the pixel art crisp.
 */
class CityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val loMask = TileConstants.LOMASK.code
    private val clear = TileConstants.CLEAR.toInt()
    private val tileIndex = TileIndex.loadDefault()
    private val atlas: Bitmap = loadAtlas()
    private val spriteBitmaps: Map<Int, Bitmap> = loadSprites()

    private val tilePaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        isDither = false
    }
    private val previewPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        alpha = 170
    }
    private val tintOk = Paint().apply { color = Color.argb(70, 0, 200, 0) }
    private val tintBad = Paint().apply { color = Color.argb(90, 220, 0, 0) }
    // A thin accent border round the map while a tool is armed (none in Pan), in
    // the palette's highlight colour. The map sits below the status bar, so the
    // border never covers it.
    private val armedBorder = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.rgb(255, 210, 77)
        strokeWidth = 3 * resources.displayMetrics.density
        isAntiAlias = false
    }

    private val src = Rect()
    private val dst = Rect()

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleAccum = 1f

    private var controller: GameController? = null
    private var viewport: Viewport? = null

    /** Called (on the UI thread) when a long press queries a tile. */
    var queryListener: ((Int, Int) -> Unit)? = null

    // Stroke / pan state
    @Volatile private var preview: ToolPreview? = null
    private var strokeActive = false
    private var strokeOriginX = 0
    private var strokeOriginY = 0
    private var strokeCurX = 0
    private var strokeCurY = 0
    // The finger's tile waypoints, so a bent drag lays a road that follows the
    // path (GameController splits it into axis-aligned strokes on release).
    private val pathX = ArrayList<Int>()
    private val pathY = ArrayList<Int>()
    private var panning = false
    private var lastPanX = 0f
    private var lastPanY = 0f
    private var suppressUp = false

    private val renderLock = Object()
    @Volatile private var dirty = true
    private var renderThread: RenderThread? = null
    private var snapshot = IntArray(0)
    private var boltSnapshot = BooleanArray(0)
    private var overlaySnapshot = IntArray(0)

    // Live engine sprites, snapshotted under the engine lock so drawing never
    // reads a sprite the sim thread is moving.
    @Volatile private var spriteFrames: List<SpriteImages.Frame> = emptyList()

    // The active data overlay (None by default). Its translucent tint is drawn
    // over the tile art (the shared MapOverlay computes the colour per tile).
    @Volatile private var overlay: MapOverlay = MapOverlay.NONE
    private val overlayPaint = Paint().apply { isAntiAlias = false }
    private val overlayAlpha = 0xB0

    init {
        holder.addCallback(this)
        isClickable = true
        isFocusable = true
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

    /** Preload every sprite frame, keyed by objectId*100 + frameIndex. */
    private fun loadSprites(): Map<Int, Bitmap> {
        val map = HashMap<Int, Bitmap>()
        val opts = BitmapFactory.Options().apply { inScaled = false }
        for (kind in SpriteKind.values()) {
            for (i in 0 until kind.numFrames) {
                val stream = SpriteImages::class.java.getResourceAsStream(
                    SpriteImages.resourcePath(kind, i)
                ) ?: continue
                stream.use {
                    val bmp = BitmapFactory.decodeStream(it, null, opts)
                    if (bmp != null) map[spriteKey(kind.objectId, i)] = bmp
                }
            }
        }
        return map
    }

    private fun spriteKey(objectId: Int, frameIndex: Int): Int = objectId * 100 + frameIndex

    fun setController(controller: GameController) {
        this.controller = controller
        controller.setFrameCallback { requestRender() }
        if (width > 0 && height > 0) {
            initViewport(width, height)
        }
        requestRender()
    }

    fun getViewport(): Viewport? = viewport

    /** Recentre the view on the middle of the map (e.g. after starting a new city). */
    fun resetViewportToMapCentre() {
        val c = controller?.getEngine() ?: return
        val vp = viewport ?: return
        vp.centreOnTile(c.width / 2, c.height / 2)
        requestRender()
    }

    /** Set the data overlay tinting the map (MapOverlay.NONE turns it off). */
    fun setMapOverlay(o: MapOverlay) {
        overlay = o
        requestRender()
    }

    fun getMapOverlay(): MapOverlay = overlay

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

    // --- input ---

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        // The tool is read live from the controller (the single source of truth)
        // and TouchRouter decides what the touch does.
        val tool = controller?.getTool()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                suppressUp = false
                lastPanX = event.x
                lastPanY = event.y
                if (TouchRouter.decide(tool, 1, TouchRouter.Phase.DOWN) == TouchRouter.Action.BUILD) {
                    beginStroke(event.x, event.y)
                } else {
                    panning = true
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Second finger: TouchRouter says pan (any tool); abandon any stroke.
                if (TouchRouter.decide(tool, event.pointerCount, TouchRouter.Phase.DOWN) == TouchRouter.Action.PAN) {
                    cancelStroke()
                    panning = true
                    lastPanX = event.getX(0)
                    lastPanY = event.getY(0)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val fx = (event.getX(0) + event.getX(1)) / 2f
                    val fy = (event.getY(0) + event.getY(1)) / 2f
                    viewport?.panBy((lastPanX - fx).toInt(), (lastPanY - fy).toInt())
                    lastPanX = fx
                    lastPanY = fy
                    requestRender()
                } else if (strokeActive) {
                    extendStroke(event.x, event.y)
                } else if (panning) {
                    viewport?.panBy((lastPanX - event.x).toInt(), (lastPanY - event.y).toInt())
                    lastPanX = event.x
                    lastPanY = event.y
                    requestRender()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // Dropping back toward one finger: keep panning with a remaining one.
                val remaining = if (event.actionIndex == 0) 1 else 0
                if (remaining < event.pointerCount) {
                    lastPanX = event.getX(remaining)
                    lastPanY = event.getY(remaining)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (strokeActive && !suppressUp) {
                    applyStroke()
                }
                cancelStroke()
                panning = false
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelStroke()
                panning = false
            }
        }
        return true
    }

    private fun beginStroke(px: Float, py: Float) {
        val vp = viewport ?: return
        strokeOriginX = vp.tileXAt(px.toInt())
        strokeOriginY = vp.tileYAt(py.toInt())
        strokeCurX = strokeOriginX
        strokeCurY = strokeOriginY
        pathX.clear()
        pathY.clear()
        pathX.add(strokeOriginX)
        pathY.add(strokeOriginY)
        strokeActive = true
        updatePreview()
    }

    private fun extendStroke(px: Float, py: Float) {
        val vp = viewport ?: return
        val tx = vp.tileXAt(px.toInt())
        val ty = vp.tileYAt(py.toInt())
        if (tx != strokeCurX || ty != strokeCurY) {
            strokeCurX = tx
            strokeCurY = ty
            pathX.add(tx)
            pathY.add(ty)
            updatePreview()
        }
    }

    private fun updatePreview() {
        preview = controller?.previewPath(pathX.toIntArray(), pathY.toIntArray())
        requestRender()
    }

    private fun applyStroke() {
        // A one-shot tool returning to Pan reaches the UI via the tool listener.
        controller?.applyPath(pathX.toIntArray(), pathY.toIntArray(), null)
    }

    private fun cancelStroke() {
        if (strokeActive || preview != null) {
            strokeActive = false
            preview = null
            pathX.clear()
            pathY.clear()
            requestRender()
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
                boltSnapshot = BooleanArray(cols * rows)
                overlaySnapshot = IntArray(cols * rows)
            }
            val ov = overlay
            val overlayOn = ov != MapOverlay.NONE
            var i = 0
            for (row in firstRow..lastRow) {
                for (col in firstCol..lastCol) {
                    val tile = c.getTile(col, row).code and loMask
                    snapshot[i] = tile
                    // Blink a lightning bolt over unpowered zone centres (shared core).
                    boltSnapshot[i] = PowerOverlay.showBolt(c, col, row, cycle)
                    overlaySnapshot[i] = if (overlayOn) ov.colorAt(c, col, row, tile) else 0
                    i++
                }
            }
            spriteFrames = SpriteImages.capture(c)
        }

        // Earthquake: a small, decaying, purely-visual shake of everything drawn
        // (the map coordinates are untouched; the black fill above hides the edges).
        val shake = gc.shakeIntensity()
        val shaking = shake > 0.0
        if (shaking) {
            canvas.save()
            val mag = (shake * SHAKE_MAX_PX).toFloat()
            val dx = ((Math.random() - 0.5) * 2.0).toFloat() * mag
            val dy = ((Math.random() - 0.5) * 2.0).toFloat() * mag
            canvas.translate(dx, dy)
        }

        val tp = vp.tilePx()
        val boltImage = tileIndex.hasImage(PowerOverlay.LIGHTNINGBOLT)
        var i = 0
        for (row in firstRow..lastRow) {
            val screenY = vp.tileScreenY(row)
            for (col in firstCol..lastCol) {
                val tile = snapshot[i]
                val bolt = boltSnapshot[i]
                val ovColor = overlaySnapshot[i]
                i++
                val screenX = vp.tileScreenX(col)
                if (tileIndex.hasImage(tile)) {
                    val yOff = tileIndex.frameOffsetY(tile, cycle)
                    src.set(0, yOff, TileIndex.TILE_SIZE, yOff + TileIndex.TILE_SIZE)
                    dst.set(screenX, screenY, screenX + tp, screenY + tp)
                    canvas.drawBitmap(atlas, src, dst, tilePaint)
                }
                if (bolt && boltImage) {
                    val yOff = tileIndex.frameOffsetY(PowerOverlay.LIGHTNINGBOLT, cycle)
                    src.set(0, yOff, TileIndex.TILE_SIZE, yOff + TileIndex.TILE_SIZE)
                    dst.set(screenX, screenY, screenX + tp, screenY + tp)
                    canvas.drawBitmap(atlas, src, dst, tilePaint)
                }
                if (ovColor != 0) {
                    overlayPaint.color = (ovColor and 0x00FFFFFF) or (overlayAlpha shl 24)
                    canvas.drawRect(
                        screenX.toFloat(), screenY.toFloat(),
                        (screenX + tp).toFloat(), (screenY + tp).toFloat(), overlayPaint
                    )
                }
            }
        }

        drawSprites(canvas, vp)
        drawPreview(canvas, vp, cycle, firstCol, lastCol, firstRow, lastRow)
        if (shaking) {
            canvas.restore()
            // The sim fires a frame every tick, so the shake re-jitters and decays
            // without a busy redraw loop here.
        }
        // Armed-tool border, read live from the controller (the tool listener
        // requests a redraw whenever the tool changes).
        if (gc.getTool() != null) {
            val half = armedBorder.strokeWidth / 2f
            canvas.drawRect(half, half, width - half, height - half, armedBorder)
        }
    }

    private fun drawSprites(canvas: Canvas, vp: Viewport) {
        val frames = spriteFrames
        if (frames.isEmpty()) return
        val z = vp.zoom
        val viewW = vp.viewWidthPx
        val viewH = vp.viewHeightPx
        for (f in frames) {
            val bmp = spriteBitmaps[spriteKey(f.objectId, f.frameIndex)] ?: continue
            val sx = (f.x + f.offx) * z - vp.scrollX
            val sy = (f.y + f.offy) * z - vp.scrollY
            val w = bmp.width * z
            val h = bmp.height * z
            if (sx + w < 0 || sy + h < 0 || sx > viewW || sy > viewH) continue
            dst.set(sx, sy, sx + w, sy + h)
            canvas.drawBitmap(bmp, null, dst, tilePaint)
        }
    }

    private fun drawPreview(
        canvas: Canvas, vp: Viewport, cycle: Int,
        firstCol: Int, lastCol: Int, firstRow: Int, lastRow: Int
    ) {
        val pv = preview ?: return
        val tiles = pv.tiles
        val tp = vp.tilePx()
        val bad = pv.toolResult == ToolResult.UH_OH || pv.toolResult == ToolResult.INSUFFICIENT_FUNDS
        val tint = if (bad) tintBad else tintOk
        for (ry in tiles.indices) {
            val rowArr = tiles[ry]
            for (rx in rowArr.indices) {
                val cValue = rowArr[rx].toInt()
                if (cValue == clear) continue
                val mapX = strokeOriginX + rx - pv.offsetX
                val mapY = strokeOriginY + ry - pv.offsetY
                if (mapX < firstCol || mapX > lastCol || mapY < firstRow || mapY > lastRow) continue
                val screenX = vp.tileScreenX(mapX)
                val screenY = vp.tileScreenY(mapY)
                val masked = cValue and loMask
                if (tileIndex.hasImage(masked)) {
                    val yOff = tileIndex.frameOffsetY(masked, cycle)
                    src.set(0, yOff, TileIndex.TILE_SIZE, yOff + TileIndex.TILE_SIZE)
                    dst.set(screenX, screenY, screenX + tp, screenY + tp)
                    canvas.drawBitmap(atlas, src, dst, previewPaint)
                }
                canvas.drawRect(
                    screenX.toFloat(), screenY.toFloat(),
                    (screenX + tp).toFloat(), (screenY + tp).toFloat(), tint
                )
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

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Centre only in pan mode; in place mode the taps place tiles.
            if (controller?.getTool() == null) {
                val vp = viewport ?: return false
                vp.centreOnTile(vp.tileXAt(e.x.toInt()), vp.tileYAt(e.y.toInt()))
                requestRender()
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            // Query regardless of the selected tool; do not also place.
            val tool = controller?.getTool()
            if (TouchRouter.decide(tool, 1, TouchRouter.Phase.LONG_PRESS) != TouchRouter.Action.QUERY) return
            suppressUp = true
            cancelStroke()
            val vp = viewport ?: return
            queryListener?.invoke(vp.tileXAt(e.x.toInt()), vp.tileYAt(e.y.toInt()))
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
        private const val SHAKE_MAX_PX = 6.0
    }
}
