/*
 * NietoCity - shared history graph data.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Extracts the engine's recorded history (Micropolis.History) into the six series
 * the graphs dialog draws - residential, commercial and industrial population,
 * crime, pollution and cash flow - for either the 10-year (monthly, indices
 * 0..119) or 120-year (yearly, indices 120..239) window, newest sample first.
 * Each series is auto-scaled to 0..1 over the window so its trend is visible
 * regardless of magnitude (the series share a graph but differ wildly in scale).
 * Both platforms draw the same values natively - no chart library. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

import micropolisj.engine.Micropolis;

public final class GraphData
{
	/** The number of samples in each window (10-year monthly, 120-year yearly). */
	public static final int POINTS = 120;

	/** The six graphed series, with display label and line colour (ARGB). */
	public enum Series
	{
		RESIDENTIAL("Residential", 0xFF00E600),
		COMMERCIAL("Commercial", 0xFF0000E6),
		INDUSTRIAL("Industrial", 0xFFFFFF00),
		CRIME("Crime", 0xFF7F0000),
		POLLUTION("Pollution", 0xFF997F4C),
		MONEY("Cash Flow", 0xFF007F00);

		private final String label;
		private final int colorArgb;

		Series(String label, int colorArgb) { this.label = label; this.colorArgb = colorArgb; }

		public String label() { return label; }
		public int colorArgb() { return colorArgb; }
	}

	private GraphData() { }

	/** Raw history values for a series and window, newest sample at index 0. */
	public static int[] values(Micropolis city, Series s, boolean longRange)
	{
		int base = longRange ? POINTS : 0;
		int[] out = new int[POINTS];
		synchronized (city) {
			int[] src = arrayFor(city, s);
			for (int i = 0; i < POINTS; i++) {
				out[i] = src[base + i];
			}
		}
		return out;
	}

	/**
	 * A series scaled to 0..1 over the window (each series by its own maximum, so a
	 * flat-but-nonzero series still shows). Newest sample at index 0.
	 */
	public static float[] normalized(Micropolis city, Series s, boolean longRange)
	{
		int[] v = values(city, s, longRange);
		int max = 1;
		for (int x : v) {
			if (x > max) {
				max = x;
			}
		}
		float[] out = new float[POINTS];
		for (int i = 0; i < POINTS; i++) {
			out[i] = v[i] / (float) max;
		}
		return out;
	}

	private static int[] arrayFor(Micropolis city, Series s)
	{
		switch (s) {
		case RESIDENTIAL: return city.history.res;
		case COMMERCIAL:  return city.history.com;
		case INDUSTRIAL:  return city.history.ind;
		case CRIME:       return city.history.crime;
		case POLLUTION:   return city.history.pollution;
		case MONEY:       return city.history.money;
		default:          return city.history.res;
		}
	}
}
