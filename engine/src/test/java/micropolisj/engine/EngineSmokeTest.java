/*
 * NietoCity - engine smoke test.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Builds a tiny but functioning city (one residential zone, one road, one coal
 * power plant joined to the zone by a power line) on a seeded map, runs 200
 * simulation ticks, and checks that the simulation is alive: both the city
 * population and the city funds have changed.
 */
package micropolisj.engine;

import static org.junit.Assert.*;
import static micropolisj.engine.TileConstants.DIRT;
import org.junit.Test;

public class EngineSmokeTest
{
	private static final long MAP_SEED = 12345L;
	private static final long SIM_SEED = 1L;
	private static final int TICKS = 200;
	private static final int START_FUNDS = 1_000_000;

	@Test
	public void cityComesAlive()
	{
		Micropolis city = new Micropolis();
		// A new random map with a fixed seed, so the test is reproducible.
		new MapGenerator(city).generateSomeCity(MAP_SEED);

		// Build near the map centre: land value is highest there (it falls off
		// with distance from the city centre), which is what lets a residential
		// zone actually develop. Clear a flat work area first so tool placement
		// never lands on water or trees.
		for (int y = 46; y <= 66; y++) {
			for (int x = 52; x <= 68; x++) {
				city.setTile(x, y, DIRT);
			}
		}

		city.setFunds(START_FUNDS);
		final int fundsBefore = city.budget.totalFunds;
		final int popBefore = city.getCityPopulation();

		// One residential zone at the centre, one road beside it for access, and
		// one coal power plant placed well to the south so its heavy pollution
		// does not smother the zone. A power line carries power from the plant up
		// to the zone (residences are never built next to a coal plant).
		ToolResult res  = MicropolisTool.RESIDENTIAL.beginStroke(city, 60, 50).apply();
		ToolResult road = MicropolisTool.ROADS.beginStroke(city, 58, 50).apply();
		ToolResult coal = MicropolisTool.POWERPLANT.beginStroke(city, 60, 62).apply();

		ToolStroke wire = MicropolisTool.WIRE.beginStroke(city, 60, 52);
		wire.dragTo(60, 60);
		ToolResult wireResult = wire.apply();

		assertEquals("residential zone should build", ToolResult.SUCCESS, res);
		assertEquals("road should build", ToolResult.SUCCESS, road);
		assertEquals("coal plant should build", ToolResult.SUCCESS, coal);
		assertEquals("power line should build", ToolResult.SUCCESS, wireResult);

		// The simulation PRNG is otherwise unseeded (a shared static new Random()),
		// which every tile behaviour holds a reference to, so growth would be
		// non-deterministic. Reseed that shared instance to make the test stable.
		city.PRNG.setSeed(SIM_SEED);

		for (int i = 0; i < TICKS; i++) {
			city.step();
		}

		final int fundsAfter = city.budget.totalFunds;
		final int popAfter = city.getCityPopulation();
		System.out.println("Engine smoke test: funds " + fundsBefore + " -> " + fundsAfter
			+ ", population " + popBefore + " -> " + popAfter);

		assertNotEquals("funds should change after 200 ticks", fundsBefore, fundsAfter);
		assertNotEquals("population should change after 200 ticks", popBefore, popAfter);
		assertTrue("population should grow above zero", popAfter > 0);
	}
}
