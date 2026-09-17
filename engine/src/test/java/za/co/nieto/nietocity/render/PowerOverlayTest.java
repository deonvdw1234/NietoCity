/*
 * NietoCity - power indicator (blinking lightning bolt) tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.render;

import static org.junit.Assert.*;
import org.junit.Test;

import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import za.co.nieto.nietocity.game.GameController;

public class PowerOverlayTest
{
	@Test
	public void blinkTogglesInHalfSecondBlocks()
	{
		// On for four cycles (~0.5s at NORMAL), then off for four.
		assertTrue(PowerOverlay.blinkOn(0));
		assertTrue(PowerOverlay.blinkOn(3));
		assertFalse(PowerOverlay.blinkOn(4));
		assertFalse(PowerOverlay.blinkOn(7));
		assertTrue(PowerOverlay.blinkOn(8));
	}

	@Test
	public void unpoweredZoneCentreBlinksTheBolt()
	{
		Micropolis c = new Micropolis();
		c.setFunds(1_000_000);
		GameController gc = new GameController(c);
		gc.applyNow(MicropolisTool.RESIDENTIAL, 40, 40, 40, 40);

		// The zone centre is unpowered (no plant): bolt shows while blink is on...
		assertTrue("bolt on when blink on", PowerOverlay.showBolt(c, 40, 40, 0));
		// ...and disappears on the off phase of the blink.
		assertFalse("no bolt on blink-off phase", PowerOverlay.showBolt(c, 40, 40, 4));
		// A plain (non-zone) tile never shows a bolt.
		assertFalse("no bolt on non-zone tile", PowerOverlay.showBolt(c, 0, 0, 0));
	}
}
