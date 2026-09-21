/*
 * NietoCity - desktop sound player (JavaFX AudioClip).
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Preloads each bundled engine sound into a JavaFX AudioClip once and plays it on
 * demand. Best-effort throughout: a clip that fails to load or play is simply
 * silent, never an exception. Runs on Java 8 with the JavaFX 8 runtime.
 */
package za.co.nieto.nietocity.desktop;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

import javafx.scene.media.AudioClip;

import micropolisj.engine.Sound;
import za.co.nieto.nietocity.game.SoundPlayer;

public final class DesktopSoundPlayer implements SoundPlayer
{
	private final Map<Sound, AudioClip> clips = new EnumMap<Sound, AudioClip>(Sound.class);
	private volatile boolean muted;

	public DesktopSoundPlayer()
	{
		for (Sound s : Sound.values()) {
			URL u = s.getAudioFile();
			if (u == null) {
				continue; // e.g. BULLDOZE is silent
			}
			try {
				clips.put(s, new AudioClip(u.toExternalForm()));
			} catch (Exception e) {
				// best-effort: this sound just won't play
			}
		}
	}

	public void play(Sound sound)
	{
		if (muted || sound == null) {
			return;
		}
		AudioClip clip = clips.get(sound);
		if (clip == null) {
			return;
		}
		try {
			clip.play();
		} catch (Exception e) {
			// best-effort: ignore
		}
	}

	public void setMuted(boolean muted)
	{
		this.muted = muted;
	}

	public boolean isMuted()
	{
		return muted;
	}

	public void release()
	{
		clips.clear();
	}
}
