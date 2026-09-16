/*
 * NietoCity - desktop entry point.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Launches the JavaFX 8 desktop renderer (DesktopApp). JavaFX is provided by the
 * Java 8 "Full" runtime (e.g. Liberica JRE 8 Full); it is not bundled in the JAR.
 */
package za.co.nieto.nietocity.desktop;

import javafx.application.Application;

public final class Main
{
	private Main() {}

	public static void main(String[] args)
	{
		Application.launch(DesktopApp.class, args);
	}
}
