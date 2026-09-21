/*
 * NietoCity - shared mini map (overview) render core.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Builds the whole-city overview as one ARGB pixel per tile (from the tile
 * colours), and maps a point on a displayed overview back to a map tile so a tap
 * or drag can re-centre the main view. Both platforms wrap the pixel array in
 * their own image type (Android Bitmap / JavaFX WritableImage) and draw the
 * current viewport rectangle over it. Pure Java 8, no awt/android.
 *
 * Callers must hold the engine lock while calling {@link #overviewPixels}, since
 * it reads tiles (and, for an overlay, the overlay maps).
 */
package za.co.nieto.nietocity.render;

import micropolisj.engine.Micropolis;
import micropolisj.engine.TileConstants;

public final class MiniMap
{
	private MiniMap() { }

	/**
	 * The overview as ARGB pixels, row-major ({@code y*width + x}), one pixel per
	 * tile. With a non-null overlay other than NONE, the overlay's colour replaces
	 * the base tile colour where the overlay has something to show.
	 */
	public static int[] overviewPixels(Micropolis city, MapOverlay overlay)
	{
		int w = city.getWidth();
		int h = city.getHeight();
		int[] px = new int[w * h];
		boolean over = overlay != null && overlay != MapOverlay.NONE;
		int i = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int tile = city.getTile(x, y) & TileConstants.LOMASK;
				int color = TileColors.colorOf(tile);
				if (over) {
					int o = overlay.colorAt(city, x, y, tile);
					if (o != 0) {
						color = o;
					}
				}
				px[i++] = color;
			}
		}
		return px;
	}

	/** Overview with no overlay (base tile colours only). */
	public static int[] overviewPixels(Micropolis city)
	{
		return overviewPixels(city, MapOverlay.NONE);
	}

	/** The map column under a pixel X within a displayed overview of the given width. */
	public static int tileXForOverview(int px, int imageWidthPx, int mapWidthTiles)
	{
		return mapTile(px, imageWidthPx, mapWidthTiles);
	}

	/** The map row under a pixel Y within a displayed overview of the given height. */
	public static int tileYForOverview(int py, int imageHeightPx, int mapHeightTiles)
	{
		return mapTile(py, imageHeightPx, mapHeightTiles);
	}

	private static int mapTile(int pixel, int imagePx, int mapTiles)
	{
		if (imagePx <= 0 || mapTiles <= 0) {
			return 0;
		}
		int t = (int) ((long) pixel * mapTiles / imagePx);
		if (t < 0) return 0;
		if (t > mapTiles - 1) return mapTiles - 1;
		return t;
	}
}
