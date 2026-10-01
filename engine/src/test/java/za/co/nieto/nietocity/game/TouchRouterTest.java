/*
 * NietoCity - touch routing decision tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.*;
import org.junit.Test;

import micropolisj.engine.MicropolisTool;
import za.co.nieto.nietocity.game.TouchRouter.Action;
import za.co.nieto.nietocity.game.TouchRouter.Phase;

public class TouchRouterTest
{
	private static final MicropolisTool[] ARMED = {
		MicropolisTool.BULLDOZER, MicropolisTool.WIRE, MicropolisTool.ROADS,
		MicropolisTool.RAIL, MicropolisTool.PARK, MicropolisTool.RESIDENTIAL,
		MicropolisTool.COMMERCIAL, MicropolisTool.INDUSTRIAL, MicropolisTool.FIRE,
		MicropolisTool.POLICE, MicropolisTool.POWERPLANT, MicropolisTool.NUCLEAR,
		MicropolisTool.STADIUM, MicropolisTool.SEAPORT, MicropolisTool.AIRPORT
	};

	@Test
	public void armedToolWithOneFingerBuilds() {
		for (MicropolisTool t : ARMED) {
			assertEquals(t + " down", Action.BUILD, TouchRouter.decide(t, 1, Phase.DOWN));
			assertEquals(t + " move", Action.BUILD, TouchRouter.decide(t, 1, Phase.MOVE));
		}
	}

	@Test
	public void twoFingersAlwaysPan() {
		for (MicropolisTool t : ARMED) {
			assertEquals(t + " down", Action.PAN, TouchRouter.decide(t, 2, Phase.DOWN));
			assertEquals(t + " move", Action.PAN, TouchRouter.decide(t, 2, Phase.MOVE));
			assertEquals(t + " three", Action.PAN, TouchRouter.decide(t, 3, Phase.MOVE));
		}
		assertEquals(Action.PAN, TouchRouter.decide(null, 2, Phase.DOWN));
		assertEquals(Action.PAN, TouchRouter.decide(null, 2, Phase.MOVE));
	}

	@Test
	public void noToolWithOneFingerPans() {
		assertEquals(Action.PAN, TouchRouter.decide(null, 1, Phase.DOWN));
		assertEquals(Action.PAN, TouchRouter.decide(null, 1, Phase.MOVE));
	}

	@Test
	public void longPressQueriesWithOrWithoutATool() {
		assertEquals(Action.QUERY, TouchRouter.decide(null, 1, Phase.LONG_PRESS));
		for (MicropolisTool t : ARMED) {
			assertEquals(t + "", Action.QUERY, TouchRouter.decide(t, 1, Phase.LONG_PRESS));
		}
	}
}
