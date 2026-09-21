/*
 * NietoCity - desktop save storage (under the user home).
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Saves cities in <user.home>/NietoCity/saves as standard classic <base>.cty
 * files with a <base>.meta sidecar (name, date, population, funds). Never writes
 * into the repo or Dropbox. The Save-as / Open-.cty file chooser (for arbitrary
 * locations) lives in the UI. Pure Java; runs on Java 8.
 */
package za.co.nieto.nietocity.desktop;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import micropolisj.engine.Micropolis;
import za.co.nieto.nietocity.game.CityFile;
import za.co.nieto.nietocity.game.GameController;
import za.co.nieto.nietocity.game.SaveMeta;

public final class DesktopSaveStore
{
	/** One saved city: its file base name, metadata and .cty file. */
	public static final class Slot
	{
		public final String base;
		public final SaveMeta meta;
		public final File cty;

		Slot(String base, SaveMeta meta, File cty)
		{
			this.base = base;
			this.meta = meta;
			this.cty = cty;
		}
	}

	private final File dir;

	public DesktopSaveStore()
	{
		this.dir = new File(new File(System.getProperty("user.home"), "NietoCity"), "saves");
		dir.mkdirs();
	}

	public File getDir()
	{
		return dir;
	}

	public File ctyFile(String base)
	{
		return new File(dir, base + ".cty");
	}

	public boolean exists(String displayName)
	{
		return ctyFile(slug(displayName)).exists();
	}

	/** Save the running city under the given display name (.cty + .meta). */
	public void save(String displayName, GameController controller) throws IOException
	{
		String base = slug(displayName);
		Micropolis engine = controller.getEngine();
		CityFile.save(engine, ctyFile(base));
		int pop, funds;
		synchronized (engine) {
			pop = engine.getCityPopulation();
			funds = engine.budget.totalFunds;
		}
		new SaveMeta(displayName, System.currentTimeMillis(), pop, funds)
			.write(new File(dir, base + ".meta"));
	}

	/** All saved slots, newest first. */
	public List<Slot> list()
	{
		File[] ctys = dir.listFiles(new FilenameFilter() {
			public boolean accept(File d, String name) { return name.endsWith(".cty"); }
		});
		List<Slot> out = new ArrayList<Slot>();
		if (ctys != null) {
			for (File cty : ctys) {
				String base = cty.getName().substring(0, cty.getName().length() - 4);
				SaveMeta meta = SaveMeta.read(new File(dir, base + ".meta"), base);
				out.add(new Slot(base, meta, cty));
			}
		}
		Collections.sort(out, new Comparator<Slot>() {
			public int compare(Slot a, Slot b) { return Long.compare(b.meta.dateMillis, a.meta.dateMillis); }
		});
		return out;
	}

	public void delete(String base)
	{
		new File(dir, base + ".cty").delete();
		new File(dir, base + ".meta").delete();
	}

	private static String slug(String name)
	{
		String t = name.trim();
		if (t.isEmpty()) {
			t = "city";
		}
		return t.replaceAll("[^A-Za-z0-9._-]", "_");
	}
}
