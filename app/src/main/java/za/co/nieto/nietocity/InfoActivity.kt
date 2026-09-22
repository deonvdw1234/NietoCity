/*
 * NietoCity - About / Licence / How-to-play screens.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * A simple scrollable information screen, used for About, the GPL licence text
 * and the How-to-play / Educational reference. The mode is passed in EXTRA_MODE.
 */
class InfoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ABOUT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        root.addView(TextView(this).apply {
            text = titleFor(mode)
            setTextColor(Color.WHITE)
            textSize = 22f
            val p = dp(16)
            setPadding(p, dp(16), p, dp(8))
        })

        val body = TextView(this).apply {
            setTextColor(0xFFEDEDED.toInt())
            textSize = if (mode == MODE_LICENCE) 11f else 15f
            if (mode == MODE_LICENCE) typeface = Typeface.MONOSPACE
            else setLineSpacing(0f, 1.2f) // roomier prose; keep the licence compact
            setTextIsSelectable(true)
            val p = dp(16)
            setPadding(p, 0, p, dp(16))
            text = bodyFor(mode)
        }
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(body)
        }
        root.addView(scroll)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            val p = dp(12)
            setPadding(p, dp(6), p, dp(12))
        }
        if (mode == MODE_ABOUT) {
            buttons.addView(Button(this).apply {
                text = "Licence (GPL)"
                setOnClickListener {
                    startActivity(Intent(this@InfoActivity, InfoActivity::class.java)
                        .putExtra(EXTRA_MODE, MODE_LICENCE))
                }
            })
        }
        buttons.addView(Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        })
        root.addView(buttons)

        setContentView(root)
    }

    private fun titleFor(mode: String): String = when (mode) {
        MODE_LICENCE -> "Licence"
        MODE_EDUCATION -> "How to play"
        else -> "About NietoCity"
    }

    private fun bodyFor(mode: String): CharSequence = when (mode) {
        MODE_LICENCE -> InfoText.licence(this)
        MODE_EDUCATION -> InfoText.howToPlay()
        else -> InfoText.about(this)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_ABOUT = "about"
        const val MODE_LICENCE = "licence"
        const val MODE_EDUCATION = "education"
    }
}
