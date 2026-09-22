/*
 * NietoCity - text for the desktop About / Licence / How-to-play screens.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * About and Licence get their full content in tasks 5 and 6; How-to-play in
 * task 4. The licence text is read from the bundled classpath resource.
 */
package za.co.nieto.nietocity.desktop;

public final class DesktopInfo
{
	public static final String ABOUT = "about";
	public static final String LICENCE = "licence";
	public static final String EDUCATION = "education";

	private DesktopInfo() { }

	public static String about()
	{
		return za.co.nieto.nietocity.game.AppInfo.about(
			za.co.nieto.nietocity.game.AppInfo.VERSION);
	}

	public static String licence()
	{
		return za.co.nieto.nietocity.game.LicenseText.full();
	}

	public static String howToPlay()
	{
		return za.co.nieto.nietocity.game.Education.fullReference();
	}
}
