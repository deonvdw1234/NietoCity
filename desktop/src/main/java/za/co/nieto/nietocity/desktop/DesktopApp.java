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
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.prefs.Preferences;

import micropolisj.engine.BudgetNumbers;
import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.Speed;
import micropolisj.engine.SpriteKind;
import micropolisj.engine.TileConstants;
import micropolisj.engine.ToolPreview;
import micropolisj.engine.ToolResult;
import za.co.nieto.nietocity.game.BudgetControl;
import za.co.nieto.nietocity.game.CurrencyFormat;
import za.co.nieto.nietocity.game.EvaluationReport;
import za.co.nieto.nietocity.game.GameController;
import za.co.nieto.nietocity.game.GameStrings;
import za.co.nieto.nietocity.game.GraphData;
import za.co.nieto.nietocity.game.QueryReport;
import za.co.nieto.nietocity.game.TerrainConfig;
import za.co.nieto.nietocity.game.StatusSnapshot;
import za.co.nieto.nietocity.render.MapOverlay;
import za.co.nieto.nietocity.render.MiniMap;
import za.co.nieto.nietocity.render.PowerOverlay;
import za.co.nieto.nietocity.render.SpriteImages;
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
	// Sprite base pixels + dimensions per (objectId*100+frame), and a nearest-
	// neighbour scaled-image cache keyed by that key and the zoom.
	private final Map<Integer, int[]> spritePixels = new HashMap<Integer, int[]>();
	private final Map<Integer, int[]> spriteDims = new HashMap<Integer, int[]>();
	private final Map<Long, Image> spriteCache = new HashMap<Long, Image>();
	private java.util.List<SpriteImages.Frame> spriteFrames = java.util.Collections.emptyList();
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

	// Budget auto-show once a year (persisted preference).
	private final Preferences prefs = Preferences.userNodeForPackage(DesktopApp.class);
	private boolean autoShowBudget = true;
	private int lastBudgetYear;
	private boolean budgetOpen;

	private DesktopSoundPlayer soundPlayer;

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

		loadSprites();

		this.controller = GameController.newGame();
		Micropolis city = controller.getEngine();

		// Sound: default on, best-effort. Created before the menu so its Mute item
		// reflects the persisted state.
		this.soundPlayer = new DesktopSoundPlayer();
		this.soundPlayer.setMuted(prefs.getBoolean("muted", false));
		controller.setSoundPlayer(soundPlayer);

		// Random disasters: default on, persisted, applied to the engine's flag.
		controller.setRandomDisastersEnabled(prefs.getBoolean("randomDisasters", true));

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
		autoShowBudget = prefs.getBoolean("autoBudget", true);
		lastBudgetYear = currentYear();

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
		if (soundPlayer != null) {
			soundPlayer.release();
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

		MenuButton menuBtn = buildMenu();

		HBox status = new HBox(16, dateLbl, fundsLbl, popLbl, selIconView, toolLbl, costLbl,
			clearToolBtn, pauseBtn, speedBtn, menuBtn);
		status.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
		status.setPadding(new Insets(4, 8, 4, 8));
		status.setStyle("-fx-background-color: #202020;");

		tickerLbl = new Label(" ");
		tickerLbl.setStyle("-fx-text-fill: white; -fx-background-color: #2277CC; -fx-padding: 2 8 2 8;");
		tickerLbl.setMaxWidth(Double.MAX_VALUE);
		tickerLbl.setVisible(false);

		return new VBox(status, tickerLbl);
	}

	/**
	 * The overflow menu: the classic dialogs, the mini map toggle and the overlay
	 * picker, so the status bar stays uncluttered.
	 */
	private MenuButton buildMenu()
	{
		MenuButton mb = new MenuButton("Menu");
		mb.setFocusTraversable(false);
		mb.setStyle("-fx-text-fill: white;");

		MenuItem newCity = new MenuItem("New City…");
		newCity.setOnAction(e -> showNewCityDialog());
		MenuItem budget = new MenuItem("Budget…");
		budget.setOnAction(e -> showBudgetDialog());
		MenuItem eval = new MenuItem("Evaluation…");
		eval.setOnAction(e -> showEvaluationDialog());
		MenuItem graphs = new MenuItem("Graphs…");
		graphs.setOnAction(e -> showGraphsDialog());
		MenuItem miniToggle = new MenuItem("Mini map");
		miniToggle.setOnAction(e -> toggleMiniMap());

		CheckMenuItem mute = new CheckMenuItem("Mute sound");
		mute.setSelected(soundPlayer != null && soundPlayer.isMuted());
		mute.setOnAction(e -> {
			if (soundPlayer != null) {
				soundPlayer.setMuted(mute.isSelected());
				prefs.putBoolean("muted", mute.isSelected());
			}
		});

		CheckMenuItem randomDisasters = new CheckMenuItem("Random disasters");
		randomDisasters.setSelected(controller.isRandomDisastersEnabled());
		randomDisasters.setOnAction(e -> {
			controller.setRandomDisastersEnabled(randomDisasters.isSelected());
			prefs.putBoolean("randomDisasters", randomDisasters.isSelected());
		});

		Menu overlayMenu = new Menu("Overlay");
		ToggleGroup group = new ToggleGroup();
		for (final MapOverlay ov : MapOverlay.values()) {
			RadioMenuItem item = new RadioMenuItem(ov.label());
			item.setToggleGroup(group);
			item.setSelected(ov == overlay);
			item.setOnAction(e -> selectOverlay(ov));
			overlayMenu.getItems().add(item);
		}

		Menu disasters = buildDisastersMenu();

		mb.getItems().addAll(newCity, new SeparatorMenuItem(),
			budget, eval, graphs, disasters, new SeparatorMenuItem(),
			miniToggle, overlayMenu, randomDisasters, mute);
		return mb;
	}

	private void selectOverlay(MapOverlay ov)
	{
		overlay = ov;
		redraw();
		refreshMiniMap();
	}

	private static final String[] LEVEL_OPTIONS = { "Auto", "None", "Low", "High" };

	/**
	 * The New City screen: difficulty, terrain controls, a seed field (shows the
	 * current seed; type one to reproduce) and Reroll, with a live preview. A
	 * two-column form. Start begins the new city (task 4 adds the discard guard).
	 */
	private void showNewCityDialog()
	{
		TerrainConfig cfg0 = controller.getTerrainConfig();

		ComboBox<String> difficulty = combo(new String[] { "Easy", "Medium", "Hard" },
			controller.getGameLevel());
		ComboBox<String> island = combo(new String[] { "None", "Seldom", "Always" },
			cfg0.island.ordinal());
		ComboBox<String> lake = combo(LEVEL_OPTIONS, cfg0.lake.ordinal());
		ComboBox<String> river = combo(LEVEL_OPTIONS, cfg0.river.ordinal());
		ComboBox<String> trees = combo(LEVEL_OPTIONS, cfg0.trees.ordinal());

		TextField seedField = new TextField(Long.toString(controller.getSeed()));
		Button reroll = new Button("Reroll");
		reroll.setFocusTraversable(false);

		Canvas previewCanvas = new Canvas(200, 168);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(8);
		grid.setPadding(new Insets(12));
		grid.addRow(0, new Label("Difficulty"), difficulty);
		grid.addRow(1, new Label("Island"), island);
		grid.addRow(2, new Label("Lake"), lake);
		grid.addRow(3, new Label("River"), river);
		grid.addRow(4, new Label("Trees"), trees);
		HBox seedRow = new HBox(6, seedField, reroll);
		grid.addRow(5, new Label("Seed"), seedRow);

		VBox previewBox = new VBox(4, new Label("Preview"), previewCanvas);
		previewBox.setPadding(new Insets(12));

		HBox content = new HBox(8, grid, previewBox);

		// Generate previews off the FX thread; a counter drops stale results.
		final java.util.concurrent.ExecutorService exec =
			java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "nieto-preview");
				t.setDaemon(true);
				return t;
			});
		final int[] genId = { 0 };
		Runnable refresh = () -> {
			final int id = ++genId[0];
			final int level = levelIndex(difficulty);
			final long seed = readSeed(seedField);
			final TerrainConfig cfg = readConfig(island, lake, river, trees);
			exec.submit(() -> {
				Micropolis city = GameController.buildCity(level, seed, cfg);
				final int w = city.getWidth();
				final int h = city.getHeight();
				final int[] px = MiniMap.overviewPixels(city);
				Platform.runLater(() -> {
					if (id == genId[0]) {
						paintPreview(previewCanvas, px, w, h);
					}
				});
			});
		};
		difficulty.setOnAction(e -> refresh.run());
		island.setOnAction(e -> refresh.run());
		lake.setOnAction(e -> refresh.run());
		river.setOnAction(e -> refresh.run());
		trees.setOnAction(e -> refresh.run());
		seedField.textProperty().addListener((o, a, b) -> refresh.run());
		reroll.setOnAction(e -> seedField.setText(Long.toString(new java.util.Random().nextLong())));
		refresh.run();

		Button startBtn = new Button("Start");
		Button cancelBtn = new Button("Cancel");
		HBox buttons = new HBox(8, startBtn, cancelBtn);
		buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
		buttons.setPadding(new Insets(0, 12, 12, 12));

		VBox outer = new VBox(4, content, buttons);

		Stage dlg = new Stage();
		dlg.initOwner(stage);
		dlg.initModality(Modality.WINDOW_MODAL);
		dlg.setTitle("New City");
		dlg.setScene(new Scene(outer));

		startBtn.setOnAction(e -> {
			int level = levelIndex(difficulty);
			long seed = readSeed(seedField);
			TerrainConfig cfg = readConfig(island, lake, river, trees);
			dlg.close();
			startNewCity(level, seed, cfg);
		});
		cancelBtn.setOnAction(e -> dlg.close());
		dlg.setOnHidden(e -> exec.shutdownNow());
		dlg.show();
	}

	private static ComboBox<String> combo(String[] options, int selected)
	{
		ComboBox<String> cb = new ComboBox<String>();
		cb.getItems().addAll(options);
		cb.getSelectionModel().select(Math.max(0, Math.min(selected, options.length - 1)));
		cb.setFocusTraversable(false);
		return cb;
	}

	private static int levelIndex(ComboBox<String> difficulty)
	{
		return difficulty.getSelectionModel().getSelectedIndex();
	}

	private static long readSeed(TextField f)
	{
		try {
			return Long.parseLong(f.getText().trim());
		} catch (NumberFormatException e) {
			return new java.util.Random().nextLong();
		}
	}

	private static TerrainConfig readConfig(ComboBox<String> island, ComboBox<String> lake,
		ComboBox<String> river, ComboBox<String> trees)
	{
		return new TerrainConfig(
			TerrainConfig.Island.values()[island.getSelectionModel().getSelectedIndex()],
			TerrainConfig.Level.values()[lake.getSelectionModel().getSelectedIndex()],
			TerrainConfig.Level.values()[river.getSelectionModel().getSelectedIndex()],
			TerrainConfig.Level.values()[trees.getSelectionModel().getSelectedIndex()]);
	}

	/** Paint an overview pixel buffer into the preview canvas (on the FX thread). */
	private void paintPreview(Canvas c, int[] px, int w, int h)
	{
		WritableImage img = new WritableImage(w, h);
		img.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);

		GraphicsContext gc = c.getGraphicsContext2D();
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, c.getWidth(), c.getHeight());
		double scale = Math.min(c.getWidth() / w, c.getHeight() / h);
		double dw = w * scale, dh = h * scale;
		double left = (c.getWidth() - dw) / 2, top = (c.getHeight() - dh) / 2;
		gc.drawImage(img, left, top, dw, dh);
		gc.setStroke(Color.rgb(255, 255, 255, 0.8));
		gc.strokeRect(left, top, dw, dh);
	}

	/** Replace the running city with a freshly generated one (task 4 adds the guard). */
	private void startNewCity(int level, long seed, TerrainConfig cfg)
	{
		GameController fresh = GameController.newGame(level, seed, cfg);
		fresh.setChosenSpeed(controller.getChosenSpeed());
		fresh.setPaused(controller.isPaused());
		fresh.setRandomDisastersEnabled(controller.isRandomDisastersEnabled());
		fresh.setSoundPlayer(soundPlayer);

		controller.stop();
		controller = fresh;
		Micropolis city = controller.getEngine();
		viewport = new Viewport(city.getWidth(), city.getHeight(),
			(int) canvas.getWidth(), (int) canvas.getHeight());
		viewport.setZoom(DEFAULT_ZOOM);
		viewport.centreOnTile(city.getWidth() / 2, city.getHeight() / 2);
		controller.setFrameCallback(() -> Platform.runLater(this::redraw));
		controller.start();
		lastBudgetYear = currentYear();
		refreshSpeed();
		refreshPalette();
		updateStatus();
		redraw();
		if (miniVisible) {
			refreshMiniMap();
		}
	}

	private Menu buildDisastersMenu()
	{
		Menu m = new Menu("Disasters");
		MenuItem fire = new MenuItem("Fire");
		fire.setOnAction(e -> controller.triggerFire());
		MenuItem flood = new MenuItem("Flood");
		flood.setOnAction(e -> controller.triggerFlood());
		MenuItem tornado = new MenuItem("Tornado");
		tornado.setOnAction(e -> controller.triggerTornado());
		MenuItem quake = new MenuItem("Earthquake");
		quake.setOnAction(e -> controller.triggerEarthquake());
		MenuItem monster = new MenuItem("Monster");
		monster.setOnAction(e -> controller.triggerMonster());
		MenuItem meltdown = new MenuItem("Nuclear meltdown");
		meltdown.setOnAction(e -> controller.triggerMeltdown());
		MenuItem note = new MenuItem("(Plane crash & shipwreck happen during play)");
		note.setDisable(true);
		m.getItems().addAll(fire, flood, tornado, quake, monster, meltdown,
			new SeparatorMenuItem(), note);
		return m;
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

	private int currentYear()
	{
		Micropolis city = controller.getEngine();
		synchronized (city) {
			return city.cityTime / 48;
		}
	}

	/** Show the budget dialog automatically when a new city year begins. */
	private void maybeAutoBudget()
	{
		int year = currentYear();
		if (year > lastBudgetYear) {
			lastBudgetYear = year;
			if (autoShowBudget && !budgetOpen) {
				showBudgetDialog();
			}
		}
	}

	/**
	 * The budget dialog: tax-rate slider (0..20), road/fire/police funding sliders,
	 * live read-outs of tax revenue, expenses and cash flow in rand, and a "don't
	 * show automatically" checkbox (persisted). Apply writes back to the engine.
	 */
	private void showBudgetDialog()
	{
		if (budgetOpen) {
			return;
		}
		budgetOpen = true;
		final Micropolis city = controller.getEngine();
		int tax0;
		double road0, fire0, police0;
		synchronized (city) {
			tax0 = city.cityTax;
			road0 = city.roadPercent;
			fire0 = city.firePercent;
			police0 = city.policePercent;
		}

		final Slider taxS = new Slider(0, BudgetControl.MAX_TAX, tax0);
		final Slider roadS = new Slider(0, 100, road0 * 100);
		final Slider fireS = new Slider(0, 100, fire0 * 100);
		final Slider policeS = new Slider(0, 100, police0 * 100);

		final Label taxL = new Label();
		final Label roadL = new Label();
		final Label fireL = new Label();
		final Label policeL = new Label();
		final Label revenueL = new Label();
		final Label expensesL = new Label();
		final Label cashL = new Label();

		Runnable refresh = () -> {
			int tax = (int) Math.round(taxS.getValue());
			double road = roadS.getValue() / 100.0;
			double fire = fireS.getValue() / 100.0;
			double police = policeS.getValue() / 100.0;
			taxL.setText("Tax rate: " + tax + "%");
			roadL.setText("Road funding: " + (int) Math.round(road * 100) + "%");
			fireL.setText("Fire funding: " + (int) Math.round(fire * 100) + "%");
			policeL.setText("Police funding: " + (int) Math.round(police * 100) + "%");
			BudgetNumbers b = BudgetControl.preview(city, tax, road, fire, police);
			revenueL.setText("Tax revenue: " + CurrencyFormat.format(b.taxIncome));
			expensesL.setText("Expenses: " + CurrencyFormat.format(b.operatingExpenses));
			cashL.setText("Cash flow: " + CurrencyFormat.format(b.taxIncome - b.operatingExpenses));
		};
		taxS.valueProperty().addListener((o, a, b) -> refresh.run());
		roadS.valueProperty().addListener((o, a, b) -> refresh.run());
		fireS.valueProperty().addListener((o, a, b) -> refresh.run());
		policeS.valueProperty().addListener((o, a, b) -> refresh.run());
		refresh.run();

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(6);
		grid.setPadding(new Insets(12));
		grid.addRow(0, taxL, taxS);
		grid.addRow(1, roadL, roadS);
		grid.addRow(2, fireL, fireS);
		grid.addRow(3, policeL, policeS);

		CheckBox dontShow = new CheckBox("Don't show automatically");
		dontShow.setSelected(!autoShowBudget);

		Button applyBtn = new Button("Apply");
		Button cancelBtn = new Button("Cancel");
		HBox buttons = new HBox(8, applyBtn, cancelBtn);
		buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

		VBox content = new VBox(8, grid, new javafx.scene.control.Separator(),
			revenueL, expensesL, cashL, dontShow, buttons);
		content.setPadding(new Insets(12));

		final Stage dlg = new Stage();
		dlg.initOwner(stage);
		dlg.initModality(Modality.WINDOW_MODAL);
		dlg.setTitle("City Budget");
		dlg.setScene(new Scene(content));
		dlg.setOnHidden(e -> budgetOpen = false);

		applyBtn.setOnAction(e -> {
			int tax = (int) Math.round(taxS.getValue());
			BudgetControl.apply(city, tax, roadS.getValue() / 100.0,
				fireS.getValue() / 100.0, policeS.getValue() / 100.0);
			setAutoShowBudget(!dontShow.isSelected());
			dlg.close();
		});
		cancelBtn.setOnAction(e -> {
			setAutoShowBudget(!dontShow.isSelected());
			dlg.close();
		});
		dlg.show();
	}

	private void setAutoShowBudget(boolean v)
	{
		autoShowBudget = v;
		prefs.putBoolean("autoBudget", v);
	}

	/** The evaluation dialog: approval, score, population, class and top problems.
	 *  Read-only; closes on OK or Esc. */
	private void showEvaluationDialog()
	{
		EvaluationReport r = EvaluationReport.of(controller.getEngine());
		StringBuilder sb = new StringBuilder();
		sb.append("Is the mayor doing a good job?\n");
		sb.append("  Yes: ").append(r.approveYes).append("%    No: ").append(r.approveNo).append("%\n\n");
		sb.append("City score: ").append(r.score).append(" (").append(signed(r.scoreDelta)).append(")\n");
		sb.append("Population: ").append(r.population).append(" (").append(signed(r.populationDelta)).append(")\n");
		sb.append("Class: ").append(r.cityClass).append("\n\n");
		sb.append("What are the worst problems?\n");
		if (r.problems.length == 0) {
			sb.append("  (none reported)");
		} else {
			for (int i = 0; i < r.problems.length; i++) {
				sb.append("  ").append(i + 1).append(". ").append(r.problems[i]);
				if (i < r.problems.length - 1) sb.append('\n');
			}
		}
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.initOwner(stage);
		a.setTitle("City Evaluation");
		a.setHeaderText("City Evaluation");
		a.setContentText(sb.toString());
		a.show();
	}

	private static String signed(int v)
	{
		return v >= 0 ? "+" + v : Integer.toString(v);
	}

	/** The graphs dialog: six history line graphs with a 10-year / 120-year toggle. */
	private void showGraphsDialog()
	{
		final Canvas gcanvas = new Canvas(640, 360);
		final boolean[] longRange = { false };
		final Runnable draw = () -> drawGraphs(gcanvas, longRange[0]);

		final Button toggle = new Button("Show 120 years");
		toggle.setFocusTraversable(false);
		toggle.setOnAction(e -> {
			longRange[0] = !longRange[0];
			toggle.setText(longRange[0] ? "Show 10 years" : "Show 120 years");
			draw.run();
		});

		VBox content = new VBox(8, toggle, gcanvas);
		content.setPadding(new Insets(10));

		Stage dlg = new Stage();
		dlg.initOwner(stage);
		dlg.initModality(Modality.WINDOW_MODAL);
		dlg.setTitle("City Graphs");
		dlg.setScene(new Scene(content));
		draw.run();
		dlg.show();
	}

	private void drawGraphs(Canvas gcanvas, boolean longRange)
	{
		Micropolis city = controller.getEngine();
		GraphicsContext gc = gcanvas.getGraphicsContext2D();
		double w = gcanvas.getWidth();
		double h = gcanvas.getHeight();
		gc.setFill(Color.rgb(24, 24, 24));
		gc.fillRect(0, 0, w, h);

		GraphData.Series[] series = GraphData.Series.values();

		// Legend row.
		double pad = 10;
		double swatch = 12;
		double x = pad;
		double y = pad + swatch;
		gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
		for (GraphData.Series s : series) {
			gc.setFill(colorOf(s));
			gc.fillRect(x, y - swatch, swatch, swatch);
			gc.setFill(Color.WHITE);
			gc.fillText(s.label(), x + swatch + 4, y);
			x += swatch + 4 + s.label().length() * 8 + 14;
		}
		gc.setFill(Color.WHITE);
		String title = longRange ? "120 years" : "10 years";
		gc.fillText(title, w - pad - title.length() * 7, y);

		double plotLeft = pad;
		double plotTop = y + 16;
		double plotRight = w - pad;
		double plotBottom = h - pad;
		gc.setStroke(Color.rgb(255, 255, 255, 0.6));
		gc.setLineWidth(1.5);
		gc.strokeLine(plotLeft, plotBottom, plotRight, plotBottom);
		gc.strokeLine(plotLeft, plotTop, plotLeft, plotBottom);

		int n = GraphData.POINTS;
		double stepX = (plotRight - plotLeft) / (n - 1);
		double plotH = plotBottom - plotTop;
		gc.setLineWidth(2.0);
		for (GraphData.Series s : series) {
			float[] vals = GraphData.normalized(city, s, longRange);
			gc.setStroke(colorOf(s));
			gc.beginPath();
			for (int i = 0; i < n; i++) {
				double px = plotRight - i * stepX;
				double py = plotBottom - vals[i] * plotH;
				if (i == 0) gc.moveTo(px, py); else gc.lineTo(px, py);
			}
			gc.stroke();
		}
	}

	private static Color colorOf(GraphData.Series s)
	{
		int c = s.colorArgb();
		return Color.rgb((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
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
		maybeAutoBudget();
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
	private static final double SHAKE_MAX_PX = 6.0;

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
			spriteFrames = SpriteImages.capture(city);
		}

		// Earthquake: a small, decaying, purely-visual shake of everything drawn
		// (map coordinates untouched; the black fill above hides the edges).
		double shake = controller.shakeIntensity();
		boolean shaking = shake > 0.0;
		if (shaking) {
			gc.save();
			double mag = shake * SHAKE_MAX_PX;
			double dx = (Math.random() - 0.5) * 2.0 * mag;
			double dy = (Math.random() - 0.5) * 2.0 * mag;
			gc.translate(dx, dy);
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

		drawSprites(gc, zoom);
		drawPreview(gc, zoom, cycle, firstCol, lastCol, firstRow, lastRow);
		if (shaking) {
			gc.restore();
		}
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

	private void loadSprites()
	{
		for (SpriteKind kind : SpriteKind.values()) {
			for (int i = 0; i < kind.numFrames; i++) {
				java.io.InputStream in = DesktopApp.class.getResourceAsStream(
					SpriteImages.resourcePath(kind, i));
				if (in == null) {
					continue;
				}
				Image img = new Image(in);
				int w = (int) img.getWidth();
				int h = (int) img.getHeight();
				if (w <= 0 || h <= 0) {
					continue;
				}
				int[] px = new int[w * h];
				img.getPixelReader().getPixels(0, 0, w, h,
					PixelFormat.getIntArgbInstance(), px, 0, w);
				int key = kind.objectId * 100 + i;
				spritePixels.put(key, px);
				spriteDims.put(key, new int[] { w, h });
			}
		}
	}

	private void drawSprites(GraphicsContext gc, int zoom)
	{
		if (spriteFrames.isEmpty()) {
			return;
		}
		double cw = canvas.getWidth();
		double ch = canvas.getHeight();
		for (SpriteImages.Frame f : spriteFrames) {
			int key = f.objectId * 100 + f.frameIndex;
			Image img = spriteImage(key, zoom);
			if (img == null) {
				continue;
			}
			int sx = (f.x + f.offx) * zoom - viewport.getScrollX();
			int sy = (f.y + f.offy) * zoom - viewport.getScrollY();
			if (sx + img.getWidth() < 0 || sy + img.getHeight() < 0 || sx > cw || sy > ch) {
				continue;
			}
			gc.drawImage(img, sx, sy);
		}
	}

	/** A sprite frame scaled by the integer zoom (nearest-neighbour), cached. */
	private Image spriteImage(int key, int zoom)
	{
		long ck = (long) key * (Viewport.MAX_ZOOM + 1) + zoom;
		Image cached = spriteCache.get(ck);
		if (cached != null) {
			return cached;
		}
		int[] px = spritePixels.get(key);
		int[] dim = spriteDims.get(key);
		if (px == null || dim == null) {
			return null;
		}
		int w = dim[0], h = dim[1];
		int zw = w * zoom, zh = h * zoom;
		int[] out = new int[zw * zh];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = px[y * w + x];
				int baseIdx = (y * zoom) * zw + x * zoom;
				for (int dy = 0; dy < zoom; dy++) {
					int rowIdx = baseIdx + dy * zw;
					for (int dx = 0; dx < zoom; dx++) {
						out[rowIdx + dx] = p;
					}
				}
			}
		}
		WritableImage wi = new WritableImage(zw, zh);
		wi.getPixelWriter().setPixels(0, 0, zw, zh, PixelFormat.getIntArgbInstance(), out, 0, zw);
		spriteCache.put(ck, wi);
		return wi;
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
