/*
 * NietoCity - Android sound player (SoundPool).
 * Created by Nieto Software.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 3 or later. See LICENSE.
 */
package za.co.nieto.nietocity

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import micropolisj.engine.Sound
import za.co.nieto.nietocity.game.SoundPlayer
import java.io.File
import java.net.URL

/**
 * Plays the engine's sounds with a SoundPool. The wavs are bundled in the engine
 * resources (on the classpath), which SoundPool cannot open directly, so each is
 * extracted once to a cache file at start-up and loaded from there. Everything is
 * best-effort: a missing or broken clip is simply silent, never an exception.
 */
class AndroidSoundPlayer(context: Context) : SoundPlayer {

    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = HashMap<Sound, Int>()
    @Volatile private var muted = false

    init {
        val byUrl = HashMap<String, Int>()
        for (s in Sound.values()) {
            val url = s.audioFile ?: continue
            val id = byUrl.getOrPut(url.toString()) { loadFromUrl(context, url) }
            if (id > 0) soundIds[s] = id
        }
    }

    private fun loadFromUrl(context: Context, url: URL): Int {
        return try {
            val tmp = File.createTempFile("nieto-snd", ".wav", context.cacheDir)
            tmp.deleteOnExit()
            url.openStream().use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
            pool.load(tmp.absolutePath, 1)
        } catch (e: Exception) {
            -1
        }
    }

    override fun play(sound: Sound) {
        if (muted) return
        val id = soundIds[sound] ?: return
        if (id <= 0) return
        try {
            pool.play(id, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            // best-effort: ignore
        }
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
    }

    override fun isMuted(): Boolean = muted

    override fun release() {
        try {
            pool.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
