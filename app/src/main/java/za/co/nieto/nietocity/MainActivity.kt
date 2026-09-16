/*
 * NietoCity - an educational port of Micropolis.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import micropolisj.engine.MapGenerator
import micropolisj.engine.Micropolis
import micropolisj.engine.MicropolisTool
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

        val columns = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 4
        cityView.setController(controller)
        palette.setup(controller, columns)

        // Long press queries the tile and shows a small panel (tap outside / Back
        // to close).
        cityView.queryListener = { x, y -> showQuery(x, y) }

        // Portrait: a "Tools" bar shows the selected tool and toggles the grid.
        val toolsBar: View? = findViewById(R.id.toolsBar)
        val paletteScroll: View? = findViewById(R.id.paletteScroll)
        val selIcon: ImageView? = findViewById(R.id.selectedIcon)
        val selName: TextView? = findViewById(R.id.selectedName)
        toolsBar?.setOnClickListener {
            paletteScroll?.let {
                it.visibility = if (it.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
        palette.listener = { tool ->
            statusBar.update(controller.snapshot())
            updateSelectedBar(selIcon, selName, tool)
        }
        updateSelectedBar(selIcon, selName, controller.getTool())
    }

    private fun updateSelectedBar(icon: ImageView?, name: TextView?, tool: MicropolisTool?) {
        if (tool == null) {
            icon?.setImageBitmap(null)
            name?.text = getString(R.string.pan_mode)
        } else {
            icon?.setImageBitmap(palette.iconFor(tool))
            name?.text = GameStrings.toolName(tool)
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

    private fun showQuery(x: Int, y: Int) {
        val report = controller.queryReport(x, y)
        val body = StringBuilder()
        for (i in report.labels.indices) {
            body.append(report.labels[i]).append(' ').append(report.values[i])
            if (i < report.labels.size - 1) body.append('\n')
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(report.header)
            .setMessage(body.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show()
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
