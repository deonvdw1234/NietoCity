/*
 * NietoCity - tool guard (STROKE vs ONE_SHOT) tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.*;
import org.junit.Test;

import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.ToolResult;
import za.co.nieto.nietocity.game.GameController.ToolKind;

public class ToolGuardTest
{
	private static final char WATER = 2;

	private static Micropolis funded() {
		Micropolis c = new Micropolis();
		c.setFunds(1_000_000);
		return c;
	}

	@Test
	public void oneShotToolReturnsToPanAfterASuccessfulPlacement() {
		Micropolis c = funded();
		GameController gc = new GameController(c);
		gc.setTool(MicropolisTool.POLICE);
		ToolResult r = gc.applyPathNow(MicropolisTool.POLICE, new int[] { 40 }, new int[] { 40 });
		assertEquals(ToolResult.SUCCESS, r);
		assertNull("a one-shot tool returns to Pan after placing", gc.getTool());
	}

	@Test
	public void strokeToolStaysSelectedAfterPlacement() {
		Micropolis c = funded();
		GameController gc = new GameController(c);
		gc.setTool(MicropolisTool.ROADS);
		gc.applyPathNow(MicropolisTool.ROADS, new int[] { 40 }, new int[] { 40 });
		assertEquals("a stroke tool stays selected", MicropolisTool.ROADS, gc.getTool());
	}

	@Test
	public void oneShotToolStaysSelectedWhenPlacementFails() {
		Micropolis c = funded();
		for (int y = 38; y <= 42; y++) {
			for (int x = 38; x <= 42; x++) {
				c.setTile(x, y, WATER);
			}
		}
		GameController gc = new GameController(c);
		gc.setTool(MicropolisTool.STADIUM);
		ToolResult r = gc.applyPathNow(MicropolisTool.STADIUM, new int[] { 40 }, new int[] { 40 });
		assertEquals(ToolResult.UH_OH, r);
		assertEquals("a failed placement keeps the one-shot tool", MicropolisTool.STADIUM, gc.getTool());
	}

	@Test
	public void toolKindClassification() {
		MicropolisTool[] oneShot = {
			MicropolisTool.POLICE, MicropolisTool.FIRE, MicropolisTool.STADIUM,
			MicropolisTool.SEAPORT, MicropolisTool.POWERPLANT, MicropolisTool.NUCLEAR,
			MicropolisTool.AIRPORT
		};
		for (MicropolisTool t : oneShot) {
			assertEquals(t + " is one-shot", ToolKind.ONE_SHOT, GameController.kindOf(t));
		}
		MicropolisTool[] stroke = {
			MicropolisTool.BULLDOZER, MicropolisTool.WIRE, MicropolisTool.ROADS,
			MicropolisTool.RAIL, MicropolisTool.PARK, MicropolisTool.RESIDENTIAL,
			MicropolisTool.COMMERCIAL, MicropolisTool.INDUSTRIAL
		};
		for (MicropolisTool t : stroke) {
			assertEquals(t + " is stroke", ToolKind.STROKE, GameController.kindOf(t));
		}
	}
}
