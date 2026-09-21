/*
 * NietoCity - save-slot metadata record.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * A small sidecar record for a saved city (its display name, when it was saved,
 * and its population and funds), so the Load screen can list slots without
 * loading every .cty. Stored as a plain .properties file next to the .cty; the
 * .cty itself stays standard classic bytes with nothing appended. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public final class SaveMeta
{
	public final String name;
	public final long dateMillis;
	public final int population;
	public final int funds;

	public SaveMeta(String name, long dateMillis, int population, int funds)
	{
		this.name = name;
		this.dateMillis = dateMillis;
		this.population = population;
		this.funds = funds;
	}

	/** Write this record to a sidecar .properties file. */
	public void write(File metaFile) throws IOException
	{
		Properties p = new Properties();
		p.setProperty("name", name);
		p.setProperty("date", Long.toString(dateMillis));
		p.setProperty("population", Integer.toString(population));
		p.setProperty("funds", Integer.toString(funds));
		OutputStream out = new FileOutputStream(metaFile);
		try {
			p.store(out, "NietoCity save metadata");
		} finally {
			out.close();
		}
	}

	/**
	 * Read a sidecar record, falling back to the given default name and zeros if
	 * the file is missing or unreadable (a save is still usable without its meta).
	 */
	public static SaveMeta read(File metaFile, String fallbackName)
	{
		Properties p = new Properties();
		if (metaFile.exists()) {
			try {
				InputStream in = new FileInputStream(metaFile);
				try {
					p.load(in);
				} finally {
					in.close();
				}
			} catch (IOException e) {
				// fall through to defaults
			}
		}
		return new SaveMeta(
			p.getProperty("name", fallbackName),
			parseLong(p.getProperty("date"), 0L),
			parseInt(p.getProperty("population"), 0),
			parseInt(p.getProperty("funds"), 0));
	}

	private static long parseLong(String s, long dflt)
	{
		try { return s != null ? Long.parseLong(s) : dflt; } catch (NumberFormatException e) { return dflt; }
	}

	private static int parseInt(String s, int dflt)
	{
		try { return s != null ? Integer.parseInt(s) : dflt; } catch (NumberFormatException e) { return dflt; }
	}
}
