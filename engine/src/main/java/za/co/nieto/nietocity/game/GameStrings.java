/*
 * NietoCity - shared string formatting.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Loads the MicropolisJ English string bundles (kept in the engine resources) and
 * formats the same values the original UI shows: the game date, funds, tool
 * names, engine messages, zone names and status lines. Pure Java 8; the bundles
 * load from the classpath via ResourceBundle, which works on desktop and Android.
 */
package za.co.nieto.nietocity.game;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.ResourceBundle;

import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.MicropolisTool;

public final class GameStrings
{
	private static final ResourceBundle GUI = ResourceBundle.getBundle("micropolisj.GuiStrings");
	private static final ResourceBundle CITY = ResourceBundle.getBundle("micropolisj.CityMessages");
	private static final ResourceBundle STATUS = ResourceBundle.getBundle("micropolisj.StatusMessages");

	private GameStrings() {}

	/** Funds formatted the way the original does, e.g. "$1,000,000" style. */
	public static String formatFunds(int funds)
	{
		return MessageFormat.format(GUI.getString("funds"), funds);
	}

	/** Game date ("MMM yyyy") derived from cityTime exactly as the original. */
	public static String formatGameDate(int cityTime)
	{
		Calendar c = Calendar.getInstance();
		c.set(1900 + cityTime / 48, (cityTime % 48) / 4, (cityTime % 4) * 7 + 1);
		return MessageFormat.format(GUI.getString("citytime"), c.getTime());
	}

	/** Display name of a tool (from GuiStrings, falling back to the enum name). */
	public static String toolName(MicropolisTool tool)
	{
		if (tool == null) {
			return "";
		}
		String key = "tool." + tool.name() + ".name";
		return GUI.containsKey(key) ? GUI.getString(key) : tool.name();
	}

	/** Human-readable text for an engine message. */
	public static String cityMessage(MicropolisMessage message)
	{
		String key = message.name();
		return CITY.containsKey(key) ? CITY.getString(key) : key;
	}

	/** Zone/terrain name for a query (StatusMessages zone.N). */
	public static String zoneName(int building)
	{
		String key = "zone." + building;
		return STATUS.containsKey(key) ? STATUS.getString(key) : ("#" + building);
	}

	/** A status line label (StatusMessages status.N: density/land value/etc.). */
	public static String statusLine(int index)
	{
		String key = "status." + index;
		return STATUS.containsKey(key) ? STATUS.getString(key) : ("#" + index);
	}
}
