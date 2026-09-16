/*
 * NietoCity - animation clock tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.render;

import static org.junit.Assert.*;
import org.junit.Test;

import micropolisj.engine.Micropolis;
import micropolisj.engine.NietoDemo;
import micropolisj.engine.Speed;

public class AnimationClockTest
{
	/** Each fire advances the engine's animation cycle by simStepsPerUpdate. */
	@Test
	public void tickAdvancesAnimationCycle()
	{
		Micropolis city = NietoDemo.newDemoCity();
		AnimationClock clock = new AnimationClock(city, null);

		int before = clock.animationCycle();
		for (int i = 0; i < 10; i++) {
			clock.tickOnce(Speed.NORMAL);
		}
		assertEquals((before + 10) % 960, clock.animationCycle());
	}

	/** A few fires actually run the simulation (population grows in the demo city). */
	@Test
	public void tickRunsTheSimulation()
	{
		Micropolis city = NietoDemo.newDemoCity();
		AnimationClock clock = new AnimationClock(city, null);
		assertEquals(0, city.getCityPopulation());

		// 200 sim steps require ~400 animate() calls (one step every 2nd call).
		for (int i = 0; i < 400; i++) {
			clock.tickOnce(Speed.NORMAL);
		}
		assertTrue("simulation should have advanced", city.getCityPopulation() > 0);
	}

	/** start()/stop() are safe to call and toggle the running flag. */
	@Test
	public void startStopLifecycle() throws InterruptedException
	{
		Micropolis city = NietoDemo.newDemoCity();
		final int[] fires = { 0 };
		AnimationClock clock = new AnimationClock(city, new AnimationClock.Listener() {
			public void onAnimated() { fires[0]++; }
		});
		clock.setSpeed(Speed.FAST);
		clock.start();
		assertTrue(clock.isRunning());
		Thread.sleep(200);
		clock.stop();
		assertFalse(clock.isRunning());
		assertTrue("clock should have fired at least once", fires[0] > 0);
	}
}
