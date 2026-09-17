/*
 * NietoCity - money formatting (South African rand).
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * The single place any money figure we produce is formatted: an "R" prefix with
 * no space, thousands grouped with a space (rand convention, e.g. R8 833). We use
 * a space separator explicitly rather than the JVM default locale so the phone
 * and the Windows desktop show the same thing. Engine message strings from the
 * bundles keep their own wording; this only formats the figures we insert. No "$"
 * ever comes from our code. Pure Java 8; usable from :engine, :app and :desktop.
 */
package za.co.nieto.nietocity.game;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CurrencyFormat
{
	/** The rand symbol prefixed to every figure. */
	public static final String SYMBOL = "R";

	// DecimalFormat is not thread-safe; guard the shared instance.
	private static final DecimalFormat GROUPED;
	static {
		DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
		sym.setGroupingSeparator(' ');
		GROUPED = new DecimalFormat("#,##0", sym);
		GROUPED.setGroupingUsed(true);
	}

	private CurrencyFormat() { }

	/** Format an amount of rand, e.g. 8833 -> "R8 833", -500 -> "-R500". */
	public static String format(long amount)
	{
		String digits;
		synchronized (GROUPED) {
			digits = GROUPED.format(Math.abs(amount));
		}
		return (amount < 0 ? "-" + SYMBOL : SYMBOL) + digits;
	}
}
