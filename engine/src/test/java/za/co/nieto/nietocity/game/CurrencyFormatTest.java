/*
 * NietoCity - currency formatter tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.*;
import org.junit.Test;

public class CurrencyFormatTest
{
	@Test
	public void randPrefixNoSpaceGroupedWithSpaces()
	{
		assertEquals("R0", CurrencyFormat.format(0));
		assertEquals("R10", CurrencyFormat.format(10));
		assertEquals("R999", CurrencyFormat.format(999));
		assertEquals("R8 833", CurrencyFormat.format(8833));      // Andre's phone example
		assertEquals("R20 000", CurrencyFormat.format(20000));    // easy starting funds
		assertEquals("R1 000 000", CurrencyFormat.format(1_000_000));
	}

	@Test
	public void negativeAmountsKeepTheSignBeforeTheSymbol()
	{
		assertEquals("-R500", CurrencyFormat.format(-500));
		assertEquals("-R8 833", CurrencyFormat.format(-8833));
	}

	@Test
	public void neverEmitsADollarSign()
	{
		assertFalse(CurrencyFormat.format(1234).contains("$"));
		assertTrue(CurrencyFormat.format(1234).startsWith("R"));
	}

	@Test
	public void gameStringsFundsUseRand()
	{
		assertEquals("R20 000", GameStrings.formatFunds(20000));
	}
}
