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
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import micropolisj.engine.MicropolisTool
import za.co.nieto.nietocity.game.GameController

/**
 * The 16-tool palette. Icons come from the engine classpath resources. The
 * selected tool shows its highlighted (_hi) icon; tapping it again deselects it
 * (pan mode). Orientation (vertical left column / horizontal bottom strip) is set
 * by the containing layout.
 */
class ToolPaletteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

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

    private val buttons = HashMap<MicropolisTool, ImageButton>()
    private val plainIcons = HashMap<MicropolisTool, Bitmap>()
    private val hiIcons = HashMap<MicropolisTool, Bitmap>()
    private var controller: GameController? = null

    init {
        setBackgroundColor(Color.argb(180, 0, 0, 0))
        val pad = (3 * resources.displayMetrics.density).toInt()
        for (tool in tools) {
            plainIcons[tool] = loadIcon("${tool.name}.png")
            hiIcons[tool] = loadIcon("${tool.name}_hi.png")
            val btn = ImageButton(context).apply {
                setImageBitmap(plainIcons[tool])
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(pad, pad, pad, pad)
                contentDescription = tool.name
                setOnClickListener { toggle(tool) }
            }
            addView(btn)
            buttons[tool] = btn
        }
    }

    fun setController(controller: GameController) {
        this.controller = controller
        refresh()
    }

    private fun toggle(tool: MicropolisTool) {
        controller?.toggleTool(tool)
        refresh()
        listener?.invoke(controller?.tool)
    }

    private fun refresh() {
        val selected = controller?.tool
        for (tool in tools) {
            buttons[tool]?.setImageBitmap(if (tool == selected) hiIcons[tool] else plainIcons[tool])
        }
    }

    private val GameController.tool: MicropolisTool? get() = this.getTool()

    private fun loadIcon(fileName: String): Bitmap {
        val stream = ToolPaletteView::class.java.getResourceAsStream("/tools/$fileName")
            ?: error("Missing tool icon /tools/$fileName")
        stream.use {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            return BitmapFactory.decodeStream(it, null, opts)
                ?: error("Could not decode /tools/$fileName")
        }
    }
}
