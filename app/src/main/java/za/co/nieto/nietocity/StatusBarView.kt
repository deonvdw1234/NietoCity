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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import micropolisj.engine.Speed
import za.co.nieto.nietocity.game.GameStrings
import za.co.nieto.nietocity.game.StatusSnapshot

/**
 * The classic top status bar: date, funds and population, plus a pause/play
 * toggle and a tap-cycle speed label (SLOW->NORMAL->FAST->ULTRA). A small menu
 * button (⋮) opens the overflow (mini map, overlays, dialogs). The selected tool
 * and its cost live in the bottom selected-tool bar, not here.
 *
 * Layout rule: the three values share the flexible width equally and each stays
 * on ONE line; when space is short the text autosizes down (13sp to a readable
 * 9sp) instead of wrapping or clipping. The buttons keep a fixed width.
 */
class StatusBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val dateView = cell()
    private val fundsView = cell()
    private val popView = cell()
    private val pauseView = button("▶")
    private val speedView = button("Normal").apply {
        // Fixed width (fits the longest name, "Normal") so cycling the speed never
        // reflows the values.
        minWidth = (64 * resources.displayMetrics.density).toInt()
    }
    private val menuView = button("⋮")

    /** Tapped the pause/play toggle. */
    var onPauseClick: (() -> Unit)? = null
    /** Tapped the speed label (cycle to the next speed). */
    var onSpeedClick: (() -> Unit)? = null
    /** Tapped the overflow menu button. */
    var onMenuClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.argb(200, 0, 0, 0))
        val pad = (6 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        addView(dateView)
        addView(fundsView)
        addView(popView)
        addView(pauseView)
        addView(speedView)
        addView(menuView)
        pauseView.setOnClickListener { onPauseClick?.invoke() }
        speedView.setOnClickListener { onSpeedClick?.invoke() }
        menuView.setOnClickListener { onMenuClick?.invoke() }
        update(null)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** A value cell: equal flexible share, one line, autosizing 13sp down to 9sp. */
    private fun cell(): TextView {
        val tv = TextView(context)
        tv.setTextColor(Color.WHITE)
        tv.textSize = 13f
        tv.gravity = Gravity.CENTER_VERTICAL
        tv.maxLines = 1
        tv.isSingleLine = true
        tv.setAutoSizeTextTypeUniformWithConfiguration(9, 13, 1, TypedValue.COMPLEX_UNIT_SP)
        // Autosize needs a bounded box: 0dp width (weight 1) and a fixed height.
        val lp = LayoutParams(0, dp(28), 1f)
        lp.rightMargin = dp(4)
        tv.layoutParams = lp
        return tv
    }

    /** A tappable control cell (fixed width, finger-sized tap target). */
    private fun button(initial: String): TextView {
        val tv = TextView(context)
        tv.text = initial
        tv.setTextColor(Color.WHITE)
        tv.textSize = 14f
        tv.gravity = Gravity.CENTER
        val padH = (8 * resources.displayMetrics.density).toInt()
        val padV = (6 * resources.displayMetrics.density).toInt()
        tv.setPadding(padH, padV, padH, padV)
        tv.isClickable = true
        tv.isFocusable = true
        tv.setBackgroundColor(Color.argb(60, 255, 255, 255))
        tv.maxLines = 1
        tv.isSingleLine = true
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.leftMargin = (4 * resources.displayMetrics.density).toInt()
        tv.layoutParams = lp
        return tv
    }

    /** Reflect the current pause state and chosen speed. */
    fun setSpeedState(paused: Boolean, chosenSpeed: Speed) {
        pauseView.text = if (paused) "▶" else "❚❚"
        speedView.text = GameStrings.speedName(chosenSpeed)
    }

    /** Show or hide the overflow menu button. */
    fun setMenuVisible(visible: Boolean) {
        menuView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun update(s: StatusSnapshot?) {
        if (s == null) {
            dateView.text = "—"
            fundsView.text = ""
            popView.text = ""
            return
        }
        dateView.text = s.date
        fundsView.text = s.fundsText
        popView.text = "Pop ${s.population}"
    }
}
