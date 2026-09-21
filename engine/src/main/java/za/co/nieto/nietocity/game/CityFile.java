/*
 * NietoCity - save/load facade for the classic binary .cty format.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * A thin, platform-neutral wrapper the Android and desktop apps use to save and
 * load cities. Saving snapshots the city under the engine lock (the same lock
 * animate() takes), so no simulation tick can tear the snapshot, and writes to a
 * temporary file first, then renames it into place, so an interrupted save never
 * corrupts an existing one. Loading delegates to the engine's own load(). The
 * bytes are standard classic v1 .cty - nothing is appended - so they interchange
 * with the engine's reader and with classic SimCity/Micropolis. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import micropolisj.engine.CityWriterV1;
import micropolisj.engine.Micropolis;

public final class CityFile
{
	/** The exact size of a classic binary v1 .cty file. */
	public static final int FILE_SIZE = CityWriterV1.FILE_SIZE;

	private CityFile() { }

	/** Save the city to a .cty file (snapshotted under the engine lock, atomic rename). */
	public static void save(Micropolis city, File file) throws IOException
	{
		File dir = file.getParentFile();
		if (dir != null && !dir.exists()) {
			dir.mkdirs();
		}
		File tmp = new File(dir, file.getName() + ".tmp");
		OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp));
		try {
			synchronized (city) {
				CityWriterV1.write(city, out);
			}
		} finally {
			out.close();
		}
		if (file.exists() && !file.delete()) {
			throw new IOException("Could not replace " + file);
		}
		if (!tmp.renameTo(file)) {
			throw new IOException("Could not save " + file);
		}
	}

	/** Save the city to a stream (snapshotted under the engine lock). */
	public static void save(Micropolis city, OutputStream out) throws IOException
	{
		synchronized (city) {
			CityWriterV1.write(city, out);
		}
	}

	/** Load a .cty file into the given engine (under the engine lock). */
	public static void load(Micropolis city, File file) throws IOException
	{
		synchronized (city) {
			city.load(file);
		}
	}
}
