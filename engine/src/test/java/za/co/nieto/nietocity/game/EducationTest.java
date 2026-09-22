/*
 * NietoCity - educational registry completeness test.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Proves that the shared Explain registry has a card for every tool, every map
 * overlay and every city problem the engine defines (so nothing is missed if the
 * engine ever gains one), plus the three basics and a non-trivial reference text.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import micropolisj.engine.CityProblem;
import micropolisj.engine.MicropolisTool;
import za.co.nieto.nietocity.render.MapOverlay;

public class EducationTest
{
	@Test
	public void everyToolHasACard()
	{
		for (MicropolisTool t : MicropolisTool.values()) {
			assertNotNull("no Explain card for tool " + t, Education.forTool(t));
		}
		assertEquals(MicropolisTool.values().length, Education.tools().size());
	}

	@Test
	public void everyOverlayHasACardExceptNone()
	{
		for (MapOverlay o : MapOverlay.values()) {
			if (o == MapOverlay.NONE) {
				assertNull("NONE should have no card", Education.forOverlay(o));
			} else {
				assertNotNull("no Explain card for overlay " + o, Education.forOverlay(o));
			}
		}
		assertEquals(MapOverlay.values().length - 1, Education.overlays().size());
	}

	@Test
	public void everyProblemHasACard()
	{
		for (CityProblem p : CityProblem.values()) {
			assertNotNull("no Explain card for problem " + p, Education.forProblem(p));
		}
		assertEquals(CityProblem.values().length, Education.problems().size());
	}

	@Test
	public void basicsAndReferencePresent()
	{
		assertNotNull(Education.POWER);
		assertNotNull(Education.ROAD_ACCESS);
		assertNotNull(Education.DEMAND);
		String ref = Education.fullReference();
		assertTrue(ref.contains("THE BASICS"));
		assertTrue(ref.contains("TOOLS"));
		assertTrue(ref.contains("MAP OVERLAYS"));
		assertTrue(ref.length() > 1000);
	}
}
