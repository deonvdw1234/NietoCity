/*
 * NietoCity - cold-launch intro splash.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView

/**
 * The launcher activity: on a cold start it plays the Nieto intro video
 * full-screen on black (letterboxed "contain", no controls), then hands off to
 * the start screen. Tap skips after 1s; a ~5s safety timer proceeds if playback
 * never ends or errors. If the OS is set to remove animations, the first frame
 * is shown briefly instead of playing the video.
 */
class SplashActivity : Activity() {

    private val ui = Handler(Looper.getMainLooper())
    private var proceeded = false
    private var startAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startAt = SystemClock.uptimeMillis()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)

        if (animationsDisabled()) {
            showFirstFrame(root)
            ui.postDelayed({ proceed() }, STILL_MS)
            return
        }

        val video = VideoView(this)
        root.addView(
            video,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )

        // Tap to skip, but only after the first second so it is not dismissed by an
        // accidental touch as the screen appears.
        root.setOnClickListener {
            if (SystemClock.uptimeMillis() - startAt >= SKIP_AFTER_MS) proceed()
        }

        val uri = Uri.parse("android.resource://$packageName/${R.raw.nieto_logo_animated}")
        video.setVideoURI(uri)
        video.setOnPreparedListener { _ ->
            // Play with the intro logo's sound (offline, best-effort).
            video.start()
        }
        video.setOnCompletionListener { proceed() }
        video.setOnErrorListener { _, _, _ -> proceed(); true }

        // Safety net: proceed even if the video never signals completion.
        ui.postDelayed({ proceed() }, SAFETY_MS)
    }

    /** Show the video's first frame (used when animations are turned off). */
    private fun showFirstFrame(root: FrameLayout) {
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }
        root.addView(
            image,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        try {
            val retriever = MediaMetadataRetriever()
            try {
                val uri = Uri.parse("android.resource://$packageName/${R.raw.nieto_logo_animated}")
                retriever.setDataSource(this, uri)
                val frame = retriever.getFrameAtTime(0)
                if (frame != null) image.setImageBitmap(frame)
            } finally {
                retriever.release()
            }
        } catch (_: Exception) {
            // No frame available; the black background is enough.
        }
        root.setOnClickListener { proceed() }
    }

    /** Whether the OS is set to remove animations (animator duration scale 0). */
    private fun animationsDisabled(): Boolean = try {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (_: Exception) {
        false
    }

    private fun proceed() {
        if (proceeded) return
        proceeded = true
        ui.removeCallbacksAndMessages(null)
        startActivity(Intent(this, StartActivity::class.java))
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Let the intro be skipped straight to the start screen.
        proceed()
    }

    private companion object {
        const val SKIP_AFTER_MS = 1000L
        const val SAFETY_MS = 5000L
        const val STILL_MS = 900L
    }
}
