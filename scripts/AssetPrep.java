/*
 * NietoCity - Phase 7 asset preparation (offline, headless).
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Turns the two big source images in the repo root into the small, shippable
 * assets the app needs, WITHOUT touching the originals:
 *
 *   1. splash_exit.png (~10 MB) -> a compressed splash_exit.jpg (well under 1 MB)
 *      for both the Android app (res/drawable-nodpi) and the desktop resources.
 *   2. AppIcon.png -> an Android adaptive launcher icon: a foreground layer with
 *      the artwork (the black corners flood-filled to transparent so the launcher
 *      mask shows the solid brand-blue background at the corners) at every mipmap
 *      density, plus legacy square/round PNG fallbacks and the background colour.
 *
 * Pure Java (javax.imageio + java.awt), run headless with the Android Studio JBR.
 * Nothing here ships in the app; it is a build-time tool like MakeTiles.
 *
 * Usage: java AssetPrep <repoRoot>
 */
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Locale;

public final class AssetPrep
{
	public static void main(String[] args) throws Exception
	{
		File root = new File(args.length > 0 ? args[0] : ".").getCanonicalFile();
		System.out.println("Repo root: " + root);

		makeSplashJpg(root);
		makeLauncherIcon(root);

		System.out.println("Asset preparation done.");
	}

	// --- splash_exit.png -> splash_exit.jpg (downscaled + JPEG-compressed) ---

	private static void makeSplashJpg(File root) throws Exception
	{
		File src = new File(root, "splash_exit.png");
		BufferedImage in = ImageIO.read(src);
		System.out.println("splash_exit.png: " + in.getWidth() + "x" + in.getHeight());

		// Downscale so the longest side is at most 1280px (plenty for a full-screen
		// splash), flattening any alpha onto black for JPEG.
		int maxSide = 1280;
		double scale = Math.min(1.0, (double) maxSide / Math.max(in.getWidth(), in.getHeight()));
		int w = (int) Math.round(in.getWidth() * scale);
		int h = (int) Math.round(in.getHeight() * scale);
		BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgb.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, w, h);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(in, 0, 0, w, h, null);
		g.dispose();

		File[] dests = {
			new File(root, "app/src/main/res/drawable-nodpi/splash_exit.jpg"),
			new File(root, "desktop/src/main/resources/splash_exit.jpg"),
		};
		for (File dest : dests) {
			dest.getParentFile().mkdirs();
			writeJpeg(rgb, dest, 0.82f);
			System.out.println("Wrote " + dest + " (" + dest.length() + " bytes, "
				+ w + "x" + h + ")");
		}
	}

	private static void writeJpeg(BufferedImage img, File dest, float quality) throws Exception
	{
		Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
		ImageWriter writer = it.next();
		ImageWriteParam p = writer.getDefaultWriteParam();
		p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		p.setCompressionQuality(quality);
		ImageOutputStream out = ImageIO.createImageOutputStream(dest);
		try {
			writer.setOutput(out);
			writer.write(null, new IIOImage(img, null, null), p);
		} finally {
			out.close();
			writer.dispose();
		}
	}

	// --- AppIcon.png -> adaptive launcher icon ---

	// Adaptive foreground layer sizes (px) per density bucket.
	private static final String[] DENSITIES = { "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi" };
	private static final int[] FG_SIZE = { 108, 162, 216, 324, 432 };
	private static final int[] LEGACY_SIZE = { 48, 72, 96, 144, 192 };

	private static void makeLauncherIcon(File root) throws Exception
	{
		File src = new File(root, "AppIcon.png");
		BufferedImage app = toArgb(ImageIO.read(src));
		System.out.println("AppIcon.png: " + app.getWidth() + "x" + app.getHeight());

		// Flood-fill the near-black corners to transparent so the launcher's mask
		// reveals the solid brand background there instead of black.
		clearBlackCorners(app);

		// Pick the brand background colour from the (blue) border ring: the first
		// opaque pixel scanning inward at the vertical middle.
		int bg = sampleBorderColour(app);
		String bgHex = String.format(Locale.US, "#%06X", bg & 0xFFFFFF);
		System.out.println("Brand background colour: " + bgHex);

		File res = new File(root, "app/src/main/res");

		// Foreground (transparent-cornered artwork) and legacy square/round icons
		// per density.
		for (int i = 0; i < DENSITIES.length; i++) {
			File dir = new File(res, "mipmap-" + DENSITIES[i]);
			dir.mkdirs();

			BufferedImage fg = scaled(app, FG_SIZE[i], FG_SIZE[i]);
			ImageIO.write(fg, "png", new File(dir, "ic_launcher_foreground.png"));

			// Legacy fallback (pre-adaptive launchers): artwork over the brand bg,
			// square and round. minSdk 26 uses the adaptive icon; these are safety.
			BufferedImage legacy = compositeOver(scaled(app, LEGACY_SIZE[i], LEGACY_SIZE[i]), bg);
			ImageIO.write(legacy, "png", new File(dir, "ic_launcher.png"));
			BufferedImage round = roundMask(legacy);
			ImageIO.write(round, "png", new File(dir, "ic_launcher_round.png"));

			System.out.println("mipmap-" + DENSITIES[i] + ": foreground " + FG_SIZE[i]
				+ "px, legacy " + LEGACY_SIZE[i] + "px");
		}

		// Adaptive icon XML (API 26+): foreground + solid background colour.
		File anydpi = new File(res, "mipmap-anydpi-v26");
		anydpi.mkdirs();
		String adaptive =
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
			+ "<!-- NietoCity adaptive launcher icon. Created by Nieto Software. GPLv3. -->\n"
			+ "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
			+ "    <background android:drawable=\"@color/ic_launcher_background\" />\n"
			+ "    <foreground android:drawable=\"@mipmap/ic_launcher_foreground\" />\n"
			+ "</adaptive-icon>\n";
		writeText(new File(anydpi, "ic_launcher.xml"), adaptive);
		writeText(new File(anydpi, "ic_launcher_round.xml"), adaptive);

		File values = new File(res, "values");
		values.mkdirs();
		String colours =
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
			+ "<!-- NietoCity launcher background (brand blue). Created by Nieto Software. GPLv3. -->\n"
			+ "<resources>\n"
			+ "    <color name=\"ic_launcher_background\">" + bgHex + "</color>\n"
			+ "</resources>\n";
		writeText(new File(values, "ic_launcher_background.xml"), colours);
	}

	/** Convert any image into a mutable ARGB buffer. */
	private static BufferedImage toArgb(BufferedImage in)
	{
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(in, 0, 0, null);
		g.dispose();
		return out;
	}

	/** Flood-fill near-black regions reachable from the four corners to transparent. */
	private static void clearBlackCorners(BufferedImage img)
	{
		int w = img.getWidth(), h = img.getHeight();
		boolean[] seen = new boolean[w * h];
		ArrayDeque<int[]> q = new ArrayDeque<int[]>();
		int[][] corners = { { 0, 0 }, { w - 1, 0 }, { 0, h - 1 }, { w - 1, h - 1 } };
		for (int[] c : corners) {
			pushIfBlack(img, seen, q, c[0], c[1]);
		}
		while (!q.isEmpty()) {
			int[] p = q.poll();
			int x = p[0], y = p[1];
			img.setRGB(x, y, 0x00000000); // transparent
			if (x > 0)     pushIfBlack(img, seen, q, x - 1, y);
			if (x < w - 1) pushIfBlack(img, seen, q, x + 1, y);
			if (y > 0)     pushIfBlack(img, seen, q, x, y - 1);
			if (y < h - 1) pushIfBlack(img, seen, q, x, y + 1);
		}
	}

	private static void pushIfBlack(BufferedImage img, boolean[] seen, ArrayDeque<int[]> q, int x, int y)
	{
		int idx = y * img.getWidth() + x;
		if (seen[idx]) {
			return;
		}
		seen[idx] = true;
		int argb = img.getRGB(x, y);
		int a = (argb >>> 24), r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
		if (a > 8 && r < 26 && g < 26 && b < 26) {
			q.add(new int[] { x, y });
		}
	}

	/** The first opaque pixel scanning inward at the vertical middle (the border). */
	private static int sampleBorderColour(BufferedImage img)
	{
		int y = img.getHeight() / 2;
		for (int x = 0; x < img.getWidth(); x++) {
			int argb = img.getRGB(x, y);
			if ((argb >>> 24) > 200) {
				return argb;
			}
		}
		return 0xFF1E5FBE; // fallback brand blue
	}

	private static BufferedImage scaled(BufferedImage src, int w, int h)
	{
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();
		return out;
	}

	/** Composite an ARGB image over a solid colour (for legacy square icons). */
	private static BufferedImage compositeOver(BufferedImage fg, int bg)
	{
		BufferedImage out = new BufferedImage(fg.getWidth(), fg.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setColor(new Color(bg, false));
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.drawImage(fg, 0, 0, null);
		g.dispose();
		return out;
	}

	/** Clip an image to a circle (for the legacy round icon). */
	private static BufferedImage roundMask(BufferedImage src)
	{
		int w = src.getWidth(), h = src.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, w, h));
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}

	private static void writeText(File f, String text) throws Exception
	{
		java.nio.file.Files.write(f.toPath(), text.getBytes("UTF-8"));
		System.out.println("Wrote " + f);
	}
}
