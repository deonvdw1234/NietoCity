/*
 * NietoCity - bundled-sound resolution test.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Proves every Sound the engine can emit maps to a wav bundled on the classpath,
 * so the platform sound players can preload them all. BULLDOZE is intentionally
 * silent in the engine (no wav name), so it is expected to have no file.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import micropolisj.engine.Sound;

public class SoundTest
{
	@Test
	public void everySoundMapsToABundledWav()
	{
		for (Sound s : Sound.values()) {
			if (s == Sound.BULLDOZE) {
				// Deliberately silent in the engine.
				assertNull("BULLDOZE has no wav", s.getAudioFile());
			} else {
				assertNotNull(s + " should resolve to a bundled wav", s.getAudioFile());
			}
		}
	}
}
