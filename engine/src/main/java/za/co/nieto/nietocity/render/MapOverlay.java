/*
 * NietoCity - shared overlay layer.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * The data overlays the player can tint the map with: none, plus population
 * density, pollution, crime, land value, traffic, the power grid, and fire and
 * police coverage. Each overlay turns a tile into an opaque ARGB colour (0 means
 * "nothing to show here"); the main-map renderer draws it translucently over the
 * tile art, and the mini map replaces the base tile colour with it. The colour
 * ramp mirrors MicropolisJ's OverlayMapView (getCI): low grey, medium yellow,
 * high orange, very-high red; the power grid shows powered zones red, unpowered
 * blue and bare power lines grey. Pure Java 8; callers hold the engine lock.
 */
package za.co.nieto.nietocity.render;

import micropolisj.engine.Micropolis;
import micropolisj.engine.TileConstants;

public enum MapOverlay
{
	NONE("None"),
	POPULATION("Population density"),
	POLLUTION("Pollution"),
	CRIME("Crime"),
	LAND_VALUE("Land value"),
	TRAFFIC("Traffic"),
	POWER("Power grid"),
	FIRE("Fire coverage"),
	POLICE("Police coverage");

	private final String label;

	MapOverlay(String label) { this.label = label; }

	/** Human-readable name for the picker. */
	public String label() { return label; }

	// Ramp colours (opaque ARGB), matching OverlayMapView.getCI.
	private static final int VAL_LOW      = 0xFFBFBFBF;
	private static final int VAL_MEDIUM   = 0xFFFFFF00;
	private static final int VAL_HIGH     = 0xFFFF7F00;
	private static final int VAL_VERYHIGH = 0xFFFF0000;

	// Power grid colours.
	private static final int POWER_ON   = 0xFFFF0000; // powered zone centre
	private static final int POWER_OFF  = 0xFF6666E6; // unpowered zone centre
	private static final int POWER_WIRE = 0xFFBFBFBF; // conductive (lines, zone body)

	private static final int NATURAL_MAX_TILE = 63; // tiles 0..63 are natural terrain

	/**
	 * The overlay colour at a tile (opaque ARGB), or 0 when there is nothing to
	 * show there (so the base tile shows through). {@code tile} is masked to LOMASK.
	 */
	public int colorAt(Micropolis city, int x, int y, int tile)
	{
		switch (this) {
		case POPULATION: return ramp(city.getPopulationDensityAt(x, y));
		case POLLUTION:  return ramp(10 + city.getPollutionAt(x, y));
		case CRIME:      return ramp(city.getCrimeAt(x, y));
		case LAND_VALUE: return ramp(city.getLandValue(x, y));
		case TRAFFIC:    return ramp(city.getTrafficDensity(x, y));
		case FIRE:       return ramp(city.getFireStationCoverage(x, y));
		case POLICE:     return ramp(city.getPoliceCoverage(x, y));
		case POWER:      return powerColor(city, x, y, tile);
		default:         return 0;
		}
	}

	private static int ramp(int v)
	{
		if (v < 50)  return 0;
		if (v < 100) return VAL_LOW;
		if (v < 150) return VAL_MEDIUM;
		if (v < 200) return VAL_HIGH;
		return VAL_VERYHIGH;
	}

	private static int powerColor(Micropolis city, int x, int y, int tile)
	{
		if (tile <= NATURAL_MAX_TILE) {
			return 0; // natural terrain conducts nothing
		}
		if (TileConstants.isZoneCenter(tile)) {
			return city.isTilePowered(x, y) ? POWER_ON : POWER_OFF;
		}
		if (TileConstants.isConductive(tile)) {
			return POWER_WIRE;
		}
		return 0;
	}
}
