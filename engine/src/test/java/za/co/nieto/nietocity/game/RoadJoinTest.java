/*
 * NietoCity - road joining across strokes and along a dragged path.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Task 0 (Phase 4a). Findings the tests pin down:
 *  - The engine already joins orthogonally-adjacent roads placed in SEPARATE
 *    strokes (its fixZone fixes each laid tile AND its four neighbours, so it
 *    reaches into the previous stroke). The two "separate strokes" tests prove
 *    this and guard it; the engine is not modified.
 *  - The real gap Andre saw comes from applying a drag as ONE axis-snapped
 *    stroke: a bent drag ends away from the finger (see gapProof). GameController's
 *    path following lays the road along the finger's waypoints instead, so an
 *    L-drag lays a connected L and charges each tile once.
 *
 * Road tile pieces (LOMASK): ROADS=66 (E-W), ROADS2=67 (N-S).
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.*;
import org.junit.Test;

import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.TileConstants;

public class RoadJoinTest
{
	private static final int ROADS_EW = 66;
	private static final int ROADS_NS = 67;
	private static final int DIRT = 0;

	private static int tile(Micropolis c, int x, int y) {
		return c.getTile(x, y) & TileConstants.LOMASK;
	}

	private static Micropolis funded() {
		Micropolis c = new Micropolis();
		c.setFunds(1_000_000);
		return c;
	}

	// --- engine behaviour: separate strokes DO join (mandated test) ---

	@Test
	public void twoRoadsHorizontallyAdjacentInSeparateStrokesJoin() {
		Micropolis c = funded();
		GameController gc = new GameController(c);
		gc.applyNow(MicropolisTool.ROADS, 40, 40, 40, 40); // stroke 1
		gc.applyNow(MicropolisTool.ROADS, 41, 40, 41, 40); // stroke 2 (east)
		assertTrue("west tile is road", TileConstants.isRoad(tile(c, 40, 40)));
		assertTrue("east tile is road", TileConstants.isRoad(tile(c, 41, 40)));
		assertTrue("west connects east", TileConstants.roadConnectsEast(tile(c, 40, 40)));
		assertTrue("east connects west", TileConstants.roadConnectsWest(tile(c, 41, 40)));
		assertEquals(ROADS_EW, tile(c, 40, 40));
		assertEquals(ROADS_EW, tile(c, 41, 40));
	}

	@Test
	public void twoRoadsVerticallyAdjacentInSeparateStrokesJoin() {
		Micropolis c = funded();
		GameController gc = new GameController(c);
		gc.applyNow(MicropolisTool.ROADS, 40, 40, 40, 40); // stroke 1
		gc.applyNow(MicropolisTool.ROADS, 40, 41, 40, 41); // stroke 2 (south)
		assertTrue("north connects south", TileConstants.roadConnectsSouth(tile(c, 40, 40)));
		assertTrue("south connects north", TileConstants.roadConnectsNorth(tile(c, 40, 41)));
		// Both re-fix to the N-S piece, i.e. they visibly join.
		assertEquals(ROADS_NS, tile(c, 40, 40));
		assertEquals(ROADS_NS, tile(c, 40, 41));
	}

	// --- our layer: a bent drag applied as one stroke leaves a gap ---

	@Test
	public void singleStrokeSnapsToOneAxisAndLeavesAGap() {
		Micropolis c = funded();
		GameController gc = new GameController(c);
		// A bent drag 40,40 -> 43,42 sent as one stroke: |dx|=3 > |dy|=2, so the
		// engine snaps it to row 40 and never reaches the finger at (43,42).
		gc.applyNow(MicropolisTool.ROADS, 40, 40, 43, 42);
		assertTrue("road laid along row 40", TileConstants.isRoad(tile(c, 43, 40)));
		assertEquals("finger tile left empty (the gap)", DIRT, tile(c, 43, 42));
	}

	// --- our layer fix: path following lays the connected L, charging once ---

	@Test
	public void pathFollowingLaysConnectedLShapeChargingOncePerTile() {
		Micropolis c = funded();
		int fundsBefore = c.budget.totalFunds;
		GameController gc = new GameController(c);
		// Finger waypoints: 40,40 -> 43,40 -> 43,42 (a right angle).
		int[] xs = { 40, 43, 43 };
		int[] ys = { 40, 40, 42 };
		gc.applyPathNow(MicropolisTool.ROADS, xs, ys);

		// The six tiles of the L are all road and connected end to end.
		int[][] path = { {40,40}, {41,40}, {42,40}, {43,40}, {43,41}, {43,42} };
		for (int[] p : path) {
			assertTrue("road at " + p[0] + "," + p[1], TileConstants.isRoad(tile(c, p[0], p[1])));
		}
		// The finger tile that the single stroke left empty is now road.
		assertTrue("finger tile filled", TileConstants.isRoad(tile(c, 43, 42)));
		// The corner turns: it connects west (into the arm) and south (down the leg).
		assertTrue("corner connects west", TileConstants.roadConnectsWest(tile(c, 43, 40)));
		assertTrue("corner connects south", TileConstants.roadConnectsSouth(tile(c, 43, 40)));

		// Charged once per tile: 6 road tiles at 10 each, corner not double-charged.
		assertEquals(6 * 10, fundsBefore - c.budget.totalFunds);
	}

	@Test
	public void tapIsASingleWaypointPath() {
		Micropolis c = funded();
		GameController gc = new GameController(c);
		gc.setTool(MicropolisTool.ROADS);
		gc.applyPathNow(MicropolisTool.ROADS, new int[] { 50 }, new int[] { 50 });
		assertTrue("single tap places one road tile", TileConstants.isRoad(tile(c, 50, 50)));
	}
}
