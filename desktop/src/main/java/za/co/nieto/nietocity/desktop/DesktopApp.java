/*
 * NietoCity - JavaFX 8 desktop UI.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * A JavaFX Stage with a top status bar + message ticker, a left tool palette, and
 * a Canvas rendering the map through the shared GameController. The simulation
 * runs on the GameController's engine thread; redraws and UI updates happen on
 * the JavaFX thread via Platform.runLater / a Timeline.
 *
 * Controls: left-drag places or draws a stroke (with a translucent preview);
 * right-drag or Space+drag pans; the mouse wheel and arrow keys zoom/pan;
 * right-click queries a tile. Crisp pixels: per-tile nearest-neighbour scaling
 * (JavaFX 8's Canvas has no image-smoothing switch).
 */
package za.co.nieto.nietocity.desktop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.Speed;
import micropolisj.engine.TileConstants;
import micropolisj.engine.ToolPreview;
import micropolisj.engine.ToolResult;
import za.co.nieto.nietocity.game.CurrencyFormat;
import za.co.nieto.nietocity.game.GameController;
import za.co.nieto.nietocity.game.GameStrings;
import za.co.nieto.nietocity.game.QueryReport;
import za.co.nieto.nietocity.game.StatusSnapshot;
import za.co.nieto.nietocity.render.MapOverlay;
import za.co.nieto.nietocity.render.MiniMap;
import za.co.nieto.nietocity.render.PowerOverlay;
import za.co.nieto.nietocity.render.TileIndex;
import za.co.nieto.nietocity.render.Viewport;

public class DesktopApp extends Application
{
	private static final int LOMASK = TileConstants.LOMASK;
	private static final int CLEAR = TileConstants.CLEAR;
	private static final int TS = TileIndex.TILE_SIZE;
	private static final int DEFAULT_ZOOM = 3;
	private static final long MESSAGE_MS = 4000;

	private static final List<MicropolisTool> TOOLS = Arrays.asList(
		MicropolisTool.BULLDOZER, MicropolisTool.WIRE, MicropolisTool.PARK,
		MicropolisTool.ROADS, MicropolisTool.RAIL,
		MicropolisTool.RESIDENTIAL, MicropolisTool.COMMERCIAL, MicropolisTool.INDUSTRIAL,
		MicropolisTool.FIRE, MicropolisTool.QUERY, MicropolisTool.POLICE,
		MicropolisTool.POWERPLANT, MicropolisTool.NUCLEAR,
		MicropolisTool.STADIUM, MicropolisTool.SEAPORT, MicropolisTool.AIRPORT);

	private final TileIndex tileIndex = TileIndex.loadDefault();
	private final Map<Integer, Image> tileCache = new HashMap<Integer, Image>();
	private final Map<MicropolisTool, Button> toolButtons = new HashMap<MicropolisTool, Button>();
	private final Map<MicropolisTool, Image> plainIcons = new HashMap<MicropolisTool, Image>();
	private final Map<MicropolisTool, Image> hiIcons = new HashMap<MicropolisTool, Image>();

	private Image atlas;
	private int[] atlasPixels;
	private int atlasWidth;

	private GameController controller;
	private Viewport viewport;
	private Canvas canvas;
	private Stage stage;
	private BorderPane root;

	// Mini map (docked overview panel).
	private Canvas miniCanvas;
	private VBox miniPanel;
	private boolean miniVisible;
	private WritableImage miniImage;
	private int miniMapW, miniMapH;
	private MapOverlay overlay = MapOverlay.NONE;

	private Label dateLbl, fundsLbl, popLbl, toolLbl, costLbl, tickerLbl;
	private ImageView selIconView;
	private Button clearToolBtn, pauseBtn, speedBtn;
	private long tickerHideAt;

	// input state
	private double lastX, lastY, pressX, pressY;
	private boolean panning, strokeActive, spaceDown;
	private int strokeOriginX, strokeOriginY, strokeCurX, strokeCurY;
	// The mouse's tile waypoints, so a bent drag lays a road that follows the path
	// (GameController splits it into axis-aligned strokes on release).
	private final List<Integer> pathX = new ArrayList<Integer>();
	private final List<Integer> pathY = new ArrayList<Integer>();
	private ToolPreview preview;

	@Override
	public void start(Stage stage)
	{
		this.stage = stage;
		this.atlas = new Image(DesktopApp.class.getResourceAsStream("/16x16/tiles.png"));
		this.atlasWidth = (int) atlas.getWidth();
		int atlasHeight = (int) atlas.getHeight();
		this.atlasPixels = new int[atlasWidth * atlasHeight];
		atlas.getPixelReader().getPixels(0, 0, atlasWidth, atlasHeight,
			PixelFormat.getIntArgbInstance(), atlasPixels, 0, atlasWidth);

		this.controller = GameController.newGame();
		Micropolis city = controller.getEngine();

		final int startW = 1024;
		final int startH = 720;

		this.canvas = new Canvas(startW, startH);
		Pane canvasPane = new Pane(canvas);
		canvas.widthProperty().bind(canvasPane.widthProperty());
		canvas.heightProperty().bind(canvasPane.heightProperty());
		canvas.widthProperty().addListener((o, a, b) -> onResize());
		canvas.heightProperty().addListener((o, a, b) -> onResize());

		this.root = new BorderPane();
		root.setTop(buildTopBar());
		root.setLeft(buildPalette());
		root.setCenter(canvasPane);
		buildMiniPanel();

		Scene scene = new Scene(root, startW, startH, Color.BLACK);
		installInput(scene);

		this.viewport = new Viewport(city.getWidth(), city.getHeight(), startW, startH);
		viewport.setZoom(DEFAULT_ZOOM);
		viewport.centreOnTile(city.getWidth() / 2, city.getHeight() / 2);

		controller.setFrameCallback(() -> Platform.runLater(this::redraw));

		stage.setScene(scene);
		stage.setTitle("NietoCity");
		stage.setOnCloseRequest(e -> controller.stop());
		stage.show();

		controller.start();
		refreshPalette();
		refreshSpeed();

		Timeline ui = new Timeline(new KeyFrame(Duration.millis(250), e -> onUiTick()));
		ui.setCycleCount(Animation.INDEFINITE);
		ui.play();

		redraw();
	}

	@Override
	public void stop()
	{
		if (controller != null) {
			controller.stop();
		}
	}

	// --- UI construction ---

	private VBox buildTopBar()
	{
		dateLbl = statusLabel();
		fundsLbl = statusLabel();
		popLbl = statusLabel();
		toolLbl = statusLabel();
		costLbl = statusLabel();
		costLbl.setStyle("-fx-text-fill: #FFE080;");

		selIconView = new ImageView();
		// A large X (>=48px) that returns to Pan; only shown when a tool is selected.
		clearToolBtn = new Button("✕");
		clearToolBtn.setMinSize(48, 48);
		clearToolBtn.setFocusTraversable(false);
		clearToolBtn.setStyle("-fx-font-size: 18; -fx-text-fill: white; -fx-background-color: #444;");
		clearToolBtn.setOnAction(e -> {
			controller.setTool(null);
			refreshPalette();
			updateStatus();
		});
		clearToolBtn.setVisible(false);
		clearToolBtn.setManaged(false);

		// Speed control: pause/play toggle and a tap-cycle speed label.
		pauseBtn = new Button("❚❚");
		pauseBtn.setFocusTraversable(false);
		pauseBtn.setStyle("-fx-text-fill: white; -fx-background-color: #444;");
		pauseBtn.setOnAction(e -> { controller.togglePause(); refreshSpeed(); });
		speedBtn = new Button("Normal");
		speedBtn.setFocusTraversable(false);
		speedBtn.setStyle("-fx-text-fill: white; -fx-background-color: #444;");
		speedBtn.setOnAction(e -> { controller.cycleSpeed(); refreshSpeed(); });

		Button mapBtn = new Button("Map");
		mapBtn.setFocusTraversable(false);
		mapBtn.setStyle("-fx-text-fill: white; -fx-background-color: #444;");
		mapBtn.setOnAction(e -> toggleMiniMap());

		MenuButton overlayBtn = buildOverlayMenu();

		HBox status = new HBox(16, dateLbl, fundsLbl, popLbl, selIconView, toolLbl, costLbl,
			clearToolBtn, pauseBtn, speedBtn, mapBtn, overlayBtn);
		status.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
		status.setPadding(new Insets(4, 8, 4, 8));
		status.setStyle("-fx-background-color: #202020;");

		tickerLbl = new Label(" ");
		tickerLbl.setStyle("-fx-text-fill: white; -fx-background-color: #2277CC; -fx-padding: 2 8 2 8;");
		tickerLbl.setMaxWidth(Double.MAX_VALUE);
		tickerLbl.setVisible(false);

		return new VBox(status, tickerLbl);
	}

	private MenuButton buildOverlayMenu()
	{
		MenuButton mb = new MenuButton("Overlay");
		mb.setFocusTraversable(false);
		mb.setStyle("-fx-text-fill: white;");
		ToggleGroup group = new ToggleGroup();
		for (final MapOverlay ov : MapOverlay.values()) {
			RadioMenuItem item = new RadioMenuItem(ov.label());
			item.setToggleGroup(group);
			item.setSelected(ov == overlay);
			item.setOnAction(e -> selectOverlay(ov));
			mb.getItems().add(item);
		}
		return mb;
	}

	private void selectOverlay(MapOverlay ov)
	{
		overlay = ov;
		redraw();
		refreshMiniMap();
	}

	private Label statusLabel()
	{
		Label l = new Label();
		l.setStyle("-fx-text-fill: white;");
		return l;
	}

	private ScrollPane buildPalette()
	{
		VBox box = new VBox(2);
		box.setPadding(new Insets(4));
		box.setStyle("-fx-background-color: #101010;");
		for (final MicropolisTool tool : TOOLS) {
			plainIcons.put(tool, icon(tool.name() + ".png"));
			hiIcons.put(tool, icon(tool.name() + "_hi.png"));
			Button b = new Button();
			b.setGraphic(new ImageView(plainIcons.get(tool)));
			b.setStyle("-fx-background-color: transparent; -fx-padding: 1;");
			b.setFocusTraversable(false);
			b.setOnAction(e -> {
				controller.toggleTool(tool);
				refreshPalette();
				updateStatus();
			});
			toolButtons.put(tool, b);
			box.getChildren().add(b);
		}
		ScrollPane sp = new ScrollPane(box);
		sp.setFitToWidth(true);
		sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		sp.setStyle("-fx-background: #101010;");
		return sp;
	}

	// --- mini map (docked overview panel) ---

	private static final int MINI_W = 220;
	private static final int MINI_H = 190;

	private void buildMiniPanel()
	{
		miniCanvas = new Canvas(MINI_W, MINI_H);
		miniCanvas.setOnMousePressed(e -> recenterFromMini(e.getX(), e.getY()));
		miniCanvas.setOnMouseDragged(e -> recenterFromMini(e.getX(), e.getY()));

		Label title = new Label("Overview");
		title.setStyle("-fx-text-fill: white;");
		miniPanel = new VBox(4, title, miniCanvas);
		miniPanel.setPadding(new Insets(6));
		miniPanel.setStyle("-fx-background-color: #101010;");
	}

	private void toggleMiniMap()
	{
		miniVisible = !miniVisible;
		root.setRight(miniVisible ? miniPanel : null);
		if (miniVisible) {
			refreshMiniMap();
		}
	}

	/** Rebuild the overview image from the map and redraw the panel. */
	private void refreshMiniMap()
	{
		if (!miniVisible) {
			return;
		}
		Micropolis city = controller.getEngine();
		int[] px;
		synchronized (city) {
			miniMapW = city.getWidth();
			miniMapH = city.getHeight();
			px = MiniMap.overviewPixels(city, overlay);
		}
		if (miniMapW <= 0 || miniMapH <= 0) {
			return;
		}
		if (miniImage == null || (int) miniImage.getWidth() != miniMapW
			|| (int) miniImage.getHeight() != miniMapH) {
			miniImage = new WritableImage(miniMapW, miniMapH);
		}
		miniImage.getPixelWriter().setPixels(0, 0, miniMapW, miniMapH,
			PixelFormat.getIntArgbInstance(), px, 0, miniMapW);

		GraphicsContext gc = miniCanvas.getGraphicsContext2D();
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, MINI_W, MINI_H);

		double[] r = miniContentRect();
		double left = r[0], top = r[1], w = r[2], h = r[3];
		gc.drawImage(miniImage, left, top, w, h);

		// Viewport rectangle.
		double sx = w / miniMapW;
		double sy = h / miniMapH;
		gc.setStroke(Color.YELLOW);
		gc.setLineWidth(1.5);
		double vx = left + viewport.firstVisibleCol() * sx;
		double vy = top + viewport.firstVisibleRow() * sy;
		double vw = (viewport.lastVisibleCol() + 1 - viewport.firstVisibleCol()) * sx;
		double vh = (viewport.lastVisibleRow() + 1 - viewport.firstVisibleRow()) * sy;
		gc.strokeRect(vx, vy, vw, vh);
	}

	/** {left, top, width, height} of the aspect-correct overview inside the canvas. */
	private double[] miniContentRect()
	{
		double scale = Math.min((double) MINI_W / miniMapW, (double) MINI_H / miniMapH);
		double w = miniMapW * scale;
		double h = miniMapH * scale;
		return new double[] { (MINI_W - w) / 2, (MINI_H - h) / 2, w, h };
	}

	private void recenterFromMini(double px, double py)
	{
		if (miniMapW <= 0) {
			return;
		}
		double[] r = miniContentRect();
		int tileX = MiniMap.tileXForOverview((int) (px - r[0]), (int) r[2], miniMapW);
		int tileY = MiniMap.tileYForOverview((int) (py - r[1]), (int) r[3], miniMapH);
		viewport.centreOnTile(tileX, tileY);
		redraw();
		refreshMiniMap();
	}

	private Image icon(String fileName)
	{
		Image base = new Image(DesktopApp.class.getResourceAsStream("/tools/" + fileName));
		return scale3x(base);
	}

	/** 3x nearest-neighbour scale so the small tool icons are visible and crisp. */
	private static Image scale3x(Image base)
	{
		int w = (int) base.getWidth();
		int h = (int) base.getHeight();
		int[] pix = new int[w * h];
		base.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pix, 0, w);
		int zw = w * 3;
		int zh = h * 3;
		int[] out = new int[zw * zh];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = pix[y * w + x];
				int baseIdx = (y * 3) * zw + x * 3;
				for (int dy = 0; dy < 3; dy++) {
					int rowIdx = baseIdx + dy * zw;
					for (int dx = 0; dx < 3; dx++) {
						out[rowIdx + dx] = p;
					}
				}
			}
		}
		WritableImage wi = new WritableImage(zw, zh);
		wi.getPixelWriter().setPixels(0, 0, zw, zh, PixelFormat.getIntArgbInstance(), out, 0, zw);
		return wi;
	}

	private void refreshPalette()
	{
		MicropolisTool sel = controller.getTool();
		for (MicropolisTool tool : TOOLS) {
			Button b = toolButtons.get(tool);
			((ImageView) b.getGraphic()).setImage(tool == sel ? hiIcons.get(tool) : plainIcons.get(tool));
		}
	}

	// --- input ---

	private void installInput(Scene scene)
	{
		canvas.setOnMousePressed(e -> {
			lastX = e.getX();
			lastY = e.getY();
			pressX = e.getX();
			pressY = e.getY();
			MicropolisTool tool = controller.getTool();
			if (e.getButton() == MouseButton.SECONDARY) {
				panning = true;
			} else if (e.getButton() == MouseButton.PRIMARY) {
				if (tool != null) {
					beginStroke(e.getX(), e.getY());
				} else {
					panning = true;
				}
			}
		});

		canvas.setOnMouseDragged(e -> {
			if (panning) {
				viewport.panBy((int) Math.round(lastX - e.getX()), (int) Math.round(lastY - e.getY()));
				lastX = e.getX();
				lastY = e.getY();
				redraw();
			} else if (strokeActive) {
				extendStroke(e.getX(), e.getY());
			}
		});

		canvas.setOnMouseReleased(e -> {
			if (strokeActive) {
				applyStroke();
			} else if (e.getButton() == MouseButton.SECONDARY
				&& Math.abs(e.getX() - pressX) < 4 && Math.abs(e.getY() - pressY) < 4) {
				queryAt(e.getX(), e.getY());
			}
			cancelStroke();
			panning = false;
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
			KeyCode c = e.getCode();
			// Space toggles pause (guard the key-repeat while held).
			if (c == KeyCode.SPACE) {
				if (!spaceDown) {
					spaceDown = true;
					controller.togglePause();
					refreshSpeed();
				}
				return;
			}
			// 1..4 set the run speed (SLOW/NORMAL/FAST/SUPER_FAST) and resume.
			Speed picked = speedForKey(c);
			if (picked != null) {
				controller.setChosenSpeed(picked);
				controller.setPaused(false);
				refreshSpeed();
				return;
			}
			int step = TS * viewport.getZoom();
			if (c == KeyCode.LEFT) viewport.panBy(-step, 0);
			else if (c == KeyCode.RIGHT) viewport.panBy(step, 0);
			else if (c == KeyCode.UP) viewport.panBy(0, -step);
			else if (c == KeyCode.DOWN) viewport.panBy(0, step);
			else return;
			redraw();
		});
		scene.setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.SPACE) spaceDown = false;
		});
	}

	private static Speed speedForKey(KeyCode c)
	{
		if (c == KeyCode.DIGIT1 || c == KeyCode.NUMPAD1) return Speed.SLOW;
		if (c == KeyCode.DIGIT2 || c == KeyCode.NUMPAD2) return Speed.NORMAL;
		if (c == KeyCode.DIGIT3 || c == KeyCode.NUMPAD3) return Speed.FAST;
		if (c == KeyCode.DIGIT4 || c == KeyCode.NUMPAD4) return Speed.SUPER_FAST;
		return null;
	}

	/** Reflect the controller's pause state and chosen speed on the toolbar. */
	private void refreshSpeed()
	{
		pauseBtn.setText(controller.isPaused() ? "▶" : "❚❚");
		speedBtn.setText(GameStrings.speedName(controller.getChosenSpeed()));
	}

	private void beginStroke(double px, double py)
	{
		strokeOriginX = viewport.tileXAt((int) px);
		strokeOriginY = viewport.tileYAt((int) py);
		strokeCurX = strokeOriginX;
		strokeCurY = strokeOriginY;
		pathX.clear();
		pathY.clear();
		pathX.add(strokeOriginX);
		pathY.add(strokeOriginY);
		strokeActive = true;
		updatePreview();
	}

	private void extendStroke(double px, double py)
	{
		int tx = viewport.tileXAt((int) px);
		int ty = viewport.tileYAt((int) py);
		if (tx != strokeCurX || ty != strokeCurY) {
			strokeCurX = tx;
			strokeCurY = ty;
			pathX.add(tx);
			pathY.add(ty);
			updatePreview();
		}
	}

	private int[] pathXs() { return toIntArray(pathX); }
	private int[] pathYs() { return toIntArray(pathY); }

	private static int[] toIntArray(List<Integer> list)
	{
		int[] a = new int[list.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = list.get(i);
		}
		return a;
	}

	private void updatePreview()
	{
		preview = controller.previewPath(pathXs(), pathYs());
		redraw();
	}

	private void applyStroke()
	{
		// onResult runs on the engine thread; a one-shot tool may have returned to
		// Pan, so refresh the palette and status on the JavaFX thread.
		controller.applyPath(pathXs(), pathYs(), r -> Platform.runLater(() -> {
			refreshPalette();
			updateStatus();
		}));
	}

	private void cancelStroke()
	{
		strokeActive = false;
		preview = null;
		pathX.clear();
		pathY.clear();
		redraw();
	}

	private void queryAt(double px, double py)
	{
		QueryReport r = controller.queryReport(viewport.tileXAt((int) px), viewport.tileYAt((int) py));
		StringBuilder body = new StringBuilder();
		for (int i = 0; i < r.labels.length; i++) {
			body.append(r.labels[i]).append(' ').append(r.values[i]);
			if (i < r.labels.length - 1) body.append('\n');
		}
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.initOwner(stage);
		a.setTitle(r.header);
		a.setHeaderText(r.header);
		a.setContentText(body.toString());
		a.show();
	}

	// --- periodic UI update ---

	private long lastStatusAt;

	private void onUiTick()
	{
		long now = System.currentTimeMillis();
		if (now - lastStatusAt >= 1000) {
			updateStatus();
			lastStatusAt = now;
		}
		if (miniVisible) {
			refreshMiniMap();
		}
		String latest = null;
		String m = controller.pollMessage();
		while (m != null) {
			latest = m;
			m = controller.pollMessage();
		}
		if (latest != null) {
			tickerLbl.setText(latest);
			tickerLbl.setVisible(true);
			tickerHideAt = now + MESSAGE_MS;
		} else if (tickerLbl.isVisible() && now >= tickerHideAt) {
			tickerLbl.setVisible(false);
		}
	}

	private void updateStatus()
	{
		StatusSnapshot s = controller.snapshot();
		dateLbl.setText(s.date);
		fundsLbl.setText(s.fundsText);
		popLbl.setText("Pop " + s.population);

		MicropolisTool tool = controller.getTool();
		if (tool == null) {
			toolLbl.setText("Pan");
			costLbl.setText("");
			selIconView.setImage(null);
			clearToolBtn.setVisible(false);
			clearToolBtn.setManaged(false);
		} else {
			toolLbl.setText(s.toolName);
			costLbl.setText(s.toolCost != 0 ? CurrencyFormat.format(s.toolCost) : "");
			selIconView.setImage(plainIcons.get(tool));
			clearToolBtn.setVisible(true);
			clearToolBtn.setManaged(true);
		}
		stage.setTitle("NietoCity - " + s.date);
	}

	private void onResize()
	{
		viewport.setViewSize((int) canvas.getWidth(), (int) canvas.getHeight());
		redraw();
	}

	// --- rendering ---

	private int[] snapshot = new int[0];
	private boolean[] boltSnapshot = new boolean[0];
	private int[] overlaySnapshot = new int[0];
	private static final double OVERLAY_ALPHA = 0xB0 / 255.0;

	private void redraw()
	{
		GraphicsContext gc = canvas.getGraphicsContext2D();
		double cw = canvas.getWidth();
		double ch = canvas.getHeight();
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, cw, ch);

		Micropolis city = controller.getEngine();
		int firstCol, lastCol, firstRow, lastRow, cycle;
		synchronized (city) {
			firstCol = viewport.firstVisibleCol();
			lastCol = viewport.lastVisibleCol();
			firstRow = viewport.firstVisibleRow();
			lastRow = viewport.lastVisibleRow();
			cycle = controller.animationCycle();

			int cols = lastCol - firstCol + 1;
			int rows = lastRow - firstRow + 1;
			if (snapshot.length < cols * rows) {
				snapshot = new int[cols * rows];
				boltSnapshot = new boolean[cols * rows];
				overlaySnapshot = new int[cols * rows];
			}
			boolean overlayOn = overlay != MapOverlay.NONE;
			int i = 0;
			for (int row = firstRow; row <= lastRow; row++) {
				for (int col = firstCol; col <= lastCol; col++) {
					int tile = city.getTile(col, row) & LOMASK;
					snapshot[i] = tile;
					// Blink a lightning bolt over unpowered zone centres (shared core).
					boltSnapshot[i] = PowerOverlay.showBolt(city, col, row, cycle);
					overlaySnapshot[i] = overlayOn ? overlay.colorAt(city, col, row, tile) : 0;
					i++;
				}
			}
		}

		int zoom = viewport.getZoom();
		int tp = viewport.tilePx();
		boolean boltImage = tileIndex.hasImage(PowerOverlay.LIGHTNINGBOLT);
		int i = 0;
		for (int row = firstRow; row <= lastRow; row++) {
			int screenY = viewport.tileScreenY(row);
			for (int col = firstCol; col <= lastCol; col++) {
				int tile = snapshot[i];
				boolean bolt = boltSnapshot[i];
				int ovColor = overlaySnapshot[i];
				i++;
				int screenX = viewport.tileScreenX(col);
				if (tileIndex.hasImage(tile)) {
					int yOff = tileIndex.frameOffsetY(tile, cycle);
					gc.drawImage(tileImage(yOff, zoom), screenX, screenY);
				}
				if (bolt && boltImage) {
					int yOff = tileIndex.frameOffsetY(PowerOverlay.LIGHTNINGBOLT, cycle);
					gc.drawImage(tileImage(yOff, zoom), screenX, screenY);
				}
				if (ovColor != 0) {
					gc.setFill(Color.rgb((ovColor >> 16) & 0xFF, (ovColor >> 8) & 0xFF,
						ovColor & 0xFF, OVERLAY_ALPHA));
					gc.fillRect(screenX, screenY, tp, tp);
				}
			}
		}

		drawPreview(gc, zoom, cycle, firstCol, lastCol, firstRow, lastRow);
	}

	private void drawPreview(GraphicsContext gc, int zoom, int cycle,
		int firstCol, int lastCol, int firstRow, int lastRow)
	{
		ToolPreview pv = preview;
		if (pv == null) {
			return;
		}
		int tp = viewport.tilePx();
		boolean bad = pv.toolResult == ToolResult.UH_OH || pv.toolResult == ToolResult.INSUFFICIENT_FUNDS;
		Color tint = bad ? Color.rgb(220, 0, 0, 0.35) : Color.rgb(0, 200, 0, 0.28);
		short[][] tiles = pv.tiles;
		for (int ry = 0; ry < tiles.length; ry++) {
			for (int rx = 0; rx < tiles[ry].length; rx++) {
				int cValue = tiles[ry][rx];
				if (cValue == CLEAR) continue;
				int mapX = strokeOriginX + rx - pv.offsetX;
				int mapY = strokeOriginY + ry - pv.offsetY;
				if (mapX < firstCol || mapX > lastCol || mapY < firstRow || mapY > lastRow) continue;
				int sx = viewport.tileScreenX(mapX);
				int sy = viewport.tileScreenY(mapY);
				int masked = cValue & LOMASK;
				if (tileIndex.hasImage(masked)) {
					int yOff = tileIndex.frameOffsetY(masked, cycle);
					gc.setGlobalAlpha(0.7);
					gc.drawImage(tileImage(yOff, zoom), sx, sy);
					gc.setGlobalAlpha(1.0);
				}
				gc.setFill(tint);
				gc.fillRect(sx, sy, tp, tp);
			}
		}
	}

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
