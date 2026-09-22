/*
 * NietoCity - shared licence text loader.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Reads the bundled GPLv3 LICENSE and the THIRD_PARTY notices from the engine's
 * classpath resources and returns them as one block for the Licence screen. This
 * is the GPL-compliance surface for the distributed build, so the text ships with
 * both the app and the desktop jar. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class LicenseText
{
	private LicenseText() { }

	/** The full licence block: the GPLv3 text, then the third-party notices. */
	public static String full()
	{
		String licence = load("/license/LICENSE.txt");
		String third = load("/license/THIRD_PARTY.txt");
		StringBuilder sb = new StringBuilder();
		if (licence != null) {
			sb.append(licence);
		}
		if (third != null) {
			if (sb.length() > 0) {
				sb.append("\n\n");
			}
			sb.append("========================================\n");
			sb.append("THIRD-PARTY NOTICES\n");
			sb.append("========================================\n\n");
			sb.append(third);
		}
		return sb.length() > 0 ? sb.toString() : "Licence text is unavailable in this build.";
	}

	/** Read a bundled UTF-8 text resource from the engine classpath, or null. */
	private static String load(String path)
	{
		InputStream in = LicenseText.class.getResourceAsStream(path);
		if (in == null) {
			return null;
		}
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			char[] buf = new char[8192];
			int n;
			while ((n = r.read(buf)) > 0) {
				sb.append(buf, 0, n);
			}
			r.close();
			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}
}
