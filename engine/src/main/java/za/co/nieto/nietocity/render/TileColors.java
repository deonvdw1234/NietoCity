/*
 * NietoCity - tile colour classifier for the overview (mini) map.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Maps a map tile to one representative opaque ARGB colour, so the mini map can
 * draw the whole city one pixel per tile (the classic Micropolis overview look).
 * Classification uses MicropolisJ's public tile predicates where they exist, and
 * a few tile-number ranges (documented, from TileConstants / tiles.rc) for the
 * natural terrain that has no predicate. Pure Java 8, no awt/android.
 */
package za.co.nieto.nietocity.render;

import micropolisj.engine.TileConstants;

public final class TileColors
{
	// Tile-number ranges for natural terrain (from TileConstants / tiles.rc).
	private static final int DIRT = 0;
	private static final int RIVER = 2;
	private static final int LAST_RIVER_EDGE = 20;
	private static final int FLOOD = 48;
	private static final int LAST_FLOOD = 51;
	private static final int RADIATION = 52;
	private static final int FIRE = 56;
	private static final int ROAD_BASE = 64;

	// Opaque ARGB colours (classic-overview palette).
	private static final int C_LAND      = 0xFFA98A56; // bare dirt
	private static final int C_WATER     = 0xFF2A5CAA;
	private static final int C_WOODS     = 0xFF4C7A34; // muted, to differ from R zones
	private static final int C_RUBBLE    = 0xFF8D7B60;
	private static final int C_FLOOD     = 0xFF4FA3E0;
	private static final int C_RADIATION = 0xFFCC33CC;
	private static final int C_FIRE      = 0xFFFF7000;
	private static final int C_ROAD      = 0xFF5A5A5A;
	private static final int C_RAIL      = 0xFF303030;
	private static final int C_WIRE      = 0xFFC8C864;
	private static final int C_RES       = 0xFF3CE03C; // residential (bright green)
	private static final int C_COM       = 0xFF4040F0; // commercial (blue)
	private static final int C_IND       = 0xFFF0F03C; // industrial (yellow)
	private static final int C_SERVICE   = 0xFFB8B8B8; // services & big buildings
	private static final int C_BUILDING  = 0xFF9A9A9A; // any other constructed tile

	private TileColors() { }

	/** The overview colour for a tile (already masked to LOMASK). Always opaque. */
	public static int colorOf(int tile)
	{
		if (tile == DIRT) {
			return C_LAND;
		}
		if (tile >= RIVER && tile <= LAST_RIVER_EDGE) {
			return C_WATER;
		}
		if (TileConstants.isTree((char) tile)) {
			return C_WOODS;
		}
		if (TileConstants.isRubble(tile)) {
			return C_RUBBLE;
		}
		if (tile >= FLOOD && tile <= LAST_FLOOD) {
			return C_FLOOD;
		}
		if (tile == RADIATION) {
			return C_RADIATION;
		}
		if (tile >= FIRE && tile < ROAD_BASE) {
			return C_FIRE;
		}
		// Constructed tiles. Roads and rail first (they overlap "conductive").
		if (TileConstants.isRoad(tile)) {
			return C_ROAD;
		}
		if (TileConstants.isRail(tile)) {
			return C_RAIL;
		}
		if (TileConstants.isResidentialZoneAny(tile)) {
			return C_RES;
		}
		if (TileConstants.isCommercialZone(tile)) {
			return C_COM;
		}
		if (TileConstants.isIndustrialZone(tile)) {
			return C_IND;
		}
		if (TileConstants.isZoneAny(tile)) {
			return C_SERVICE; // police, fire, port, airport, plants, stadium, ...
		}
		if (TileConstants.isConductive(tile)) {
			return C_WIRE; // bare power lines
		}
		if (TileConstants.isConstructed(tile)) {
			return C_BUILDING;
		}
		return C_LAND;
	}
}
