/*
 * NietoCity - platform-neutral tile index.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Parses the tile atlas index (16x16/tiles.idx, produced by scripts/
 * compose-tiles.cmd) into, for each tile number, the list of frame Y offsets in
 * the vertical atlas strip. Static tiles have one frame; animated tiles (water,
 * fire, smoke, ...) have several, selected by the engine's animation cycle using
 * the same timing as the original MicropolisJ renderer.
 *
 * Pure Java 8, no java.awt and no Android. NOTE: the atlas index is parsed with a
 * small dependency-free scanner rather than the engine's XML_Helper, because
 * XML_Helper uses javax.xml.stream (StAX) which the Android runtime does not
 * provide - using it here would crash on the phone.
 */
package za.co.nieto.nietocity.render;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TileIndex
{
	/** Tile edge length, in pixels, of the atlas this index describes. */
	public static final int TILE_SIZE = 16;

	/** The animation-frame period the original renderer uses, in milliseconds. */
	private static final int FRAME_QUANTUM_MS = 125;

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Pattern TILE_P =
		Pattern.compile("<tile\\s+name=\"(\\d+)\">(.*?)</tile>", Pattern.DOTALL);
	private static final Pattern IMAGE_P = Pattern.compile("at=\"(\\d+),(\\d+)\"");
	private static final Pattern DURATION_P = Pattern.compile("duration=\"(\\d+)\"");

	// Indexed by tile number. Null entries are tile numbers with no artwork
	// (animation-reserved numbers that never appear on a real map).
	private final int[][] frameOffsetY;    // [tile][frame] = y offset in the strip
	private final int[][] frameDurationMs; // [tile][frame] = duration (ms), animated tiles only
	private final int[] totalDurationMs;   // [tile] = sum of frame durations (0 for static tiles)

	private TileIndex(int[][] frameOffsetY, int[][] frameDurationMs, int[] totalDurationMs)
	{
		this.frameOffsetY = frameOffsetY;
		this.frameDurationMs = frameDurationMs;
		this.totalDurationMs = totalDurationMs;
	}

	/** Load the shipped atlas index from the classpath (/16x16/tiles.idx). */
	public static TileIndex loadDefault()
	{
		try (InputStream in = TileIndex.class.getResourceAsStream("/16x16/tiles.idx")) {
			if (in == null) {
				throw new IllegalStateException("Missing atlas index resource /16x16/tiles.idx");
			}
			return load(in);
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not read /16x16/tiles.idx", e);
		}
	}

	/** Parse a tile index from a stream. The stream is not closed. */
	public static TileIndex load(InputStream in) throws IOException
	{
		String xml = readAll(in);

		List<int[]> offsets = new ArrayList<int[]>();
		List<int[]> durations = new ArrayList<int[]>();
		int maxTile = -1;

		Matcher tm = TILE_P.matcher(xml);
		// Parse into temporary maps first (tile numbers are not guaranteed dense).
		List<Integer> tileNums = new ArrayList<Integer>();
		while (tm.find()) {
			int tile = Integer.parseInt(tm.group(1));
			String body = tm.group(2);

			List<Integer> ys = new ArrayList<Integer>();
			Matcher im = IMAGE_P.matcher(body);
			while (im.find()) {
				ys.add(Integer.parseInt(im.group(2))); // y offset
			}

			List<Integer> durs = new ArrayList<Integer>();
			Matcher dm = DURATION_P.matcher(body);
			while (dm.find()) {
				durs.add(Integer.parseInt(dm.group(1)));
			}

			if (ys.isEmpty()) {
				continue; // malformed tile entry; skip defensively
			}

			int[] offArr = new int[ys.size()];
			for (int i = 0; i < offArr.length; i++) offArr[i] = ys.get(i);

			int[] durArr;
			if (durs.size() == ys.size()) {
				durArr = new int[durs.size()];
				for (int i = 0; i < durArr.length; i++) durArr[i] = durs.get(i);
			}
			else {
				// Static tile (single image, no <frame duration>).
				durArr = new int[offArr.length];
			}

			tileNums.add(tile);
			offsets.add(offArr);
			durations.add(durArr);
			if (tile > maxTile) maxTile = tile;
		}

		int size = maxTile + 1;
		int[][] offBy = new int[size][];
		int[][] durBy = new int[size][];
		int[] totalBy = new int[size];
		for (int k = 0; k < tileNums.size(); k++) {
			int tile = tileNums.get(k);
			int[] offArr = offsets.get(k);
			int[] durArr = durations.get(k);
			offBy[tile] = offArr;
			durBy[tile] = durArr;
			int total = 0;
			for (int d : durArr) total += d;
			totalBy[tile] = total;
		}

		return new TileIndex(offBy, durBy, totalBy);
	}

	/** Number of tile numbers this index spans (max tile number + 1). */
	public int size()
	{
		return frameOffsetY.length;
	}

	/** True if the given tile number has artwork in the atlas. */
	public boolean hasImage(int tileNumber)
	{
		return tileNumber >= 0
			&& tileNumber < frameOffsetY.length
			&& frameOffsetY[tileNumber] != null;
	}

	/** Number of animation frames for the tile (1 for static tiles). */
	public int frameCount(int tileNumber)
	{
		return hasImage(tileNumber) ? frameOffsetY[tileNumber].length : 0;
	}

	/**
	 * The Y offset in the atlas strip for the given tile at the given animation
	 * cycle (engine.getAnimationCycle()). Uses the same frame timing as the
	 * original renderer. Returns the single frame for static tiles.
	 *
	 * @throws IllegalArgumentException if the tile number has no artwork.
	 */
	public int frameOffsetY(int tileNumber, int animationCycle)
	{
		if (!hasImage(tileNumber)) {
			throw new IllegalArgumentException("No artwork for tile " + tileNumber);
		}
		int[] offs = frameOffsetY[tileNumber];
		if (offs.length == 1) {
			return offs[0];
		}
		int total = totalDurationMs[tileNumber];
		int[] durs = frameDurationMs[tileNumber];
		int t = (animationCycle * FRAME_QUANTUM_MS) % total;
		for (int i = 0; i < offs.length - 1; i++) {
			t -= durs[i];
			if (t < 0) {
				return offs[i];
			}
		}
		return offs[offs.length - 1];
	}

	private static String readAll(InputStream in) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = in.read(buf)) != -1) {
			bos.write(buf, 0, n);
		}
		return new String(bos.toByteArray(), UTF8);
	}
}
