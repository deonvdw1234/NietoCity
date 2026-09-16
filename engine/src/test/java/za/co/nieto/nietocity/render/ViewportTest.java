/*
 * NietoCity - viewport tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class ViewportTest
{
	private static final int W = 120;
	private static final int H = 100;

	/** However hard we scroll, the visible tile range stays inside the map. */
	@Test
	public void neverExposesTilesOutsideTheMap()
	{
		for (int zoom = 1; zoom <= 3; zoom++) {
			Viewport vp = new Viewport(W, H, 800, 600);
			vp.setZoom(zoom);

			int[] pans = { -100000, -1, 0, 1, 100000 };
			for (int dx : pans) {
				for (int dy : pans) {
					vp.setScroll(0, 0);
					vp.panBy(dx, dy);

					assertTrue(vp.firstVisibleCol() >= 0);
					assertTrue(vp.firstVisibleRow() >= 0);
					assertTrue(vp.lastVisibleCol() <= W - 1);
					assertTrue(vp.lastVisibleRow() <= H - 1);
					assertTrue(vp.firstVisibleCol() <= vp.lastVisibleCol());
					assertTrue(vp.firstVisibleRow() <= vp.lastVisibleRow());

					// tile-at-pixel is clamped inside the map too
					assertTrue(vp.tileXAt(0) >= 0 && vp.tileXAt(0) <= W - 1);
					assertTrue(vp.tileXAt(vp.getViewWidthPx() - 1) <= W - 1);
					assertTrue(vp.tileYAt(vp.getViewHeightPx() - 1) <= H - 1);
				}
			}
		}
	}

	/** A map smaller than the view is pinned to the top-left and fully visible. */
	@Test
	public void smallMapIsPinnedAndFullyVisible()
	{
		Viewport vp = new Viewport(5, 5, 800, 600);
		vp.panBy(9999, 9999);
		assertEquals(0, vp.getScrollX());
		assertEquals(0, vp.getScrollY());
		assertEquals(0, vp.firstVisibleCol());
		assertEquals(4, vp.lastVisibleCol());
		assertEquals(0, vp.firstVisibleRow());
		assertEquals(4, vp.lastVisibleRow());
	}

	/** Changing zoom keeps the tile under the view centre under the view centre. */
	@Test
	public void zoomKeepsTheCentreTile()
	{
		Viewport vp = new Viewport(W, H, 800, 600);
		vp.centreOnTile(60, 50);

		int cx = vp.getViewWidthPx() / 2;
		int cy = vp.getViewHeightPx() / 2;
		int beforeX = vp.tileXAt(cx);
		int beforeY = vp.tileYAt(cy);

		for (int z : new int[] { 2, 3, 1, 3, 2 }) {
			vp.setZoom(z);
			assertEquals("zoom " + z + " moved the centre tile (x)", beforeX, vp.tileXAt(cx));
			assertEquals("zoom " + z + " moved the centre tile (y)", beforeY, vp.tileYAt(cy));
		}
	}

	/** Zoom is clamped to the 1x..3x range. */
	@Test
	public void zoomIsClamped()
	{
		Viewport vp = new Viewport(W, H, 800, 600);
		vp.setZoom(99);
		assertEquals(3, vp.getZoom());
		vp.setZoom(-5);
		assertEquals(1, vp.getZoom());
	}
}
