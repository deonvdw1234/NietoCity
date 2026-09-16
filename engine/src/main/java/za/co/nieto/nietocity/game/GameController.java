/*
 * NietoCity - shared game controller.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Owns the engine thread (via AnimationClock) and the currently selected tool.
 * Tool actions (beginStroke/dragTo/apply) run on the engine thread; reads that
 * need a consistent view (status snapshot, preview, query) take the engine lock.
 * Engine messages are collected into a queue the UI drains. Pure Java 8, no
 * awt/android, so both the Android app and the JavaFX desktop app share it.
 */
package za.co.nieto.nietocity.game;

import java.util.concurrent.ConcurrentLinkedQueue;

import micropolisj.engine.CityLocation;
import micropolisj.engine.GameLevel;
import micropolisj.engine.MapGenerator;
import micropolisj.engine.MapListener;
import micropolisj.engine.MapState;
import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.Sound;
import micropolisj.engine.Sprite;
import micropolisj.engine.Speed;
import micropolisj.engine.ToolPreview;
import micropolisj.engine.ToolResult;
import micropolisj.engine.ToolStroke;
import micropolisj.engine.ZoneStatus;
import za.co.nieto.nietocity.render.AnimationClock;

public final class GameController
{
	/** Delivers the result of an applied tool (on the engine thread). */
	public interface ResultCallback
	{
		void onResult(ToolResult result);
	}

	/** Difficulty levels (match the engine's GameLevel). Easy is the default. */
	public static final int LEVEL_EASY = 0;
	public static final int LEVEL_MEDIUM = 1;
	public static final int LEVEL_HARD = 2;

	private final Micropolis engine;
	private final AnimationClock clock;

	private volatile MicropolisTool tool;        // selected tool, or null (pan mode)
	private volatile Runnable frameCallback;     // renderer redraw hook
	private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();

	/** Create a fresh random city at the default (easy) level with its funds. */
	public static GameController newGame()
	{
		return newGame(LEVEL_EASY);
	}

	/**
	 * Create a fresh random city at the given level and apply that level's
	 * starting funds (easy 20000, medium 10000, hard 5000). The engine
	 * constructor leaves funds at 0; the original applies them when a city is
	 * created, which is what this does.
	 */
	public static GameController newGame(int gameLevel)
	{
		Micropolis city = new Micropolis();
		new MapGenerator(city).generateNewCity();
		city.setGameLevel(gameLevel);
		city.setFunds(GameLevel.getStartingFunds(gameLevel));
		return new GameController(city);
	}

	public GameController(Micropolis engine)
	{
		this.engine = engine;
		this.clock = new AnimationClock(engine, new AnimationClock.Listener() {
			public void onAnimated() {
				Runnable cb = frameCallback;
				if (cb != null) {
					cb.run();
				}
			}
		});
		engine.addListener(new EngineListener());
		engine.addMapListener(new FrameListener());
	}

	private void fireFrame()
	{
		Runnable cb = frameCallback;
		if (cb != null) {
			cb.run();
		}
	}

	public Micropolis getEngine() { return engine; }

	// --- engine thread lifecycle ---

	public void start() { clock.start(); }
	public void stop() { clock.stop(); }
	public void setSpeed(Speed speed) { clock.setSpeed(speed); }
	public Speed getSpeed() { return clock.getSpeed(); }
	public int animationCycle() { return clock.animationCycle(); }

	/** The renderer sets this to be called (on the engine thread) after each frame. */
	public void setFrameCallback(Runnable r) { this.frameCallback = r; }

	// --- tool selection ---

	public MicropolisTool getTool() { return tool; }
	public void setTool(MicropolisTool tool) { this.tool = tool; }

	/** Selecting the already-selected tool clears it (returns to pan mode). */
	public void toggleTool(MicropolisTool t)
	{
		this.tool = (this.tool == t) ? null : t;
	}

	// --- status ---

	public StatusSnapshot snapshot()
	{
		synchronized (engine) {
			int funds = engine.budget.totalFunds;
			MicropolisTool t = tool;
			return new StatusSnapshot(
				GameStrings.formatGameDate(engine.cityTime),
				funds,
				GameStrings.formatFunds(funds),
				engine.getCityPopulation(),
				t != null ? GameStrings.toolName(t) : "",
				t != null ? t.getToolCost() : 0);
		}
	}

	/** Next queued engine message text, or null if none. */
	public String pollMessage() { return messages.poll(); }

	/** Add a message to the ticker queue (e.g. a tool-result notice). */
	public void postMessage(String text)
	{
		if (text != null) {
			messages.add(text);
		}
	}

	// --- tools ---

	/** A preview of the current tool's stroke (read-only). Null if no tool. */
	public ToolPreview preview(int x0, int y0, int x1, int y1)
	{
		MicropolisTool t = tool;
		if (t == null) {
			return null;
		}
		synchronized (engine) {
			ToolStroke stroke = t.beginStroke(engine, x0, y0);
			stroke.dragTo(x1, y1);
			return stroke.getPreview();
		}
	}

	/** Apply the current tool's stroke on the engine thread; deliver the result. */
	public void apply(final int x0, final int y0, final int x1, final int y1,
		final ResultCallback cb)
	{
		final MicropolisTool t = tool;
		if (t == null) {
			if (cb != null) cb.onResult(ToolResult.NONE);
			return;
		}
		clock.post(new Runnable() {
			public void run() {
				ToolResult r = applyStroke(t, x0, y0, x1, y1);
				noteResult(r);
				if (cb != null) cb.onResult(r);
			}
		});
	}

	/**
	 * Apply a tool synchronously under the engine lock and return the result.
	 * Used by tests and simple callers; the UI normally uses {@link #apply}.
	 */
	public ToolResult applyNow(MicropolisTool t, int x0, int y0, int x1, int y1)
	{
		synchronized (engine) {
			return applyStroke(t, x0, y0, x1, y1);
		}
	}

	private ToolResult applyStroke(MicropolisTool t, int x0, int y0, int x1, int y1)
	{
		ToolStroke stroke = t.beginStroke(engine, x0, y0);
		stroke.dragTo(x1, y1);
		return stroke.apply();
	}

	private void noteResult(ToolResult r)
	{
		if (r == ToolResult.INSUFFICIENT_FUNDS) {
			messages.add(GameStrings.cityMessage(MicropolisMessage.INSUFFICIENT_FUNDS));
		} else if (r == ToolResult.UH_OH) {
			messages.add("Cannot build there.");
		}
	}

	/** Query the zone at a tile (read-only). */
	public ZoneStatus query(int x, int y)
	{
		synchronized (engine) {
			return engine.queryZoneStatus(x, y);
		}
	}

	/** A formatted zone query report for the tile (read-only). */
	public QueryReport queryReport(int x, int y)
	{
		return QueryReport.of(query(x, y));
	}

	private final class EngineListener implements Micropolis.Listener
	{
		public void cityMessage(MicropolisMessage message, CityLocation loc)
		{
			messages.add(GameStrings.cityMessage(message));
		}
		public void citySound(Sound sound, CityLocation loc) { }
		public void censusChanged() { }
		public void demandChanged() { }
		public void evaluationChanged() { }
		public void fundsChanged() { }
		public void optionsChanged() { }
	}

	/** Triggers a repaint (via the render callback) when the map changes. */
	private final class FrameListener implements MapListener
	{
		public void mapAnimation() { fireFrame(); }
		public void mapOverlayDataChanged(MapState overlayDataType) { }
		public void spriteMoved(Sprite sprite) { fireFrame(); }
		public void tileChanged(int xpos, int ypos) { fireFrame(); }
		public void wholeMapChanged() { fireFrame(); }
	}
}
