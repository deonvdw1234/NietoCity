/*
 * NietoCity - platform-neutral viewport.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Tracks how the map is scrolled and zoomed within a fixed on-screen area, and
 * works out which tiles are visible. All coordinates are integers; zoom is an
 * integer factor (1x/2x/3x) so pixels stay crisp. Pure Java 8, no awt/android.
 *
 * Pixel coordinate model: "device pixels" are on-screen pixels; one map tile is
 * TILE_SIZE * zoom device pixels. scrollX/scrollY are the device-pixel position
 * of the map's top-left corner relative to the top-left of the view, clamped so
 * the view never shows anything outside the map (unless the map is smaller than
 * the view, in which case it is pinned to the top-left).
 */
package za.co.nieto.nietocity.render;

public final class Viewport
{
	public static final int MIN_ZOOM = 1;
	public static final int MAX_ZOOM = 3;

	private final int tileSize;
	private final int mapWidthTiles;
	private final int mapHeightTiles;

	private int viewWidthPx;
	private int viewHeightPx;
	private int zoom = 1;
	private int scrollX; // device px from map's left edge to view's left edge
	private int scrollY;

	public Viewport(int mapWidthTiles, int mapHeightTiles, int viewWidthPx, int viewHeightPx)
	{
		this(TileIndex.TILE_SIZE, mapWidthTiles, mapHeightTiles, viewWidthPx, viewHeightPx);
	}

	public Viewport(int tileSize, int mapWidthTiles, int mapHeightTiles,
		int viewWidthPx, int viewHeightPx)
	{
		this.tileSize = tileSize;
		this.mapWidthTiles = mapWidthTiles;
		this.mapHeightTiles = mapHeightTiles;
		setViewSize(viewWidthPx, viewHeightPx);
	}

	public int getZoom() { return zoom; }
	public int getScrollX() { return scrollX; }
	public int getScrollY() { return scrollY; }
	public int getViewWidthPx() { return viewWidthPx; }
	public int getViewHeightPx() { return viewHeightPx; }

	/** Device-pixel size of one tile at the current zoom. */
	public int tilePx() { return tileSize * zoom; }

	/** Full map size in device pixels at the current zoom. */
	public int mapPixelWidth() { return mapWidthTiles * tilePx(); }
	public int mapPixelHeight() { return mapHeightTiles * tilePx(); }

	public void setViewSize(int viewWidthPx, int viewHeightPx)
	{
		this.viewWidthPx = Math.max(0, viewWidthPx);
		this.viewHeightPx = Math.max(0, viewHeightPx);
		clampScroll();
	}

	/** Pan by a device-pixel delta (e.g. a finger drag). */
	public void panBy(int dxPx, int dyPx)
	{
		scrollX += dxPx;
		scrollY += dyPx;
		clampScroll();
	}

	public void setScroll(int scrollX, int scrollY)
	{
		this.scrollX = scrollX;
		this.scrollY = scrollY;
		clampScroll();
	}

	/**
	 * Change the zoom (clamped to 1x..3x) while keeping the tile currently at the
	 * centre of the view at the centre of the view.
	 */
	public void setZoom(int newZoom)
	{
		int z = clamp(newZoom, MIN_ZOOM, MAX_ZOOM);
		if (z == zoom) {
			return;
		}
		// Tile (fractional) currently under the view centre.
		double centreTileX = (scrollX + viewWidthPx / 2.0) / tilePx();
		double centreTileY = (scrollY + viewHeightPx / 2.0) / tilePx();

		zoom = z;

		scrollX = (int) Math.round(centreTileX * tilePx() - viewWidthPx / 2.0);
		scrollY = (int) Math.round(centreTileY * tilePx() - viewHeightPx / 2.0);
		clampScroll();
	}

	/** Scroll so that the given tile is centred in the view (then clamped). */
	public void centreOnTile(int tileX, int tileY)
	{
		scrollX = (tileX * tilePx() + tilePx() / 2) - viewWidthPx / 2;
		scrollY = (tileY * tilePx() + tilePx() / 2) - viewHeightPx / 2;
		clampScroll();
	}

	/** The tile column at the given device-pixel X within the view (clamped). */
	public int tileXAt(int viewPx)
	{
		return clamp((scrollX + viewPx) / tilePx(), 0, mapWidthTiles - 1);
	}

	public int tileYAt(int viewPx)
	{
		return clamp((scrollY + viewPx) / tilePx(), 0, mapHeightTiles - 1);
	}

	/** First visible tile column (inclusive), always within the map. */
	public int firstVisibleCol() { return clamp(scrollX / tilePx(), 0, mapWidthTiles - 1); }
	public int firstVisibleRow() { return clamp(scrollY / tilePx(), 0, mapHeightTiles - 1); }

	/** Last visible tile column (inclusive), always within the map. */
	public int lastVisibleCol()
	{
		return clamp((scrollX + viewWidthPx - 1) / tilePx(), 0, mapWidthTiles - 1);
	}

	public int lastVisibleRow()
	{
		return clamp((scrollY + viewHeightPx - 1) / tilePx(), 0, mapHeightTiles - 1);
	}

	/** Device-pixel X of the left edge of the given tile column, within the view. */
	public int tileScreenX(int tileX) { return tileX * tilePx() - scrollX; }
	public int tileScreenY(int tileY) { return tileY * tilePx() - scrollY; }

	private void clampScroll()
	{
		scrollX = clampAxis(scrollX, mapPixelWidth(), viewWidthPx);
		scrollY = clampAxis(scrollY, mapPixelHeight(), viewHeightPx);
	}

	private static int clampAxis(int scroll, int mapPx, int viewPx)
	{
		int max = mapPx - viewPx;
		if (max <= 0) {
			// Map narrower/shorter than the view: pin to the top-left.
			return 0;
		}
		return clamp(scroll, 0, max);
	}

	private static int clamp(int v, int lo, int hi)
	{
		if (v < lo) return lo;
		if (v > hi) return hi;
		return v;
	}
}
