/*
 * NietoCity - selected-tool single source of truth tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Every path that changes the selected tool must leave ONE value (the
 * controller's) and tell the listeners (palette, bottom bar, map) about it,
 * including when New City / Load swaps the controller.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.*;
import org.junit.Test;

import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.ToolResult;

public class ToolSelectionTest
{
	/** Records what a UI observer (palette, bar, map) last heard. */
	private static final class Recorder implements GameController.ToolListener
	{
		boolean heard;
		MicropolisTool last;
		int count;

		public void toolChanged(MicropolisTool tool) {
			heard = true;
			last = tool;
			count++;
		}
	}

	private static GameController funded() {
		Micropolis c = new Micropolis();
		c.setFunds(1_000_000);
		return new GameController(c);
	}

	@Test
	public void addingAListenerDeliversTheCurrentTool() {
		GameController gc = funded();
		gc.setTool(MicropolisTool.RAIL);
		Recorder rec = new Recorder();
		gc.addToolListener(rec);
		assertTrue("a new observer is synced at once", rec.heard);
		assertEquals(MicropolisTool.RAIL, rec.last);
	}

	@Test
	public void selectThenReselectDeselects() {
		GameController gc = funded();
		Recorder rec = new Recorder();
		gc.addToolListener(rec);
		gc.toggleTool(MicropolisTool.ROADS);
		assertEquals(MicropolisTool.ROADS, gc.getTool());
		assertEquals("listener told of select", MicropolisTool.ROADS, rec.last);
		gc.toggleTool(MicropolisTool.ROADS);
		assertNull(gc.getTool());
		assertTrue(rec.heard);
		assertNull("listener told of deselect", rec.last);
	}

	@Test
	public void clearXReturnsToPanAndNotifies() {
		GameController gc = funded();
		Recorder rec = new Recorder();
		gc.addToolListener(rec);
		gc.toggleTool(MicropolisTool.WIRE);
		assertEquals(MicropolisTool.WIRE, rec.last);
		gc.setTool(null);
		assertNull(gc.getTool());
		assertNull("listener told of X", rec.last);
	}

	@Test
	public void oneShotAfterPlacementNotifiesPan() {
		GameController gc = funded();
		Recorder rec = new Recorder();
		gc.addToolListener(rec);
		gc.toggleTool(MicropolisTool.POWERPLANT);
		assertEquals(MicropolisTool.POWERPLANT, rec.last);
		ToolResult r = gc.applyPathNow(MicropolisTool.POWERPLANT, new int[] { 40 }, new int[] { 40 });
		assertEquals(ToolResult.SUCCESS, r);
		assertNull(gc.getTool());
		assertNull("listener told of the one-shot auto-clear", rec.last);
	}

	@Test
	public void controllerSwapRebindsTheListener() {
		GameController old = funded();
		Recorder rec = new Recorder();
		old.addToolListener(rec);
		old.toggleTool(MicropolisTool.POWERPLANT);

		// New City / Load: the UI's observers move to the fresh controller.
		GameController fresh = funded();
		old.transferToolListeners(fresh);
		assertEquals("listener synced to the fresh controller (Pan)", fresh.getTool(), rec.last);
		assertNull(rec.last);

		// A palette tap now goes to, and is reported from, the fresh controller...
		fresh.toggleTool(MicropolisTool.POWERPLANT);
		assertEquals(MicropolisTool.POWERPLANT, fresh.getTool());
		assertEquals(MicropolisTool.POWERPLANT, rec.last);

		// ...and the discarded controller no longer reaches the UI.
		int before = rec.count;
		old.toggleTool(MicropolisTool.STADIUM);
		assertEquals("old controller is detached", before, rec.count);
		assertEquals(fresh.getTool(), rec.last);
	}
}
