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
import android.widget.Button
import android.widget.LinearLayout

/**
 * The start menu shown after the intro splash. Task 2 fills in the full set of
 * options (Continue, New City, Load City, How to play, About, Licence, Quit);
 * for now it simply enters the game.
 */
class StartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = android.view.Gravity.CENTER
        }
        root.addView(Button(this).apply {
            text = "Play"
            setOnClickListener {
                startActivity(Intent(this@StartActivity, MainActivity::class.java))
            }
        })
        setContentView(root)
    }
}
