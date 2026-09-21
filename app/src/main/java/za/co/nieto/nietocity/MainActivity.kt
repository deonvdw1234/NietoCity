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
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import micropolisj.engine.MicropolisTool
import za.co.nieto.nietocity.game.CurrencyFormat
import za.co.nieto.nietocity.game.GameController
import za.co.nieto.nietocity.game.GameStrings
import za.co.nieto.nietocity.game.SoundPlayer
import za.co.nieto.nietocity.render.MapOverlay

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
    private lateinit var miniMap: MiniMapView

    private val ui = Handler(Looper.getMainLooper())
    private var lastStatusAt = 0L
    private var tickerHideAt = 0L

    private var queryDialog: android.app.AlertDialog? = null
    private var backCount = 0
    private var firstBackAt = 0L

    // Budget auto-show: once a year unless the player turned it off (persisted).
    private var autoShowBudget = true
    private var lastBudgetYear = 0
    private var budgetDialogOpen = false

    private var soundPlayer: SoundPlayer? = null

    private val pump = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            if (now - lastStatusAt >= 1000) {
                statusBar.update(controller.snapshot())
                lastStatusAt = now
            }
            if (miniMap.visibility == View.VISIBLE) {
                miniMap.setViewport(cityView.getViewport())
                miniMap.rebuild()
            }
            maybeAutoBudget()
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
        miniMap = findViewById(R.id.miniMap)

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

        // Speed control: pause/play toggle and a tap-cycle speed label.
        statusBar.onPauseClick = {
            controller.togglePause()
            refreshSpeed()
        }
        statusBar.onSpeedClick = {
            controller.cycleSpeed()
            refreshSpeed()
        }
        // Mini map (overview) in a corner, toggled from the status bar. It re-centres
        // the main view on tap/drag.
        miniMap.bind(controller, cityView.getViewport())
        miniMap.onRecenter = { cityView.requestRender() }
        statusBar.setMenuVisible(true)
        statusBar.onMenuClick = { showMenu() }

        // Budget auto-show once a year (persisted preference).
        autoShowBudget = prefs().getBoolean(PREF_AUTO_BUDGET, true)
        lastBudgetYear = currentYear()

        // Sound: default on, best-effort. The controller is retained across
        // rotation, so release any player from the previous activity first.
        controller.soundPlayer?.release()
        val player = AndroidSoundPlayer(applicationContext)
        player.isMuted = prefs().getBoolean(PREF_MUTED, false)
        controller.setSoundPlayer(player)
        soundPlayer = player

        refreshSpeed()
        refreshTool()
    }

    private fun prefs() = getSharedPreferences("nietocity", MODE_PRIVATE)

    private fun currentYear(): Int {
        val city = controller.engine
        return synchronized(city) { city.cityTime / 48 }
    }

    private fun showBudget() {
        if (budgetDialogOpen) return
        budgetDialogOpen = true
        AppDialogs.showBudget(
            this, controller, autoShowBudget,
            onAutoShowChanged = { auto ->
                autoShowBudget = auto
                prefs().edit().putBoolean(PREF_AUTO_BUDGET, auto).apply()
            },
            onDismiss = { budgetDialogOpen = false }
        )
    }

    /** The overflow menu: mini map toggle and the data-overlay picker. */
    private fun showMenu() {
        val popup = PopupMenu(this, statusBar)
        val menu = popup.menu
        menu.add(0, ID_BUDGET, 0, "Budget…")
        menu.add(0, ID_EVALUATION, 1, "Evaluation…")
        menu.add(0, ID_GRAPHS, 2, "Graphs…")
        val miniOn = miniMap.visibility == View.VISIBLE
        menu.add(0, ID_MINIMAP, 4, if (miniOn) "Hide mini map" else "Show mini map")

        val muteItem = menu.add(0, ID_MUTE, 6, "Mute sound")
        muteItem.isCheckable = true
        muteItem.isChecked = soundPlayer?.isMuted == true

        val sub = menu.addSubMenu(0, ID_OVERLAY_SUB, 5, "Overlay")
        val current = cityView.getMapOverlay()
        val overlays = MapOverlay.values()
        for (idx in overlays.indices) {
            val item = sub.add(GROUP_OVERLAY, ID_OVERLAY_BASE + idx, idx, overlays[idx].label())
            item.isCheckable = true
            item.isChecked = overlays[idx] == current
        }
        sub.setGroupCheckable(GROUP_OVERLAY, true, true)

        popup.setOnMenuItemClickListener { item -> onMenuItem(item) }
        popup.show()
    }

    private fun onMenuItem(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            ID_BUDGET -> { showBudget(); return true }
            ID_EVALUATION -> { AppDialogs.showEvaluation(this, controller); return true }
            ID_GRAPHS -> { AppDialogs.showGraphs(this, controller); return true }
            ID_MINIMAP -> { toggleMiniMap(); return true }
            ID_MUTE -> { toggleMute(); return true }
        }
        val idx = item.itemId - ID_OVERLAY_BASE
        val overlays = MapOverlay.values()
        if (idx in overlays.indices) {
            selectOverlay(overlays[idx])
            return true
        }
        return false
    }

    private fun selectOverlay(overlay: MapOverlay) {
        cityView.setMapOverlay(overlay)
        miniMap.setOverlay(overlay)
    }

    private fun toggleMute() {
        val player = soundPlayer ?: return
        val newMuted = !player.isMuted
        player.isMuted = newMuted
        prefs().edit().putBoolean(PREF_MUTED, newMuted).apply()
    }

    private fun toggleMiniMap() {
        val show = miniMap.visibility != View.VISIBLE
        miniMap.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            miniMap.setViewport(cityView.getViewport())
            miniMap.rebuild()
        }
    }

    /** Reflect the controller's pause state and chosen speed in the status bar. */
    private fun refreshSpeed() {
        statusBar.setSpeedState(controller.isPaused, controller.chosenSpeed)
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

    override fun onDestroy() {
        // Release this activity's sound player (a new one is made on recreate).
        soundPlayer?.release()
        super.onDestroy()
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

    /** Show the budget dialog automatically when a new city year begins. */
    private fun maybeAutoBudget() {
        val year = currentYear()
        if (year > lastBudgetYear) {
            lastBudgetYear = year
            if (autoShowBudget && !budgetDialogOpen) {
                showBudget()
            }
        }
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
        private const val ID_BUDGET = 3
        private const val ID_EVALUATION = 4
        private const val ID_GRAPHS = 5
        private const val ID_MUTE = 6
        private const val ID_MINIMAP = 1
        private const val ID_OVERLAY_SUB = 2
        private const val GROUP_OVERLAY = 10
        private const val ID_OVERLAY_BASE = 100
        private const val PREF_AUTO_BUDGET = "autoBudget"
        private const val PREF_MUTED = "muted"
    }
}
