/*
 * NietoCity - tool palette.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import micropolisj.engine.MicropolisTool
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.GameStrings

/**
 * The 16-tool palette as a grid of finger-sized cells. Each cell is at least
 * 56dp square, shows a 3x nearest-neighbour-scaled icon (crisp), the tool name
 * under it, and the cost on the selected cell. The selected cell uses the _hi
 * icon and a highlight border. Column count is chosen by the caller (4 in the
 * portrait bottom sheet, 1 in the landscape left column).
 */
class ToolPaletteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GridLayout(context, attrs) {

    /** Notified when the selection changes (null = pan mode). */
    var listener: ((MicropolisTool?) -> Unit)? = null

    private val tools = listOf(
        MicropolisTool.BULLDOZER, MicropolisTool.WIRE, MicropolisTool.PARK,
        MicropolisTool.ROADS, MicropolisTool.RAIL,
        MicropolisTool.RESIDENTIAL, MicropolisTool.COMMERCIAL, MicropolisTool.INDUSTRIAL,
        MicropolisTool.FIRE, MicropolisTool.QUERY, MicropolisTool.POLICE,
        MicropolisTool.POWERPLANT, MicropolisTool.NUCLEAR,
        MicropolisTool.STADIUM, MicropolisTool.SEAPORT, MicropolisTool.AIRPORT
    )

    private val plainIcons = HashMap<MicropolisTool, Bitmap>()
    private val hiIcons = HashMap<MicropolisTool, Bitmap>()
    private val cells = HashMap<MicropolisTool, Cell>()
    private var controller: GameController? = null

    init {
        setBackgroundColor(Color.argb(210, 16, 16, 16))
        for (tool in tools) {
            plainIcons[tool] = scaled(loadIcon("${tool.name}.png"))
            hiIcons[tool] = scaled(loadIcon("${tool.name}_hi.png"))
        }
    }

    /** Build the cells laid out in [columns] columns and bind the controller. */
    fun setup(controller: GameController, columns: Int) {
        this.controller = controller
        columnCount = columns
        removeAllViews()
        cells.clear()
        for (tool in tools) {
            val cell = Cell(tool)
            cells[tool] = cell
            addView(cell)
        }
        refresh()
    }

    /** The (scaled) icon for a tool, for the collapsed "Tools" bar. */
    fun iconFor(tool: MicropolisTool): Bitmap? = plainIcons[tool]

    /** Re-read the controller's selection into the cells (after an external change). */
    fun syncSelection() = refresh()

    private fun toggle(tool: MicropolisTool) {
        controller?.toggleTool(tool)
        refresh()
        listener?.invoke(controller?.getTool())
    }

    private fun refresh() {
        val selected = controller?.getTool()
        for (tool in tools) {
            cells[tool]?.setSelectedState(tool == selected)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun scaled(src: Bitmap): Bitmap {
        // 3x nearest-neighbour (filter = false) so pixels stay crisp.
        return Bitmap.createScaledBitmap(src, src.width * 3, src.height * 3, false)
    }

    private fun loadIcon(fileName: String): Bitmap {
        val stream = ToolPaletteView::class.java.getResourceAsStream("/tools/$fileName")
            ?: error("Missing tool icon /tools/$fileName")
        stream.use {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            return BitmapFactory.decodeStream(it, null, opts)
                ?: error("Could not decode /tools/$fileName")
        }
    }

    private fun borderSelected(): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.argb(60, 255, 255, 255))
        g.setStroke(dp(2), Color.rgb(255, 210, 77))
        g.cornerRadius = dp(4).toFloat()
        return g
    }

    private inner class Cell(val tool: MicropolisTool) : LinearLayout(context) {
        private val icon = ImageView(context)
        private val nameLbl = TextView(context)
        private val costLbl = TextView(context)

        init {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            minimumWidth = dp(56)
            minimumHeight = dp(56)
            setPadding(dp(2), dp(4), dp(2), dp(4))

            icon.setImageBitmap(plainIcons[tool])
            nameLbl.text = GameStrings.toolName(tool)
            nameLbl.setTextColor(Color.WHITE)
            nameLbl.textSize = 9f
            nameLbl.gravity = Gravity.CENTER
            nameLbl.maxLines = 1
            costLbl.setTextColor(Color.rgb(255, 224, 128))
            costLbl.textSize = 9f
            costLbl.gravity = Gravity.CENTER
            costLbl.visibility = View.GONE

            addView(icon)
            addView(nameLbl)
            addView(costLbl)

            val lp = GridLayout.LayoutParams()
            lp.setMargins(dp(2), dp(2), dp(2), dp(2))
            layoutParams = lp

            setOnClickListener { toggle(tool) }
        }

        fun setSelectedState(selected: Boolean) {
            icon.setImageBitmap(if (selected) hiIcons[tool] else plainIcons[tool])
            background = if (selected) borderSelected() else null
            val cost = tool.toolCost
            if (selected && cost > 0) {
                costLbl.text = GameStrings.formatFunds(cost)
                costLbl.visibility = View.VISIBLE
            } else {
                costLbl.visibility = View.GONE
            }
        }

        private val MicropolisTool.toolCost: Int get() = getToolCost()
    }
}
