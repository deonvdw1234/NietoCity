/*
 * NietoCity - shared sprite frame resolver and snapshot.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Resolves the per-kind sprite frame images on the classpath (the obj<id>-<frame>
 * PNGs bundled under /sprites) and snapshots the engine's live sprites for the
 * renderer. Like the tile atlas, this shared core only resolves classpath
 * resources and copies plain values - it never decodes images (the engine module
 * must stay free of java.awt/android); each platform loads the PNGs itself.
 *
 * Sprite positions are in map pixels where 16 px == one tile; a sprite's top-left
 * on screen is (x + offx) * zoom - scroll. Frame is 1-based in the engine (0 means
 * the sprite is dead), so the image index is frame - 1.
 */
package za.co.nieto.nietocity.render;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import micropolisj.engine.Micropolis;
import micropolisj.engine.Sprite;
import micropolisj.engine.SpriteKind;

public final class SpriteImages
{
	private SpriteImages() { }

	/** Classpath path of a frame image for an object id and 0-based frame index. */
	public static String resourcePath(int objectId, int frameIndex)
	{
		return "/sprites/obj" + objectId + "-" + frameIndex + ".png";
	}

	public static String resourcePath(SpriteKind kind, int frameIndex)
	{
		return resourcePath(kind.objectId, frameIndex);
	}

	/** The frame image resource, or null if it is not on the classpath. */
	public static URL resource(SpriteKind kind, int frameIndex)
	{
		return SpriteImages.class.getResource(resourcePath(kind, frameIndex));
	}

	/** True if every frame of the given kind resolves to a bundled image. */
	public static boolean hasAllFrames(SpriteKind kind)
	{
		for (int i = 0; i < kind.numFrames; i++) {
			if (resource(kind, i) == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * One visible sprite captured for drawing: its object id and 0-based frame
	 * index (which image), and its position/size in map pixels. Plain values, so
	 * the renderer can draw it after releasing the engine lock without tearing.
	 */
	public static final class Frame
	{
		public final int objectId;
		public final int frameIndex;
		public final int x, y, offx, offy, width, height;

		Frame(Sprite s)
		{
			this.objectId = s.kind.objectId;
			this.frameIndex = s.frame - 1;
			this.x = s.x;
			this.y = s.y;
			this.offx = s.offx;
			this.offy = s.offy;
			this.width = s.width;
			this.height = s.height;
		}
	}

	/**
	 * Snapshot the currently visible sprites. The caller must already hold the
	 * engine lock (it reads live sprite state). Returns an empty list if none.
	 */
	public static List<Frame> capture(Micropolis city)
	{
		List<Frame> out = new ArrayList<Frame>();
		for (Sprite s : city.allSprites()) {
			if (s.isVisible() && s.frame >= 1) {
				out.add(new Frame(s));
			}
		}
		return out;
	}
}
