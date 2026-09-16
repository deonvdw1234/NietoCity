/*
 * NietoCity - shared headless demo helper.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * This is Nieto glue (not part of the imported MicropolisJ engine). It builds a
 * tiny but functioning reference city and exposes a public tick, so the Android
 * app, the desktop app, and the tests all drive the engine the same way. It
 * lives in the micropolisj.engine package only because the per-tick step()
 * method is package-private.
 */
package micropolisj.engine;

import static micropolisj.engine.TileConstants.DIRT;

public final class NietoDemo
{
	public static final long MAP_SEED = 12345L;
	public static final long SIM_SEED = 1L;
	public static final int START_FUNDS = 1_000_000;

	private NietoDemo() {}

	/**
	 * Build a small, functioning city on a seeded map: one residential zone, one
	 * road for access, and one coal power plant placed away from the zone and
	 * joined to it by a power line. Built near the map centre where land value is
	 * high enough for the zone to develop. The simulation PRNG is reseeded so the
	 * result is reproducible.
	 */
	public static Micropolis newDemoCity()
	{
		Micropolis city = new Micropolis();
		new MapGenerator(city).generateSomeCity(MAP_SEED);

		for (int y = 46; y <= 66; y++) {
			for (int x = 52; x <= 68; x++) {
				city.setTile(x, y, DIRT);
			}
		}

		city.setFunds(START_FUNDS);
		MicropolisTool.RESIDENTIAL.beginStroke(city, 60, 50).apply();
		MicropolisTool.ROADS.beginStroke(city, 58, 50).apply();
		MicropolisTool.POWERPLANT.beginStroke(city, 60, 62).apply();

		ToolStroke wire = MicropolisTool.WIRE.beginStroke(city, 60, 52);
		wire.dragTo(60, 60);
		wire.apply();

		city.PRNG.setSeed(SIM_SEED);
		return city;
	}

	/** Advance the simulation by one tick (step() is package-private). */
	public static void tick(Micropolis city)
	{
		city.step();
	}

	/** Current city funds. */
	public static int funds(Micropolis city)
	{
		return city.budget.totalFunds;
	}

	/** Current city population. */
	public static int population(Micropolis city)
	{
		return city.getCityPopulation();
	}
}
