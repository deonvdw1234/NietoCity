/*
 * NietoCity - an educational port of Micropolis.
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file.
 */
package za.co.nieto.nietocity

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import micropolisj.engine.NietoDemo

/**
 * Phase 1 smoke screen: proves the shared Micropolis engine loads its data files
 * and simulates inside the Android app. It builds the demo city, runs 100 ticks
 * on a background thread, then shows the population and funds. No rendering yet.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 20f
            text = getString(R.string.engine_starting)
        }
        setContentView(text)

        Thread {
            val city = NietoDemo.newDemoCity()
            repeat(100) { NietoDemo.tick(city) }
            val population = NietoDemo.population(city)
            val funds = NietoDemo.funds(city)

            Handler(Looper.getMainLooper()).post {
                text.text = getString(R.string.engine_ok, population, funds)
            }
        }.start()
    }
}
