/*
 * NietoCity - game layer tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.*;
import org.junit.Test;

import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.ToolResult;

public class GameControllerTest
{
	// A fresh Micropolis (no MapGenerator) is an all-dirt map, so tool placement
	// is deterministic. Water tile value (RIVER) is 2 in the engine.
	private static final char WATER = 2;

	@Test
	public void roadCostsTenAndFundsDropByTen()
	{
		Micropolis city = new Micropolis();
		city.setFunds(1000);
		GameController gc = new GameController(city);

		ToolResult r = gc.applyNow(MicropolisTool.ROADS, 40, 40, 40, 40);

		assertEquals(ToolResult.SUCCESS, r);
		assertEquals(990, city.budget.totalFunds);
	}

	@Test
	public void insufficientFundsLeavesMapUnchanged()
	{
		Micropolis city = new Micropolis();
		city.setFunds(0);
		GameController gc = new GameController(city);

		char before = city.getTile(40, 40);
		ToolResult r = gc.applyNow(MicropolisTool.ROADS, 40, 40, 40, 40);

		assertEquals(ToolResult.INSUFFICIENT_FUNDS, r);
		assertEquals("map should be unchanged", before, city.getTile(40, 40));
		assertEquals(0, city.budget.totalFunds);
	}

	@Test
	public void residentialZoneOnWaterIsUhOh()
	{
		Micropolis city = new Micropolis();
		city.setFunds(1_000_000);
		// A patch of water where the 3x3 zone would go (anchor 40,40 -> 39..41).
		for (int y = 39; y <= 41; y++) {
			for (int x = 39; x <= 41; x++) {
				city.setTile(x, y, WATER);
			}
		}
		GameController gc = new GameController(city);

		ToolResult r = gc.applyNow(MicropolisTool.RESIDENTIAL, 40, 40, 40, 40);
		assertEquals(ToolResult.UH_OH, r);
	}

	@Test
	public void dateFormattingMatchesTheOriginal()
	{
		assertEquals("Jan 1900", GameStrings.formatGameDate(0));
		assertEquals("Jan 1901", GameStrings.formatGameDate(48));
	}
}
