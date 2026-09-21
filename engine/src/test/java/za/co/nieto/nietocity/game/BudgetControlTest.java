/*
 * NietoCity - budget write-back tests.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Proves that applying a tax rate and the road/fire/police funding percents from
 * the budget dialog writes those values onto the engine's budget fields (clamped
 * to the valid ranges), and that previewing does not disturb the running values.
 */
package za.co.nieto.nietocity.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import micropolisj.engine.Micropolis;

public class BudgetControlTest
{
	@Test
	public void applyWritesTaxAndPercents()
	{
		Micropolis c = GameController.newGame().getEngine();
		BudgetControl.apply(c, 12, 0.50, 0.25, 0.75);
		assertEquals(12, c.cityTax);
		assertEquals(0.50, c.roadPercent, 1e-9);
		assertEquals(0.25, c.firePercent, 1e-9);
		assertEquals(0.75, c.policePercent, 1e-9);
	}

	@Test
	public void applyClampsToValidRanges()
	{
		Micropolis c = GameController.newGame().getEngine();
		BudgetControl.apply(c, 30, 1.5, -0.2, 2.0);
		assertEquals(BudgetControl.MAX_TAX, c.cityTax);
		assertEquals(1.0, c.roadPercent, 1e-9);
		assertEquals(0.0, c.firePercent, 1e-9);
		assertEquals(1.0, c.policePercent, 1e-9);

		BudgetControl.apply(c, -5, 0.0, 0.0, 0.0);
		assertEquals(BudgetControl.MIN_TAX, c.cityTax);
	}

	@Test
	public void previewDoesNotChangeEngineFields()
	{
		Micropolis c = GameController.newGame().getEngine();
		BudgetControl.apply(c, 7, 1.0, 1.0, 1.0);
		BudgetControl.preview(c, 20, 0.1, 0.2, 0.3);
		// The running values are unchanged by a preview.
		assertEquals(7, c.cityTax);
		assertEquals(1.0, c.roadPercent, 1e-9);
		assertEquals(1.0, c.firePercent, 1e-9);
		assertEquals(1.0, c.policePercent, 1e-9);
	}

	@Test
	public void previewReflectsTheProposedTaxRate()
	{
		Micropolis c = GameController.newGame().getEngine();
		// A higher tax rate never yields less tax income than a zero rate.
		int zero = BudgetControl.preview(c, 0, 1.0, 1.0, 1.0).taxIncome;
		int high = BudgetControl.preview(c, 20, 1.0, 1.0, 1.0).taxIncome;
		assertEquals(0, zero);
		org.junit.Assert.assertTrue("higher tax >= zero tax income", high >= zero);
	}
}
