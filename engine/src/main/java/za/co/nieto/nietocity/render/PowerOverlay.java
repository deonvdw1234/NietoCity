/*
 * NietoCity - shared power indicator (blinking lightning bolt).
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Faithful to MicropolisJ's drawing area: an unpowered zone centre blinks a
 * yellow lightning bolt (the LIGHTNINGBOLT tile). The blink is driven from the
 * engine's animation cycle so it toggles about every half second while the sim
 * runs and holds steady while it is paused (the cycle only advances when the
 * sim animates). Pure Java 8, no awt/android; both renderers call it.
 */
package za.co.nieto.nietocity.render;

import micropolisj.engine.Micropolis;
import micropolisj.engine.TileConstants;

public final class PowerOverlay
{
	/** The lightning-bolt tile drawn over an unpowered zone centre. */
	public static final int LIGHTNINGBOLT = TileConstants.LIGHTNINGBOLT;

	// The animation cycle advances by one each engine animate() call (125 ms at
	// NORMAL). Four cycles on, four off gives an ~0.5 s blink at NORMAL speed.
	private static final int HALF_PERIOD = 4;

	private PowerOverlay() { }

	/**
	 * The on/off phase of the blink for a given engine animation cycle. On for
	 * ~half a second, then off; frozen (steady) while the sim is paused.
	 */
	public static boolean blinkOn(int animationCycle)
	{
		return (Math.floorMod(animationCycle, 2 * HALF_PERIOD)) < HALF_PERIOD;
	}

	/**
	 * Whether a lightning bolt should be drawn over the tile right now: it is an
	 * unpowered zone centre and the blink is currently on. Callers must already
	 * hold the engine lock (they read tiles under it).
	 */
	public static boolean showBolt(Micropolis city, int x, int y, int animationCycle)
	{
		if (!blinkOn(animationCycle)) {
			return false;
		}
		int tile = city.getTile(x, y) & TileConstants.LOMASK;
		return TileConstants.isZoneCenter(tile) && !city.isTilePowered(x, y);
	}
}
