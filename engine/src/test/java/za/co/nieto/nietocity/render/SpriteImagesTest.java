/*
 * NietoCity - sprite frame resolution tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Proves every SpriteKind resolves all of its frame images on the classpath, so
 * the renderer can draw every train, car, helicopter, aeroplane, ship, monster,
 * tornado and explosion frame the engine produces.
 */
package za.co.nieto.nietocity.render;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import micropolisj.engine.SpriteKind;

public class SpriteImagesTest
{
	@Test
	public void everyKindResolvesAllFrames()
	{
		for (SpriteKind kind : SpriteKind.values()) {
			assertTrue("all frames present for " + kind, SpriteImages.hasAllFrames(kind));
			for (int i = 0; i < kind.numFrames; i++) {
				assertNotNull(kind + " frame " + i, SpriteImages.resource(kind, i));
			}
		}
	}

	@Test
	public void resourcePathMatchesTheBundledNaming()
	{
		assertTrue(SpriteImages.resourcePath(SpriteKind.GOD, 15).endsWith("/sprites/obj5-15.png"));
		assertTrue(SpriteImages.resourcePath(1, 0).endsWith("/sprites/obj1-0.png"));
	}
}
