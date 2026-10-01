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
    private var exiting = false

    // Budget auto-show: once a year unless the player turned it off (persisted).
    private var autoShowBudget = true
    private var lastBudgetYear = 0
    private var budgetDialogOpen = false

    private var soundPlayer: SoundPlayer? = null
    private lateinit var saveStore: SaveStore
    private var cityName = "My City"

    // Educational mode: when on, picking a tool, choosing an overlay, querying a
    // tile or viewing the evaluation shows a short plain-language card. Off by
    // default; persisted.
    private var explainEnabled = false

    // The ONE observer of the selected tool. GameController is the only owner of
    // the selection; this listener redraws the palette highlight, the selected-
    // tool bar and the status bar from it. It moves with the controller on every
    // swap (New City / Load) and is removed when this activity is destroyed, so
    // a rotated activity registers its own. It can fire on the engine thread (a
    // one-shot auto-clear), so it hops to the UI thread.
    private val toolListener = GameController.ToolListener { _ ->
        if (Looper.myLooper() == Looper.getMainLooper()) refreshTool() else ui.post { refreshTool() }
    }

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

        // A fresh cold start honours the action the start screen asked for; a
        // configuration change (rotation) keeps the retained running city.
        val retained = lastNonConfigurationInstance as? GameController
        val action = if (retained == null) intent.getStringExtra(EXTRA_ACTION) else null
        saveStore = SaveStore(applicationContext)
        controller = retained ?: buildInitialController(action)

        setContentView(R.layout.activity_main)
        statusBar = findViewById(R.id.statusBar)
        ticker = findViewById(R.id.ticker)
        cityView = findViewById(R.id.cityView)
        palette = findViewById(R.id.palette)
        miniMap = findViewById(R.id.miniMap)

        val columns = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 4
        cityView.setController(controller)
        palette.setup(columns)

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
        clearBtn?.setOnClickListener { controller.setTool(null) }
        // A palette tap toggles the tool on the LIVE controller (never a copy); the
        // tool listener then redraws the palette and the bar.
        palette.onToolTapped = { tool ->
            controller.toggleTool(tool)
            // Picking a tool collapses the portrait drawer so the whole map shows
            // (landscape's column is permanent and has no toolsBar).
            if (controller.getTool() != null && toolsBar != null) {
                paletteScroll?.visibility = View.GONE
            }
            // Explain: show a card when the user picks a tool (not on deselect).
            if (explainEnabled) {
                val t = controller.getTool()
                if (t != null) showCard(za.co.nieto.nietocity.game.Education.forTool(t))
            }
        }

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

        // Random disasters: default on, persisted, applied to the engine's own flag.
        controller.setRandomDisastersEnabled(prefs().getBoolean(PREF_RANDOM, true))

        // Educational mode: off by default, persisted.
        explainEnabled = prefs().getBoolean(PREF_EXPLAIN, false)

        refreshSpeed()
        // Observe the selected tool (delivers the current tool at once).
        controller.addToolListener(toolListener)

        // New City / Load City from the start screen open the matching screen on
        // top of the (placeholder) running city, with no discard guard.
        when (action) {
            ACTION_NEW -> showNewCityScreen(guard = false)
            ACTION_LOAD -> showLoadScreen(guard = false)
        }
    }

    /** Build the controller for a fresh cold start according to the chosen action. */
    private fun buildInitialController(action: String?): GameController {
        if (action == ACTION_CONTINUE && saveStore.autosaveExists()) {
            try {
                val gc = GameController.loadGame(saveStore.autosaveFile())
                cityName = saveStore.autosaveMeta().name
                return gc
            } catch (e: Exception) {
                // Autosave unreadable: fall back to a fresh city.
            }
        }
        return GameController.newGame()
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

    /** The overflow menu, grouped by order: city files, dialogs, disasters,
     *  view (mini map + overlay) and the on/off toggles. */
    private fun showMenu() {
        val popup = PopupMenu(this, statusBar)
        val menu = popup.menu
        menu.add(0, ID_NEW_CITY, 0, "New City…")
        menu.add(0, ID_SAVE, 1, "Save…")
        menu.add(0, ID_LOAD, 2, "Load…")
        menu.add(0, ID_BUDGET, 10, "Budget…")
        menu.add(0, ID_EVALUATION, 11, "Evaluation…")
        menu.add(0, ID_GRAPHS, 12, "Graphs…")
        val miniOn = miniMap.visibility == View.VISIBLE
        menu.add(0, ID_MINIMAP, 30, if (miniOn) "Hide mini map" else "Show mini map")

        val dsub = menu.addSubMenu(0, ID_DISASTER_SUB, 20, "Disasters")
        for (i in DISASTER_NAMES.indices) {
            dsub.add(0, ID_DISASTER_BASE + i, i, DISASTER_NAMES[i])
        }
        val note = dsub.add(0, ID_DISASTER_NOTE, DISASTER_NAMES.size,
            "(Plane crash & shipwreck happen during play)")
        note.isEnabled = false

        val randomItem = menu.add(0, ID_RANDOM_DISASTERS, 40, "Random disasters")
        randomItem.isCheckable = true
        randomItem.isChecked = controller.isRandomDisastersEnabled

        val muteItem = menu.add(0, ID_MUTE, 41, "Mute sound")
        muteItem.isCheckable = true
        muteItem.isChecked = soundPlayer?.isMuted == true

        val explainItem = menu.add(0, ID_EXPLAIN, 42, "Explain (learn as you play)")
        explainItem.isCheckable = true
        explainItem.isChecked = explainEnabled

        val sub = menu.addSubMenu(0, ID_OVERLAY_SUB, 31, "Overlay")
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
            ID_NEW_CITY -> { showNewCityScreen(); return true }
            ID_SAVE -> { showSaveScreen(); return true }
            ID_LOAD -> { showLoadScreen(); return true }
            ID_BUDGET -> { showBudget(); return true }
            ID_EVALUATION -> { AppDialogs.showEvaluation(this, controller, explainEnabled); return true }
            ID_GRAPHS -> { AppDialogs.showGraphs(this, controller); return true }
            ID_MINIMAP -> { toggleMiniMap(); return true }
            ID_MUTE -> { toggleMute(); return true }
            ID_RANDOM_DISASTERS -> { toggleRandomDisasters(); return true }
            ID_EXPLAIN -> { toggleExplain(); return true }
        }
        val dIdx = item.itemId - ID_DISASTER_BASE
        if (dIdx in DISASTER_NAMES.indices) {
            triggerDisaster(dIdx)
            return true
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
        if (explainEnabled && overlay != MapOverlay.NONE) {
            showCard(za.co.nieto.nietocity.game.Education.forOverlay(overlay))
        }
    }

    private fun toggleExplain() {
        explainEnabled = !explainEnabled
        prefs().edit().putBoolean(PREF_EXPLAIN, explainEnabled).apply()
        if (explainEnabled) {
            Toast.makeText(this, "Explain on: pick a tool or overlay to learn how it works.",
                Toast.LENGTH_SHORT).show()
        }
    }

    /** Show an educational card (Explain mode). */
    private fun showCard(card: za.co.nieto.nietocity.game.Education.Card?) {
        if (card == null) return
        AppDialogs.titledMessage(this, card.title, card.body).show()
    }

    private fun showNewCityScreen(guard: Boolean = true) {
        AppDialogs.showNewCity(this, controller) { level, seed, cfg ->
            if (guard) confirmNewCity(level, seed, cfg) else applyNewCity(level, seed, cfg)
        }
    }

    private fun confirmNewCity(level: Int, seed: Long, cfg: za.co.nieto.nietocity.game.TerrainConfig) {
        confirmDiscard("Start a new city?", "Your current city will be discarded.", "Start") {
            applyNewCity(level, seed, cfg)
        }
    }

    /**
     * Guard a swap that discards the running city, offering to Save first (a
     * quick save under the current name), Proceed, or Cancel (the default).
     */
    private fun confirmDiscard(title: String, message: String, proceed: String, onProceed: () -> Unit) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(proceed) { _, _ -> onProceed() }
            .setNeutralButton("Save first") { _, _ -> if (quickSave()) onProceed() }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setCancelable(true)
            .create()
        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.requestFocus()
    }

    /** Save the current city under its current name; returns whether it succeeded. */
    private fun quickSave(): Boolean = try {
        saveStore.save(cityName, controller)
        Toast.makeText(this, "Saved \"$cityName\"", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        Toast.makeText(this, "Could not save: ${e.message}", Toast.LENGTH_LONG).show()
        false
    }

    private fun applyNewCity(level: Int, seed: Long, cfg: za.co.nieto.nietocity.game.TerrainConfig) {
        swapController(GameController.newGame(level, seed, cfg))
        cityName = "New City"
    }

    /** Swap the running controller for a fresh one (new city or loaded city). */
    private fun swapController(fresh: GameController) {
        val old = controller
        // Carry over the player's session settings.
        fresh.setChosenSpeed(old.chosenSpeed)
        fresh.setPaused(old.isPaused)
        fresh.setRandomDisastersEnabled(old.isRandomDisastersEnabled)
        soundPlayer?.let { fresh.setSoundPlayer(it) }

        old.stop()
        controller = fresh
        // Move the tool observer to the live city (it syncs to fresh's tool now).
        old.transferToolListeners(fresh)
        cityView.setController(controller)
        cityView.resetViewportToMapCentre()
        miniMap.bind(controller, cityView.getViewport())
        lastBudgetYear = currentYear()
        controller.start()
        refreshSpeed()
    }

    private fun showSaveScreen() {
        AppDialogs.showSave(this, cityName, { saveStore.exists(it) }) { name ->
            try {
                saveStore.save(name, controller)
                cityName = name
                Toast.makeText(this, "Saved \"$name\"", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoadScreen(guard: Boolean = true) {
        AppDialogs.showLoad(this, saveStore) { slot -> if (guard) confirmLoad(slot) else applyLoad(slot) }
    }

    private fun confirmLoad(slot: SaveSlot) {
        confirmDiscard("Load a city?", "Unsaved changes will be lost.", "Load") { applyLoad(slot) }
    }

    private fun applyLoad(slot: SaveSlot) {
        val fresh = try {
            GameController.loadGame(saveStore.ctyFile(slot.base))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        swapController(fresh)
        cityName = slot.meta.name
        Toast.makeText(this, "Loaded \"${slot.meta.name}\"", Toast.LENGTH_SHORT).show()
    }

    private fun triggerDisaster(index: Int) {
        when (index) {
            0 -> controller.triggerFire()
            1 -> controller.triggerFlood()
            2 -> controller.triggerTornado()
            3 -> controller.triggerEarthquake()
            4 -> controller.triggerMonster()
            5 -> controller.triggerMeltdown()
        }
    }

    private fun toggleMute() {
        val player = soundPlayer ?: return
        val newMuted = !player.isMuted
        player.isMuted = newMuted
        prefs().edit().putBoolean(PREF_MUTED, newMuted).apply()
    }

    private fun toggleRandomDisasters() {
        val enabled = !controller.isRandomDisastersEnabled
        controller.setRandomDisastersEnabled(enabled)
        prefs().edit().putBoolean(PREF_RANDOM, enabled).apply()
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

    /** Redraw the status bar, the palette highlight and the selected-tool bar from
     *  the controller's tool (called only by the tool listener). */
    private fun refreshTool() {
        val tool = controller.getTool()
        statusBar.update(controller.snapshot())
        palette.showSelection(tool)
        cityView.requestRender() // armed-tool border on/off

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

    /**
     * Autosave the current city to the reserved slot on pause/exit so Continue can
     * resume it. Best-effort: a failure must never crash or block leaving.
     */
    override fun onStop() {
        try {
            saveStore.saveAutosave(cityName, controller)
        } catch (e: Exception) {
            // Swallow: an autosave failure must not stop the app from backgrounding.
        }
        super.onStop()
    }

    override fun onDestroy() {
        // Release this activity's sound player (a new one is made on recreate) and
        // stop observing the (possibly retained) controller.
        controller.removeToolListener(toolListener)
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
        if (explainEnabled) {
            za.co.nieto.nietocity.game.Education.forTool(MicropolisTool.QUERY)?.let {
                body.append("\n\n").append(it.body)
            }
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
        // 2. A tool is armed: return to Pan first (before the drawer/exit rules).
        if (controller.getTool() != null) {
            controller.setTool(null)
            backCount = 0
            return
        }
        // 3. Close the tools drawer (portrait only; landscape's palette is permanent).
        val toolsBar: View? = findViewById(R.id.toolsBar)
        val paletteScroll: View? = findViewById(R.id.paletteScroll)
        if (toolsBar != null && paletteScroll != null && paletteScroll.visibility == View.VISIBLE) {
            paletteScroll.visibility = View.GONE
            return
        }
        // 4. Nothing open: three presses within 2s ask to exit; fewer just hint.
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

    /** The single exit point: autosave, then the exit splash, then quit. */
    private fun exitApp() {
        if (exiting) return
        exiting = true
        try {
            saveStore.saveAutosave(cityName, controller)
        } catch (e: Exception) {
            // Autosave failure must never block the exit.
        }
        startActivity(android.content.Intent(this, ExitSplashActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        private const val ID_EXPLAIN = 14
        private const val ID_NEW_CITY = 11
        private const val ID_SAVE = 12
        private const val ID_LOAD = 13
        private const val ID_RANDOM_DISASTERS = 8
        private const val ID_MINIMAP = 1
        private const val ID_OVERLAY_SUB = 2
        private const val GROUP_OVERLAY = 10
        private const val ID_OVERLAY_BASE = 100
        private const val ID_DISASTER_SUB = 7
        private const val ID_DISASTER_BASE = 200
        private const val ID_DISASTER_NOTE = 250
        private val DISASTER_NAMES = arrayOf(
            "Fire", "Flood", "Tornado", "Earthquake", "Monster", "Nuclear meltdown"
        )
        private const val PREF_AUTO_BUDGET = "autoBudget"
        private const val PREF_MUTED = "muted"
        private const val PREF_RANDOM = "randomDisasters"
        private const val PREF_EXPLAIN = "explain"

        // Start-screen action passed in the launch intent.
        const val EXTRA_ACTION = "action"
        const val ACTION_CONTINUE = "continue"
        const val ACTION_NEW = "new"
        const val ACTION_LOAD = "load"
    }
}
