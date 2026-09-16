/*
 * NietoCity - platform-neutral animation/engine clock.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Runs the simulation on its own thread at the original MicropolisJ cadence: it
 * fires every Speed.animationDelay milliseconds and calls city.animate()
 * Speed.simStepsPerUpdate times per fire (animate() advances the animation cycle
 * and steps the simulation every second call). After each fire it notifies a
 * listener so the platform renderer can repaint. Pure Java 8, no awt/android.
 *
 * Reads/writes to the engine are synchronised on the Micropolis instance; the
 * platform renderer must synchronise on the same instance while reading tiles.
 */
package za.co.nieto.nietocity.render;

import micropolisj.engine.Micropolis;
import micropolisj.engine.Speed;

public final class AnimationClock
{
	/** Notified after each animation fire, on the clock thread. */
	public interface Listener
	{
		void onAnimated();
	}

	private final Micropolis engine;
	private final Listener listener;
	private volatile Speed speed = Speed.NORMAL;
	private volatile boolean running;
	private Thread thread;

	public AnimationClock(Micropolis engine, Listener listener)
	{
		this.engine = engine;
		this.listener = listener;
	}

	public void setSpeed(Speed speed)
	{
		this.speed = speed;
	}

	public Speed getSpeed()
	{
		return speed;
	}

	/** The engine's current animation cycle, used to choose animation frames. */
	public int animationCycle()
	{
		return engine.getAnimationCycle();
	}

	public synchronized void start()
	{
		if (running) {
			return;
		}
		running = true;
		thread = new Thread(new Runnable() {
			public void run() { loop(); }
		}, "nieto-engine");
		thread.setDaemon(true);
		thread.start();
	}

	public synchronized void stop()
	{
		running = false;
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	public boolean isRunning()
	{
		return running;
	}

	private void loop()
	{
		while (running) {
			Speed s = speed;
			if (s != Speed.PAUSED) {
				tickOnce(s);
			}
			if (listener != null) {
				listener.onAnimated();
			}
			try {
				Thread.sleep(s.animationDelay);
			}
			catch (InterruptedException e) {
				break;
			}
		}
	}

	/**
	 * Perform one timer fire's worth of animation. Package-visible and separate
	 * from the thread loop so it can be driven directly from tests.
	 */
	void tickOnce(Speed s)
	{
		synchronized (engine) {
			for (int i = 0; i < s.simStepsPerUpdate; i++) {
				engine.animate();
			}
		}
	}
}
