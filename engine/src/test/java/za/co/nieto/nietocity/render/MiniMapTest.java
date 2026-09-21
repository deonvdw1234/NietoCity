/*
 * NietoCity - mini map (overview) mapping tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Proves the overview point->tile mapping used to re-centre the main view: a tap
 * anywhere inside a tile's cell of a displayed overview maps back to that tile
 * (within one tile), for a range of map sizes and display scales, and that the
 * mapping clamps to the map at the edges.
 */
package za.co.nieto.nietocity.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MiniMapTest
{
	@Test
	public void tapMapsToMatchingTileWithinOne()
	{
		int[] mapWidths = { 1, 10, 120, 100 };
		int[] scales = { 1, 2, 3, 5 };
		for (int mapW : mapWidths) {
			for (int scale : scales) {
				int imageW = mapW * scale;
				for (int tile = 0; tile < mapW; tile++) {
					// A tap at the centre of this tile's cell in the overview image.
					int px = tile * scale + scale / 2;
					int got = MiniMap.tileXForOverview(px, imageW, mapW);
					assertTrue("tile " + tile + " mapW " + mapW + " scale " + scale
						+ " -> " + got, Math.abs(got - tile) <= 1);
				}
			}
		}
	}

	@Test
	public void edgesClampInsideTheMap()
	{
		int mapW = 120, imageW = 360; // scale 3
		assertEquals(0, MiniMap.tileXForOverview(-10, imageW, mapW));
		assertEquals(0, MiniMap.tileXForOverview(0, imageW, mapW));
		assertEquals(mapW - 1, MiniMap.tileXForOverview(imageW, imageW, mapW));
		assertEquals(mapW - 1, MiniMap.tileXForOverview(imageW * 4, imageW, mapW));
	}

	@Test
	public void yAxisMapsLikeX()
	{
		int mapH = 100, imageH = 200; // scale 2
		for (int tile = 0; tile < mapH; tile++) {
			int py = tile * 2 + 1;
			assertEquals(tile, MiniMap.tileYForOverview(py, imageH, mapH));
		}
	}

	@Test
	public void degenerateSizesReturnZero()
	{
		assertEquals(0, MiniMap.tileXForOverview(50, 0, 120));
		assertEquals(0, MiniMap.tileXForOverview(50, 360, 0));
	}
}
