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

import java.util.ArrayList;
import java.util.List;
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
import micropolisj.engine.TileConstants;
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

	/**
	 * The two kinds of tool. STROKE tools (bulldozer, road, rail, wire, park and
	 * the three zones) stay selected until cleared, so you can keep drawing.
	 * ONE_SHOT tools (the services and the big buildings) return to Pan after one
	 * successful placement, so you do not accidentally drop a second R5000 stadium.
	 */
	public enum ToolKind { STROKE, ONE_SHOT }

	/** Which kind a tool is. QUERY is not placed here (it is a long press). */
	public static ToolKind kindOf(MicropolisTool t)
	{
		if (t == null) {
			return ToolKind.STROKE;
		}
		switch (t) {
		case POLICE:
		case FIRE:
		case STADIUM:
		case SEAPORT:
		case POWERPLANT:
		case NUCLEAR:
		case AIRPORT:
			return ToolKind.ONE_SHOT;
		default:
			return ToolKind.STROKE;
		}
	}

	public MicropolisTool getTool() { return tool; }
	public void setTool(MicropolisTool tool) { this.tool = tool; }

	/** Selecting the already-selected tool clears it (returns to pan mode). */
	public void toggleTool(MicropolisTool t)
	{
		this.tool = (this.tool == t) ? null : t;
	}

	/** After a successful placement, a one-shot tool returns to Pan. */
	private void maybeAutoClear(MicropolisTool t, ToolResult r)
	{
		if (r == ToolResult.SUCCESS && kindOf(t) == ToolKind.ONE_SHOT && this.tool == t) {
			this.tool = null;
		}
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
				maybeAutoClear(t, r);
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
			ToolResult r = applyStroke(t, x0, y0, x1, y1);
			maybeAutoClear(t, r);
			return r;
		}
	}

	private ToolResult applyStroke(MicropolisTool t, int x0, int y0, int x1, int y1)
	{
		ToolStroke stroke = t.beginStroke(engine, x0, y0);
		stroke.dragTo(x1, y1);
		return stroke.apply();
	}

	// --- path following (the finger's drag path, not just press->release) ---
	//
	// The engine's ToolStroke snaps a single stroke to one straight axis, so a bent
	// drag would end away from the finger and a later stroke could leave a gap. We
	// keep the finger's tile waypoints and, on release, lay the road along that path
	// as a chain of axis-aligned engine strokes. Each shared corner tile is a road
	// by the time the next segment runs, so the engine lays (and charges) each tile
	// once. The engine (ToolStroke) is not modified.

	/** A read-only preview of the whole path (merged from its segments). Null if no tool. */
	public ToolPreview previewPath(int[] xs, int[] ys)
	{
		MicropolisTool t = tool;
		if (t == null || xs == null || xs.length == 0) {
			return null;
		}
		synchronized (engine) {
			// ToolPreview has no public constructor, so we use the first segment's
			// engine-made preview as the base and merge the rest into it (setTile is
			// public). The first segment's origin is the path origin, so offsets line up.
			int ox = xs[0];
			int oy = ys[0];
			ToolPreview merged = null;
			int laid = 0;
			boolean anyUhOh = false;
			for (int[] seg : pathSegments(xs, ys)) {
				ToolStroke stroke = t.beginStroke(engine, seg[0], seg[1]);
				stroke.dragTo(seg[2], seg[3]);
				ToolPreview sp = stroke.getPreview();
				if (sp.toolResult == ToolResult.UH_OH) {
					anyUhOh = true;
				}
				if (merged == null) {
					merged = sp;
					laid += countTiles(sp);
				} else {
					laid += mergePreview(merged, sp, seg[0], seg[1], ox, oy);
				}
			}
			if (merged == null) {
				return null;
			}
			// Only flag the whole path bad if nothing at all could be laid.
			if (laid == 0 && anyUhOh) {
				merged.toolResult = ToolResult.UH_OH;
			}
			return merged;
		}
	}

	/** Apply the current tool along the path on the engine thread; deliver the result. */
	public void applyPath(final int[] xs, final int[] ys, final ResultCallback cb)
	{
		final MicropolisTool t = tool;
		if (t == null || xs == null || xs.length == 0) {
			if (cb != null) cb.onResult(ToolResult.NONE);
			return;
		}
		clock.post(new Runnable() {
			public void run() {
				ToolResult r = applyPathStroke(t, xs, ys);
				noteResult(r);
				maybeAutoClear(t, r);
				if (cb != null) cb.onResult(r);
			}
		});
	}

	/** Apply a tool along a path synchronously under the engine lock (tests/simple callers). */
	public ToolResult applyPathNow(MicropolisTool t, int[] xs, int[] ys)
	{
		synchronized (engine) {
			ToolResult r = applyPathStroke(t, xs, ys);
			maybeAutoClear(t, r);
			return r;
		}
	}

	private ToolResult applyPathStroke(MicropolisTool t, int[] xs, int[] ys)
	{
		ToolResult best = ToolResult.NONE;
		for (int[] seg : pathSegments(xs, ys)) {
			ToolStroke stroke = t.beginStroke(engine, seg[0], seg[1]);
			stroke.dragTo(seg[2], seg[3]);
			best = combineResult(best, stroke.apply());
		}
		return best;
	}

	/**
	 * Split a chain of tile waypoints into axis-aligned segments (each {sx,sy,ex,ey}).
	 * A diagonal step between two waypoints becomes a horizontal then a vertical leg
	 * (an L), so the laid road always follows the finger with no diagonal gaps.
	 */
	private static List<int[]> pathSegments(int[] xs, int[] ys)
	{
		List<int[]> segs = new ArrayList<int[]>();
		int n = Math.min(xs.length, ys.length);
		for (int i = 1; i < n; i++) {
			int x0 = xs[i - 1], y0 = ys[i - 1];
			int x1 = xs[i], y1 = ys[i];
			if (x0 == x1 && y0 == y1) {
				continue;
			}
			if (x0 == x1 || y0 == y1) {
				segs.add(new int[] { x0, y0, x1, y1 });
			} else {
				segs.add(new int[] { x0, y0, x1, y0 }); // horizontal leg
				segs.add(new int[] { x1, y0, x1, y1 }); // vertical leg
			}
		}
		if (segs.isEmpty()) {
			// A single waypoint (a tap): place the one tile at the origin.
			segs.add(new int[] { xs[0], ys[0], xs[0], ys[0] });
		}
		return segs;
	}

	/** Count the non-CLEAR tiles in a preview. */
	private static int countTiles(ToolPreview sp)
	{
		int n = 0;
		for (short[] row : sp.tiles) {
			for (short value : row) {
				if (value != TileConstants.CLEAR) {
					n++;
				}
			}
		}
		return n;
	}

	/** Copy a segment preview's tiles into the merged preview; returns tiles copied. */
	private static int mergePreview(ToolPreview merged, ToolPreview sp,
		int segX, int segY, int originX, int originY)
	{
		int copied = 0;
		for (int r = 0; r < sp.tiles.length; r++) {
			short[] row = sp.tiles[r];
			for (int c = 0; c < row.length; c++) {
				short value = row[c];
				if (value == TileConstants.CLEAR) {
					continue;
				}
				int mapX = segX + (c - sp.offsetX);
				int mapY = segY + (r - sp.offsetY);
				merged.setTile(mapX - originX, mapY - originY, value);
				copied++;
			}
		}
		return copied;
	}

	private static ToolResult combineResult(ToolResult a, ToolResult b)
	{
		if (a == ToolResult.SUCCESS || b == ToolResult.SUCCESS) {
			return ToolResult.SUCCESS;
		}
		return (a == ToolResult.NONE) ? b : a;
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
