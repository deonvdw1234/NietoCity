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
import android.widget.Toast
import micropolisj.engine.MicropolisTool
import za.co.nieto.nietocity.game.CurrencyFormat
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

    private var queryDialog: android.app.AlertDialog? = null
    private var backCount = 0
    private var firstBackAt = 0L

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

        controller = (lastNonConfigurationInstance as? GameController) ?: GameController.newGame()

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

        // Selected-tool bar: always shows the current tool (or Pan), its cost and a
        // large X that returns to Pan. In portrait the bar also toggles the grid.
        val toolsBar: View? = findViewById(R.id.toolsBar)
        val paletteScroll: View? = findViewById(R.id.paletteScroll)
        toolsBar?.setOnClickListener {
            paletteScroll?.let {
                it.visibility = if (it.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
        val clearBtn: View? = findViewById(R.id.clearTool)
        clearBtn?.setOnClickListener {
            controller.setTool(null)
            refreshTool()
        }
        palette.listener = {
            refreshTool()
        }
        // A one-shot tool clears itself after placing; refresh the bar and palette.
        cityView.placementListener = { refreshTool() }
        refreshTool()
    }

    /** Sync the status bar, the palette highlight and the selected-tool bar to the
     *  controller's current tool (which may have auto-cleared after a placement). */
    private fun refreshTool() {
        val tool = controller.getTool()
        statusBar.update(controller.snapshot())
        palette.syncSelection()

        val icon: ImageView? = findViewById(R.id.selectedIcon)
        val name: TextView? = findViewById(R.id.selectedName)
        val cost: TextView? = findViewById(R.id.selectedCost)
        val clearBtn: View? = findViewById(R.id.clearTool)
        if (tool == null) {
            icon?.setImageBitmap(null)
            name?.text = getString(R.string.pan_mode)
            cost?.text = ""
            clearBtn?.visibility = View.GONE
        } else {
            icon?.setImageBitmap(palette.iconFor(tool))
            name?.text = GameStrings.toolName(tool)
            cost?.text = if (tool.toolCost != 0) CurrencyFormat.format(tool.toolCost.toLong()) else ""
            clearBtn?.visibility = View.VISIBLE
        }
    }

    private val MicropolisTool.toolCost: Int get() = getToolCost()

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
        queryDialog = android.app.AlertDialog.Builder(this)
            .setTitle(report.header)
            .setMessage(body.toString())
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener { queryDialog = null }
            .show()
    }

    override fun onBackPressed() {
        // 1. Close the query dialog if it is open.
        val dlg = queryDialog
        if (dlg != null && dlg.isShowing) {
            dlg.dismiss()
            return
        }
        // 2. Close the tools drawer (portrait only; landscape's palette is permanent).
        val toolsBar: View? = findViewById(R.id.toolsBar)
        val paletteScroll: View? = findViewById(R.id.paletteScroll)
        if (toolsBar != null && paletteScroll != null && paletteScroll.visibility == View.VISIBLE) {
            paletteScroll.visibility = View.GONE
            return
        }
        // 3. Nothing open: three presses within 2s ask to exit; fewer just hint.
        val now = SystemClock.uptimeMillis()
        if (backCount == 0 || now - firstBackAt > BACK_WINDOW_MS) {
            backCount = 1
            firstBackAt = now
        } else {
            backCount++
        }
        if (backCount >= 3) {
            backCount = 0
            confirmExit()
        } else {
            Toast.makeText(this, R.string.back_hint, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmExit() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(R.string.exit_title)
            .setPositiveButton(R.string.exit_yes) { _, _ -> exitApp() }
            .setNegativeButton(R.string.exit_no) { d, _ -> d.dismiss() }
            .setCancelable(true) // back / tap-outside means Stay
            .create()
        dialog.show()
        // Stay is the default: focus it so Enter/centre keeps the city.
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.requestFocus()
    }

    /** The single exit point (Phase 7 will show the exit splash here). */
    private fun exitApp() {
        finish()
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
        private const val BACK_WINDOW_MS = 2000L
    }
}
