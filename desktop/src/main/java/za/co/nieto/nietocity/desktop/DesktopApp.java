/*
 * NietoCity - JavaFX 8 desktop renderer.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * A JavaFX Stage with a Canvas that renders the Micropolis map using the shared
 * render core and the same 16x16 atlas as the Android app. The simulation runs
 * on its own thread (AnimationClock); redraws happen on the JavaFX thread via
 * Platform.runLater.
 *
 * Crisp pixels: JavaFX 8's GraphicsContext has no image-smoothing switch (that
 * arrived in JavaFX 12), so we pre-scale each tile nearest-neighbour and blit it
 * 1:1 - the equivalent of setSmooth(false). We scale per tile (not the whole
 * atlas) because the atlas strip is far taller than Prism's maximum texture
 * size, and drawing it whole throws inside the JavaFX render thread.
 */
package za.co.nieto.nietocity.desktop;

import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import micropolisj.engine.MapGenerator;
import micropolisj.engine.Micropolis;
import micropolisj.engine.Speed;
import micropolisj.engine.TileConstants;
import za.co.nieto.nietocity.render.AnimationClock;
import za.co.nieto.nietocity.render.TileIndex;
import za.co.nieto.nietocity.render.Viewport;

public class DesktopApp extends Application
{
	private static final int LOMASK = TileConstants.LOMASK;
	private static final int TS = TileIndex.TILE_SIZE;
	private static final int DEFAULT_ZOOM = 3;

	private final TileIndex tileIndex = TileIndex.loadDefault();
	// Cache of pre-scaled per-tile images, keyed by (atlas y offset, zoom). Small
	// images only - the full atlas strip is too tall to be a Prism texture.
	private final Map<Integer, Image> tileCache = new HashMap<Integer, Image>();

	private Image atlas;
	private int[] atlasPixels;   // whole atlas read to the CPU once (not a texture)
	private int atlasWidth;
	private Micropolis city;
	private Viewport viewport;
	private AnimationClock clock;
	private Canvas canvas;
	private Stage stage;

	private double lastMouseX;
	private double lastMouseY;
	private int[] snapshot = new int[0];

	@Override
	public void start(Stage stage)
	{
		this.stage = stage;
		this.atlas = new Image(DesktopApp.class.getResourceAsStream("/16x16/tiles.png"));
		this.atlasWidth = (int) atlas.getWidth();
		int atlasHeight = (int) atlas.getHeight();
		this.atlasPixels = new int[atlasWidth * atlasHeight];
		atlas.getPixelReader().getPixels(
			0, 0, atlasWidth, atlasHeight,
			PixelFormat.getIntArgbInstance(), atlasPixels, 0, atlasWidth);

		this.city = new Micropolis();
		new MapGenerator(city).generateNewCity();

		final int startW = 960;
		final int startH = 720;

		this.canvas = new Canvas(startW, startH);
		Pane root = new Pane(canvas);
		Scene scene = new Scene(root, startW, startH, Color.BLACK);

		this.viewport = new Viewport(city.getWidth(), city.getHeight(), startW, startH);
		viewport.setZoom(DEFAULT_ZOOM);
		viewport.centreOnTile(city.getWidth() / 2, city.getHeight() / 2);

		canvas.widthProperty().bind(scene.widthProperty());
		canvas.heightProperty().bind(scene.heightProperty());
		canvas.widthProperty().addListener((o, a, b) -> onResize(scene));
		canvas.heightProperty().addListener((o, a, b) -> onResize(scene));

		canvas.setOnMousePressed(e -> { lastMouseX = e.getX(); lastMouseY = e.getY(); });
		canvas.setOnMouseDragged(e -> {
			int dx = (int) Math.round(lastMouseX - e.getX());
			int dy = (int) Math.round(lastMouseY - e.getY());
			lastMouseX = e.getX();
			lastMouseY = e.getY();
			viewport.panBy(dx, dy);
			redraw();
		});

		scene.setOnScroll(e -> {
			if (e.getDeltaY() > 0) {
				viewport.setZoom(viewport.getZoom() + 1);
			} else if (e.getDeltaY() < 0) {
				viewport.setZoom(viewport.getZoom() - 1);
			}
			redraw();
		});

		scene.setOnKeyPressed(e -> {
			int step = TS * viewport.getZoom();
			KeyCode c = e.getCode();
			if (c == KeyCode.LEFT) viewport.panBy(-step, 0);
			else if (c == KeyCode.RIGHT) viewport.panBy(step, 0);
			else if (c == KeyCode.UP) viewport.panBy(0, -step);
			else if (c == KeyCode.DOWN) viewport.panBy(0, step);
			else return;
			redraw();
		});

		this.clock = new AnimationClock(city, () -> Platform.runLater(this::redraw));
		clock.setSpeed(Speed.NORMAL);

		stage.setScene(scene);
		stage.setTitle("NietoCity Phase 2");
		stage.setOnCloseRequest(e -> clock.stop());
		stage.show();

		clock.start();
		redraw();
	}

	@Override
	public void stop()
	{
		if (clock != null) {
			clock.stop();
		}
	}

	private void onResize(Scene scene)
	{
		viewport.setViewSize((int) canvas.getWidth(), (int) canvas.getHeight());
		redraw();
	}

	private void redraw()
	{
		GraphicsContext gc = canvas.getGraphicsContext2D();
		double cw = canvas.getWidth();
		double ch = canvas.getHeight();
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, cw, ch);

		int firstCol, lastCol, firstRow, lastRow, cycle, pop, funds;
		synchronized (city) {
			firstCol = viewport.firstVisibleCol();
			lastCol = viewport.lastVisibleCol();
			firstRow = viewport.firstVisibleRow();
			lastRow = viewport.lastVisibleRow();
			cycle = city.getAnimationCycle();
			pop = city.getCityPopulation();
			funds = city.budget.totalFunds;

			int cols = lastCol - firstCol + 1;
			int rows = lastRow - firstRow + 1;
			if (snapshot.length < cols * rows) {
				snapshot = new int[cols * rows];
			}
			int i = 0;
			for (int row = firstRow; row <= lastRow; row++) {
				for (int col = firstCol; col <= lastCol; col++) {
					snapshot[i++] = city.getTile(col, row) & LOMASK;
				}
			}
		}

		int zoom = viewport.getZoom();
		int tp = viewport.tilePx();
		int i = 0;
		for (int row = firstRow; row <= lastRow; row++) {
			int screenY = viewport.tileScreenY(row);
			for (int col = firstCol; col <= lastCol; col++) {
				int tile = snapshot[i++];
				if (!tileIndex.hasImage(tile)) continue;
				int yOff = tileIndex.frameOffsetY(tile, cycle);
				int screenX = viewport.tileScreenX(col);
				// 1:1 blit of a small pre-scaled tile image keeps pixels crisp.
				gc.drawImage(tileImage(yOff, zoom), screenX, screenY);
			}
		}

		final int fpop = pop;
		final int ffunds = funds;
		stage.setTitle("NietoCity Phase 2: pop " + fpop + ", funds " + ffunds);
	}

	/** A single tile, nearest-neighbour scaled to (TS*zoom) square, cached. */
	private Image tileImage(int yOff, int zoom)
	{
		int key = yOff * (Viewport.MAX_ZOOM + 1) + zoom;
		Image img = tileCache.get(key);
		if (img != null) {
			return img;
		}
		int size = TS * zoom;
		int[] out = new int[size * size];
		for (int y = 0; y < TS; y++) {
			for (int x = 0; x < TS; x++) {
				int p = atlasPixels[(yOff + y) * atlasWidth + x];
				int baseIdx = (y * zoom) * size + x * zoom;
				for (int dy = 0; dy < zoom; dy++) {
					int rowIdx = baseIdx + dy * size;
					for (int dx = 0; dx < zoom; dx++) {
						out[rowIdx + dx] = p;
					}
				}
			}
		}
		WritableImage wi = new WritableImage(size, size);
		wi.getPixelWriter().setPixels(0, 0, size, size, PixelFormat.getIntArgbInstance(), out, 0, size);
		tileCache.put(key, wi);
		return wi;
	}
}
