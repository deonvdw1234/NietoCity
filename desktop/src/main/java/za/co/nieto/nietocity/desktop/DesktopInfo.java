/*
 * NietoCity - text for the desktop About / Licence / How-to-play screens.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * About and Licence get their full content in tasks 5 and 6; How-to-play in
 * task 4. The licence text is read from the bundled classpath resource.
 */
package za.co.nieto.nietocity.desktop;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class DesktopInfo
{
	public static final String ABOUT = "about";
	public static final String LICENCE = "licence";
	public static final String EDUCATION = "education";

	private DesktopInfo() { }

	public static String about()
	{
		return "About NietoCity.";
	}

	public static String licence()
	{
		String text = loadResource("/license/LICENSE.txt");
		return text != null ? text : "Licence text is unavailable in this build.";
	}

	public static String howToPlay()
	{
		return "How to play.";
	}

	/** Read a bundled UTF-8 text resource from the classpath, or null if absent. */
	static String loadResource(String path)
	{
		InputStream in = DesktopInfo.class.getResourceAsStream(path);
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
