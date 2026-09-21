/*
 * NietoCity - map generator determinism tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Proves the New City generator is reproducible: the same seed and terrain
 * settings produce an identical map (every tile), while a different seed or a
 * different terrain setting produces a different map. Also checks the public
 * generator config actually changes the result (e.g. "no trees" removes trees).
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import micropolisj.engine.Micropolis;
import micropolisj.engine.TileConstants;

public class MapGenTest
{
	private static boolean sameMap(Micropolis a, Micropolis b)
	{
		if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
			return false;
		}
		for (int y = 0; y < a.getHeight(); y++) {
			for (int x = 0; x < a.getWidth(); x++) {
				if (a.getTile(x, y) != b.getTile(x, y)) {
					return false;
				}
			}
		}
		return true;
	}

	@Test
	public void sameSeedAndSettingsProduceIdenticalMap()
	{
		TerrainConfig cfg = TerrainConfig.defaults();
		Micropolis a = GameController.buildCity(GameController.LEVEL_EASY, 12345L, cfg);
		Micropolis b = GameController.buildCity(GameController.LEVEL_EASY, 12345L, cfg);
		assertTrue("same seed + settings => identical map", sameMap(a, b));
	}

	@Test
	public void differentSeedsDiffer()
	{
		TerrainConfig cfg = TerrainConfig.defaults();
		Micropolis a = GameController.buildCity(GameController.LEVEL_EASY, 12345L, cfg);
		Micropolis b = GameController.buildCity(GameController.LEVEL_EASY, 67890L, cfg);
		assertFalse("different seeds => different map", sameMap(a, b));
	}

	@Test
	public void newGameRemembersItsSeed()
	{
		GameController gc = GameController.newGame(GameController.LEVEL_EASY, 4242L,
			TerrainConfig.defaults());
		assertEquals(4242L, gc.getSeed());
		// The remembered seed reproduces the same map.
		Micropolis again = GameController.buildCity(GameController.LEVEL_EASY, gc.getSeed(),
			gc.getTerrainConfig());
		assertTrue(sameMap(gc.getEngine(), again));
	}

	@Test
	public void treesOffRemovesTrees()
	{
		long seed = 999L;
		TerrainConfig withTrees = new TerrainConfig(TerrainConfig.Island.NONE,
			TerrainConfig.Level.NONE, TerrainConfig.Level.NONE, TerrainConfig.Level.HIGH);
		TerrainConfig noTrees = new TerrainConfig(TerrainConfig.Island.NONE,
			TerrainConfig.Level.NONE, TerrainConfig.Level.NONE, TerrainConfig.Level.NONE);
		assertTrue("trees present when asked", hasTree(GameController.buildCity(
			GameController.LEVEL_EASY, seed, withTrees)));
		assertFalse("no trees when turned off", hasTree(GameController.buildCity(
			GameController.LEVEL_EASY, seed, noTrees)));
	}

	private static boolean hasTree(Micropolis city)
	{
		for (int y = 0; y < city.getHeight(); y++) {
			for (int x = 0; x < city.getWidth(); x++) {
				if (TileConstants.isTree((char) (city.getTile(x, y)))) {
					return true;
				}
			}
		}
		return false;
	}
}
