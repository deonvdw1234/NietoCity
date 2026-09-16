/*
 * NietoCity - engine smoke test.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Builds the shared demo city (one residential zone, one road, one coal power
 * plant joined to the zone by a power line) on a seeded map, runs 200 simulation
 * ticks, and checks that the simulation is alive: both the city population and
 * the city funds have changed.
 */
package micropolisj.engine;

import static org.junit.Assert.*;
import org.junit.Test;

public class EngineSmokeTest
{
	private static final int TICKS = 200;

	@Test
	public void cityComesAlive()
	{
		Micropolis city = NietoDemo.newDemoCity();

		// Compare against the starting funds (before the city was built): building
		// the zones spends money. Tax income only arrives much later than 200
		// ticks, so funds do not change during the ticks alone.
		final int fundsBefore = NietoDemo.START_FUNDS;
		final int popBefore = NietoDemo.population(city);

		for (int i = 1; i <= TICKS; i++) {
			NietoDemo.tick(city);
			if (i == 100) {
				System.out.println("At 100 ticks: population " + NietoDemo.population(city)
					+ ", funds " + NietoDemo.funds(city));
			}
		}

		final int fundsAfter = NietoDemo.funds(city);
		final int popAfter = NietoDemo.population(city);
		System.out.println("At 200 ticks: population " + popBefore + " -> " + popAfter
			+ ", funds " + fundsBefore + " -> " + fundsAfter);

		assertNotEquals("funds should change after 200 ticks", fundsBefore, fundsAfter);
		assertNotEquals("population should change after 200 ticks", popBefore, popAfter);
		assertTrue("population should grow above zero", popAfter > 0);
	}
}
