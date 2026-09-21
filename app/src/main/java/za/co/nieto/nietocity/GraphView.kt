/*
 * NietoCity - Android history graphs (drawn natively).
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.GraphData

/**
 * Draws the six city-history series (residential, commercial, industrial, crime,
 * pollution, cash flow) as line graphs with a colour legend, for either the
 * 10-year or 120-year window. No chart library - the shared GraphData supplies
 * the scaled points and this paints the axes, lines and legend directly.
 */
class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var controller: GameController? = null
    private var longRange = false

    private val axisPaint = Paint().apply { color = Color.argb(160, 255, 255, 255); strokeWidth = 2f }
    private val linePaint = Paint().apply { isAntiAlias = true; strokeWidth = 3f; style = Paint.Style.STROKE }
    private val textPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; textSize = sp(12f) }
    private val swatchPaint = Paint()
    private val bgPaint = Paint().apply { color = Color.rgb(24, 24, 24) }

    fun bind(controller: GameController) {
        this.controller = controller
        invalidate()
    }

    fun setLongRange(long: Boolean) {
        longRange = long
        invalidate()
    }

    fun isLongRange(): Boolean = longRange

    override fun onDraw(canvas: Canvas) {
        val gc = controller ?: return
        val city = gc.engine

        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val series = GraphData.Series.values()

        // Legend across the top (two rows if needed).
        val pad = dp(10f)
        val legendTop = pad
        val swatch = dp(12f)
        val colGap = dp(12f)
        var lx = pad
        var ly = legendTop
        for (s in series) {
            val label = s.label()
            val labelW = textPaint.measureText(label)
            if (lx + swatch + dp(4f) + labelW > w - pad) {
                lx = pad
                ly += swatch + dp(8f)
            }
            swatchPaint.color = s.colorArgb()
            canvas.drawRect(lx, ly, lx + swatch, ly + swatch, swatchPaint)
            canvas.drawText(label, lx + swatch + dp(4f), ly + swatch, textPaint)
            lx += swatch + dp(4f) + labelW + colGap
        }

        val title = if (longRange) "120 years" else "10 years"
        canvas.drawText(title, w - pad - textPaint.measureText(title), ly + swatch, textPaint)

        // Plot area below the legend.
        val plotLeft = pad
        val plotTop = ly + swatch + dp(12f)
        val plotRight = w - pad
        val plotBottom = h - pad
        if (plotBottom <= plotTop || plotRight <= plotLeft) return

        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axisPaint)
        canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axisPaint)

        val n = GraphData.POINTS
        val stepX = (plotRight - plotLeft) / (n - 1)
        val plotH = plotBottom - plotTop
        val path = android.graphics.Path()
        for (s in series) {
            val vals = GraphData.normalized(city, s, longRange)
            linePaint.color = s.colorArgb()
            path.reset()
            // Index 0 is the newest sample -> draw it at the right edge.
            for (i in 0 until n) {
                val x = plotRight - i * stepX
                val y = plotBottom - vals[i] * plotH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, linePaint)
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}
