/*
 * NietoCity - tile index tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.render;

import static org.junit.Assert.*;
import static micropolisj.engine.TileConstants.LOMASK;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import micropolisj.engine.MapGenerator;
import micropolisj.engine.Micropolis;
import micropolisj.engine.NietoDemo;
import micropolisj.engine.Tiles;

public class TileIndexTest
{
	private static TileIndex index;

	@BeforeClass
	public static void loadIndex()
	{
		index = TileIndex.loadDefault();
	}

	/**
	 * Every tile number that can actually appear on a map must resolve to a frame.
	 * We collect the tiles from several freshly generated random maps plus the
	 * demo city after simulation. (The atlas legitimately has no artwork for the
	 * ~174 animation-reserved tile numbers, e.g. 57-63, which never appear on a
	 * real map - fire is one animated tile, number 56, with eight frames.)
	 */
	@Test
	public void everyMapTileResolvesToAFrame()
	{
		Set<Integer> seen = new HashSet<Integer>();

		for (long seed : new long[] { 1L, 42L, 12345L, 99999L }) {
			Micropolis city = new Micropolis();
			new MapGenerator(city).generateSomeCity(seed);
			collectTiles(city, seen);
		}

		Micropolis demo = NietoDemo.newDemoCity();
		for (int i = 0; i < 200; i++) {
			NietoDemo.tick(demo);
		}
		collectTiles(demo, seen);

		assertFalse("expected to see some tiles", seen.isEmpty());

		for (int tile : seen) {
			assertTrue("tile " + tile + " that appears on a map has no artwork",
				index.hasImage(tile));
			int y = index.frameOffsetY(tile, 0);
			assertTrue("tile " + tile + " has a negative atlas offset", y >= 0);
			assertEquals("tile " + tile + " atlas offset is not tile-aligned",
				0, y % TileIndex.TILE_SIZE);
		}
	}

	/** Every named tile in the atlas index resolves for any animation cycle. */
	@Test
	public void everyNamedTileResolvesAtEveryCycle()
	{
		int named = 0;
		for (int tile = 0; tile < index.size(); tile++) {
			if (!index.hasImage(tile)) {
				continue;
			}
			named++;
			for (int cycle : new int[] { 0, 1, 7, 59, 123, 959 }) {
				int y = index.frameOffsetY(tile, cycle);
				assertTrue("tile " + tile + " cycle " + cycle, y >= 0);
				assertEquals(0, y % TileIndex.TILE_SIZE);
			}
		}
		// Sanity: the shipped atlas names hundreds of tiles.
		assertTrue("unexpectedly few named tiles: " + named, named >= 700);
	}

	/** Fire (tile 56) is animated: several frames that actually change over time. */
	@Test
	public void animatedTileCyclesThroughFrames()
	{
		final int FIRE = 56;
		assertTrue(index.hasImage(FIRE));
		assertTrue("fire should have multiple frames", index.frameCount(FIRE) > 1);

		Set<Integer> distinct = new HashSet<Integer>();
		for (int cycle = 0; cycle < 960; cycle++) {
			distinct.add(index.frameOffsetY(FIRE, cycle));
		}
		assertTrue("fire animation should show more than one frame over a cycle",
			distinct.size() > 1);
	}

	/** The index spans the engine's tile-number range. */
	@Test
	public void indexSpansEngineTileRange()
	{
		assertTrue("index should cover the engine tile range",
			index.size() >= Tiles.getTileCount() - 200);
	}

	private static void collectTiles(Micropolis city, Set<Integer> seen)
	{
		for (int y = 0; y < city.getHeight(); y++) {
			for (int x = 0; x < city.getWidth(); x++) {
				seen.add(city.getTile(x, y) & LOMASK);
			}
		}
	}
}
