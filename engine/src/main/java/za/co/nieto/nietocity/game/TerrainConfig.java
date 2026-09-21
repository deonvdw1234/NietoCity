/*
 * NietoCity - map generator terrain configuration.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * A small immutable description of the terrain choices on the New City screen -
 * island mode and the lake, river and tree levels - and how they apply to the
 * engine's MapGenerator. The lake/river/tree choices are a friendly Auto/None/
 * Low/High that map to the engine's -1 (auto) / 0 (none) / >0 (level) values.
 * Pure Java 8.
 */
package za.co.nieto.nietocity.game;

import micropolisj.engine.MapGenerator;

public final class TerrainConfig
{
	/** Island generation mode. */
	public enum Island
	{
		NONE(0), SELDOM(1), ALWAYS(2);
		private final int mode;
		Island(int mode) { this.mode = mode; }
		public int mode() { return mode; }
	}

	/** A four-way terrain level; the engine value depends on the feature. */
	public enum Level { AUTO, NONE, LOW, HIGH }

	// Concrete engine levels for LOW/HIGH per feature (AUTO=-1, NONE=0).
	private static final int LAKE_LOW = 8,  LAKE_HIGH = 30;
	private static final int CURVE_LOW = 10, CURVE_HIGH = 100;
	private static final int TREE_LOW = 10,  TREE_HIGH = 100;

	public final Island island;
	public final Level lake;
	public final Level river;
	public final Level trees;

	public TerrainConfig(Island island, Level lake, Level river, Level trees)
	{
		this.island = island != null ? island : Island.SELDOM;
		this.lake = lake != null ? lake : Level.AUTO;
		this.river = river != null ? river : Level.AUTO;
		this.trees = trees != null ? trees : Level.AUTO;
	}

	/** The engine defaults (seldom island, everything auto). */
	public static TerrainConfig defaults()
	{
		return new TerrainConfig(Island.SELDOM, Level.AUTO, Level.AUTO, Level.AUTO);
	}

	/** Apply these settings to a generator (before generateSomeCity). */
	public void applyTo(MapGenerator gen)
	{
		gen.setCreateIslandMode(island.mode());
		gen.setLakeLevel(levelValue(lake, LAKE_LOW, LAKE_HIGH));
		gen.setCurveLevel(levelValue(river, CURVE_LOW, CURVE_HIGH));
		gen.setTreeLevel(levelValue(trees, TREE_LOW, TREE_HIGH));
	}

	private static int levelValue(Level l, int low, int high)
	{
		switch (l) {
		case NONE: return 0;
		case LOW:  return low;
		case HIGH: return high;
		default:   return -1; // AUTO
		}
	}
}
