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
import micropolisj.engine.MapGenerator
import micropolisj.engine.Micropolis

/**
 * Phase 2: generates a new random map and shows it full screen in CityView.
 * The simulation runs on its own thread while the Activity is resumed.
 */
class MainActivity : Activity() {

    private lateinit var cityView: CityView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val city = Micropolis()
        MapGenerator(city).generateNewCity()

        cityView = CityView(this)
        cityView.setCity(city)
        setContentView(cityView)
    }

    override fun onResume() {
        super.onResume()
        cityView.resumeEngine()
    }

    override fun onPause() {
        cityView.pauseEngine()
        super.onPause()
    }
}
