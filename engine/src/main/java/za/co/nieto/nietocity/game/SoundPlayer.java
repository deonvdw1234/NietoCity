/*
 * NietoCity - platform sound player interface.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * The engine only reports which sound should play (the Sound enum, via its
 * citySound listener); actually playing it is the front-end's job. GameController
 * routes those reports to a SoundPlayer that each platform implements over its own
 * audio API (Android SoundPool, desktop JavaFX AudioClip). Sound is best-effort:
 * an implementation must never throw from play() - a missing or broken clip is
 * simply silent. Pure Java 8 interface; no audio API in the engine module.
 */
package za.co.nieto.nietocity.game;

import micropolisj.engine.Sound;

public interface SoundPlayer
{
	/** Play the given sound now, unless muted. Never throws; failures are silent. */
	void play(Sound sound);

	/** Mute or unmute all sound. */
	void setMuted(boolean muted);

	boolean isMuted();

	/** Release any audio resources (called when the UI is going away). */
	void release();
}
