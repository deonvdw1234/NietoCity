/*
 * NietoCity - an educational port of Micropolis.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import micropolisj.engine.MapGenerator
import micropolisj.engine.Micropolis
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.GameStrings

/**
 * Phase 3: a new random map shown in CityView, with a top status bar, a message
 * ticker, and (from task 3) a tool palette. The GameController owns the engine
 * thread and is retained across rotation so the city survives.
 */
class MainActivity : Activity() {

    private lateinit var controller: GameController
    private lateinit var statusBar: StatusBarView
    private lateinit var ticker: TextView
    private lateinit var cityView: CityView
    private lateinit var palette: ToolPaletteView

    private val ui = Handler(Looper.getMainLooper())
    private var lastStatusAt = 0L
    private var tickerHideAt = 0L

    private val pump = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            if (now - lastStatusAt >= 1000) {
                statusBar.update(controller.snapshot())
                lastStatusAt = now
            }
            pumpTicker(now)
            ui.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = (lastNonConfigurationInstance as? GameController) ?: run {
            val city = Micropolis()
            MapGenerator(city).generateNewCity()
            GameController(city)
        }

        setContentView(R.layout.activity_main)
        statusBar = findViewById(R.id.statusBar)
        ticker = findViewById(R.id.ticker)
        cityView = findViewById(R.id.cityView)
        palette = findViewById(R.id.palette)

        cityView.setController(controller)
        palette.setController(controller)
        palette.listener = { statusBar.update(controller.snapshot()) }

        // Long press queries the tile. (The full query panel arrives in task 5;
        // for now show the zone name in the ticker.)
        cityView.queryListener = { x, y ->
            val zs = controller.query(x, y)
            controller.postMessage(GameStrings.zoneName(zs.building))
        }

        // Portrait bottom sheet: the toggle collapses the palette to a thin strip.
        val toggle: Button? = findViewById(R.id.paletteToggle)
        val scroll: View? = findViewById(R.id.paletteScroll)
        toggle?.setOnClickListener {
            scroll?.let { it.visibility = if (it.visibility == View.GONE) View.VISIBLE else View.GONE }
        }
    }

    override fun onRetainNonConfigurationInstance(): Any = controller

    override fun onResume() {
        super.onResume()
        controller.start()
        lastStatusAt = 0L
        ui.post(pump)
    }

    override fun onPause() {
        ui.removeCallbacks(pump)
        controller.stop()
        super.onPause()
    }

    private fun pumpTicker(now: Long) {
        // Drain to the newest message so the ticker shows the latest event.
        var latest: String? = null
        var m = controller.pollMessage()
        while (m != null) {
            latest = m
            m = controller.pollMessage()
        }
        if (latest != null) {
            ticker.text = latest
            ticker.visibility = View.VISIBLE
            tickerHideAt = now + MESSAGE_MS
        } else if (ticker.visibility == View.VISIBLE && now >= tickerHideAt) {
            ticker.visibility = View.GONE
        }
    }

    companion object {
        private const val TICK_MS = 250L
        private const val MESSAGE_MS = 4000L
    }
}
