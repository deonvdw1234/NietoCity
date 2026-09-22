/*
 * NietoCity - start screen (menu).
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The start menu shown after the intro splash: Continue (only when an autosave
 * exists), New City, Load City, How to play / Educational, About, Licence (GPL)
 * and Quit, plus a disabled "Scenarios (coming soon)". Continue/New/Load hand off
 * to MainActivity, which honours the chosen action.
 */
class StartActivity : Activity() {

    private lateinit var saveStore: SaveStore
    private lateinit var menu: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveStore = SaveStore(applicationContext)

        val root = ScrollView(this).apply { setBackgroundColor(Color.BLACK) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val p = dp(24)
            setPadding(p, dp(36), p, dp(36))
        }

        column.addView(TextView(this).apply {
            text = "NietoCity"
            setTextColor(Color.WHITE)
            textSize = 32f
            gravity = Gravity.CENTER
        })
        column.addView(TextView(this).apply {
            text = "Created by Nieto Software"
            setTextColor(0xFFB0C4FF.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(28))
        })

        menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        column.addView(menu)
        root.addView(column)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        rebuildMenu() // Continue may have appeared/disappeared since we last showed.
    }

    private fun rebuildMenu() {
        menu.removeAllViews()
        if (saveStore.autosaveExists()) {
            menu.addView(menuButton("Continue") { launchGame(MainActivity.ACTION_CONTINUE) })
        }
        menu.addView(menuButton("New City") { launchGame(MainActivity.ACTION_NEW) })
        menu.addView(menuButton("Load City") { launchGame(MainActivity.ACTION_LOAD) })
        menu.addView(menuButton("How to play / Educational") { launchInfo(InfoActivity.MODE_EDUCATION) })
        menu.addView(menuButton("About") { launchInfo(InfoActivity.MODE_ABOUT) })
        menu.addView(menuButton("Licence (GPL)") { launchInfo(InfoActivity.MODE_LICENCE) })
        menu.addView(menuButton("Scenarios (coming soon)", enabled = false) {})
        menu.addView(menuButton("Quit") { quit() })
    }

    private fun launchGame(action: String) {
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_ACTION, action))
    }

    private fun launchInfo(mode: String) {
        startActivity(Intent(this, InfoActivity::class.java).putExtra(InfoActivity.EXTRA_MODE, mode))
    }

    private fun quit() {
        // Task 7 wires the exit splash here (the app's single exit surface).
        finishAffinity()
    }

    private fun menuButton(text: String, enabled: Boolean = true, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            isEnabled = enabled
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
            setOnClickListener { onClick() }
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
