/*
 * NietoCity - speed control tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Proves the shared speed control: setting a chosen speed makes the controller
 * report that speed's tick interval, the tap-cycle order is SLOW->NORMAL->FAST->
 * SUPER_FAST->SLOW, pause is separate (unpausing returns to the chosen speed),
 * and PAUSED stops ticks (one pump does not advance the engine animation cycle).
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import micropolisj.engine.Speed;

public class SpeedTest
{
	private static final Speed[] RUN_SPEEDS =
		{ Speed.SLOW, Speed.NORMAL, Speed.FAST, Speed.SUPER_FAST };

	@Test
	public void chosenSpeedSetsTickInterval()
	{
		GameController gc = GameController.newGame();
		for (Speed s : RUN_SPEEDS) {
			gc.setChosenSpeed(s);
			assertFalse(gc.isPaused());
			assertEquals(s, gc.getEffectiveSpeed());
			assertEquals(s.animationDelay, gc.tickIntervalMs());
		}
	}

	@Test
	public void tapCycleOrder()
	{
		GameController gc = GameController.newGame();
		gc.setChosenSpeed(Speed.SLOW);
		assertEquals(Speed.NORMAL, gc.cycleSpeed());
		assertEquals(Speed.FAST, gc.cycleSpeed());
		assertEquals(Speed.SUPER_FAST, gc.cycleSpeed());
		assertEquals(Speed.SLOW, gc.cycleSpeed());
	}

	@Test
	public void pauseIsSeparateFromChosenSpeed()
	{
		GameController gc = GameController.newGame();
		gc.setChosenSpeed(Speed.FAST);
		gc.setPaused(true);
		assertEquals(Speed.PAUSED, gc.getEffectiveSpeed());
		assertEquals(Speed.PAUSED.animationDelay, gc.tickIntervalMs());
		// Resuming returns to the chosen speed, not NORMAL.
		gc.setPaused(false);
		assertEquals(Speed.FAST, gc.getEffectiveSpeed());
	}

	@Test
	public void pausedStopsTicks()
	{
		GameController gc = GameController.newGame();
		gc.setChosenSpeed(Speed.NORMAL);
		gc.setPaused(true);
		int cycle0 = gc.animationCycle();
		assertFalse("a paused pump does not tick", gc.pumpOnceForTest());
		assertEquals("paused: the animation cycle stays put", cycle0, gc.animationCycle());
		// Resume: a pump now ticks and advances the cycle.
		gc.setPaused(false);
		assertTrue("a running pump ticks", gc.pumpOnceForTest());
		assertNotEquals("running: the animation cycle advances", cycle0, gc.animationCycle());
	}

	@Test
	public void setChosenSpeedIgnoresPausedAndNull()
	{
		GameController gc = GameController.newGame();
		gc.setChosenSpeed(Speed.FAST);
		gc.setChosenSpeed(Speed.PAUSED); // ignored
		gc.setChosenSpeed(null);         // ignored
		assertEquals(Speed.FAST, gc.getChosenSpeed());
	}
}
