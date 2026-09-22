/*
 * NietoCity - shared app / About information.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * The credits and description shown on the About screen, kept in one place so
 * both platforms show the same text. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

public final class AppInfo
{
	/** Fallback display version (the Android app reads its real versionName). */
	public static final String VERSION = "0.1";

	private AppInfo() { }

	/** The About screen text for the given display version. */
	public static String about(String version)
	{
		return "NietoCity\n"
			+ "Created by Nieto Software\n"
			+ "Version " + version + "\n"
			+ "\n"
			+ "An offline city-building game, based on Micropolis - the GPLv3 "
			+ "open-source release of the classic city simulator.\n"
			+ "\n"
			+ "Credits\n"
			+ "• Micropolis - the open-source city simulator this game is built on.\n"
			+ "• Electronic Arts Inc. - the original SimCity, which Micropolis derives from.\n"
			+ "• Maxis and Will Wright - creators of the original game and its design.\n"
			+ "• Don Hopkins - the Micropolis open-source release and its GPL relicensing.\n"
			+ "• Jason Long - MicropolisJ, the Java port this engine comes from.\n"
			+ "\n"
			+ "NietoCity is free software under the GNU General Public License, version 3 "
			+ "(see the Licence screen). It runs entirely offline: no internet permission, "
			+ "no analytics and no update checks.";
	}
}
