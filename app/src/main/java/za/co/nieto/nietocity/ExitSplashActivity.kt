/*
 * NietoCity - exit splash.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/**
 * The final screen: shows the exit splash image and the copyright line for about
 * 1.8s, then quits. A ceiling timer and swallow-all make sure shutdown never
 * blocks, and it is re-entrancy safe.
 */
class ExitSplashActivity : Activity() {

    private val ui = Handler(Looper.getMainLooper())
    private var exited = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        try {
            root.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.splash_exit)
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        } catch (_: Exception) {
            // No image: the black background and copyright line are enough.
        }
        root.addView(TextView(this).apply {
            text = "Copyright 2026 Nieto Software. All rights reserved."
            setTextColor(Color.WHITE)
            setBackgroundColor(0x8C000000.toInt())
            gravity = Gravity.CENTER
            val p = (12 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, p / 2)
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM).apply {
            bottomMargin = (24 * resources.displayMetrics.density).toInt()
        })

        setContentView(root)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        ui.postDelayed({ doExit() }, HOLD_MS)
        // Hard ceiling in case anything above stalls.
        ui.postDelayed({ doExit() }, CEILING_MS)
    }

    private fun doExit() {
        if (exited) return
        exited = true
        try {
            ui.removeCallbacksAndMessages(null)
            finishAffinity() // close the whole task
        } catch (_: Throwable) {
            // ignore: never block shutdown
        }
    }

    override fun onBackPressed() {
        doExit()
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private companion object {
        const val HOLD_MS = 1800L
        const val CEILING_MS = 4000L
    }
}
