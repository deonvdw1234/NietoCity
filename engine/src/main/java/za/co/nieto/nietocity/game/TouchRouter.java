/*
 * NietoCity - touch routing decision.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * The one place that decides what a touch on the map does, as a pure function of
 * the selected tool, the number of fingers down and the gesture phase. Both the
 * Android CityView and the desktop canvas ask it, so the rule is tested once.
 */
package za.co.nieto.nietocity.game;

import micropolisj.engine.MicropolisTool;

public final class TouchRouter
{
	/** What a touch does. */
	public enum Action { BUILD, PAN, QUERY }

	/** Where the gesture is: a finger going down, moving, or a long press. */
	public enum Phase { DOWN, MOVE, LONG_PRESS }

	private TouchRouter() { }

	/** Decide what a touch does (stub: not implemented yet). */
	public static Action decide(MicropolisTool selectedTool, int pointerCount, Phase phase)
	{
		return Action.PAN;
	}
}
