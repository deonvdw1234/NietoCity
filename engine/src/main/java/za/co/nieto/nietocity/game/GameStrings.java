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

import micropolisj.engine.CityProblem;
import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.Speed;

public final class GameStrings
{
	/**
	 * City-size class name (CityEval.cityClass 0..5). The original reads these from
	 * a CityStrings bundle that was not part of the imported source, so the names
	 * are provided here (matching the engine's doPopNum thresholds).
	 */
	public static String cityClassName(int cityClass)
	{
		switch (cityClass) {
		case 0:  return "Village";
		case 1:  return "Town";
		case 2:  return "City";
		case 3:  return "Capital";
		case 4:  return "Metropolis";
		case 5:  return "Megalopolis";
		default: return "Village";
		}
	}

	/** Display name for a city problem (the CityStrings bundle was not imported). */
	public static String problemName(CityProblem p)
	{
		if (p == null) {
			return "";
		}
		switch (p) {
		case CRIME:        return "Crime";
		case POLLUTION:    return "Pollution";
		case HOUSING:      return "Housing costs";
		case TAXES:        return "Taxes";
		case TRAFFIC:      return "Traffic";
		case UNEMPLOYMENT: return "Unemployment";
		case FIRE:         return "Fire";
		default:           return p.name();
		}
	}

	/** Short display name for a simulation speed (status bar speed label). */
	public static String speedName(Speed speed)
	{
		if (speed == null) {
			return "";
		}
		switch (speed) {
		case PAUSED:     return "Paused";
		case SLOW:       return "Slow";
		case NORMAL:     return "Normal";
		case FAST:       return "Fast";
		case SUPER_FAST: return "Ultra";
		default:         return speed.name();
		}
	}

	private static final ResourceBundle GUI = ResourceBundle.getBundle("micropolisj.GuiStrings");
	private static final ResourceBundle CITY = ResourceBundle.getBundle("micropolisj.CityMessages");
	private static final ResourceBundle STATUS = ResourceBundle.getBundle("micropolisj.StatusMessages");

	private GameStrings() {}

	/** Funds/costs formatted as South African rand, e.g. "R8 833" (never a "$"). */
	public static String formatFunds(int funds)
	{
		return CurrencyFormat.format(funds);
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

	/** A GUI label (GuiStrings), e.g. notification.density_lbl. */
	public static String gui(String key)
	{
		return GUI.containsKey(key) ? GUI.getString(key) : key;
	}
}
