/*
 * NietoCity - desktop entry point.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Phase 1 smoke check: builds the shared demo city, runs the same 100 ticks as
 * the Android app, and prints the result. No desktop UI yet.
 */
package za.co.nieto.nietocity.desktop;

import micropolisj.engine.Micropolis;
import micropolisj.engine.NietoDemo;

public final class Main
{
	private Main() {}

	public static void main(String[] args)
	{
		Micropolis city = NietoDemo.newDemoCity();
		for (int i = 0; i < 100; i++) {
			NietoDemo.tick(city);
		}
		System.out.println("Engine OK: population " + NietoDemo.population(city)
			+ ", funds " + NietoDemo.funds(city));
	}
}
