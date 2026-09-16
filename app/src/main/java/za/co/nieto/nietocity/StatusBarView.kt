/*
 * NietoCity - top status bar.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import za.co.nieto.nietocity.game.StatusSnapshot

/**
 * The classic top status bar: date, funds, population, current tool and its cost.
 */
class StatusBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val dateView = cell()
    private val fundsView = cell()
    private val popView = cell()
    private val toolView = cell()
    private val costView = cell()

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.argb(200, 0, 0, 0))
        val pad = (6 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        addView(dateView)
        addView(fundsView)
        addView(popView)
        addView(toolView)
        addView(costView)
        update(null)
    }

    private fun cell(): TextView {
        val tv = TextView(context)
        tv.setTextColor(Color.WHITE)
        tv.textSize = 13f
        tv.gravity = Gravity.CENTER_VERTICAL
        val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        tv.layoutParams = lp
        return tv
    }

    fun update(s: StatusSnapshot?) {
        if (s == null) {
            dateView.text = "—"
            fundsView.text = ""
            popView.text = ""
            toolView.text = ""
            costView.text = ""
            return
        }
        dateView.text = s.date
        fundsView.text = s.fundsText
        popView.text = "Pop ${s.population}"
        toolView.text = s.toolName
        costView.text = if (s.toolCost != 0) "$${s.toolCost}" else ""
    }
}
