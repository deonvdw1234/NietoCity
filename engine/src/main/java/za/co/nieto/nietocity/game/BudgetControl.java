/*
 * NietoCity - shared budget read/apply helper.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * Thin, platform-neutral wrapper over the engine's budget so both the Android and
 * desktop budget dialogs share the same read-out and write-back logic. The engine
 * computes the budget numbers (generateBudget); this only reads them and writes
 * the player's tax rate and funding percentages back onto the engine's public
 * budget fields. All access is under the engine lock. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

import micropolisj.engine.BudgetNumbers;
import micropolisj.engine.Micropolis;

public final class BudgetControl
{
	public static final int MIN_TAX = 0;
	public static final int MAX_TAX = 20;

	private BudgetControl() { }

	/** The budget numbers for the engine's current settings. */
	public static BudgetNumbers current(Micropolis city)
	{
		synchronized (city) {
			return city.generateBudget();
		}
	}

	/**
	 * The budget numbers that the given proposed settings would produce, without
	 * changing the running simulation: the engine fields are set, generateBudget()
	 * is read, then the fields are restored. Lets a dialog show live read-outs as
	 * the sliders move before the player commits with {@link #apply}.
	 */
	public static BudgetNumbers preview(Micropolis city, int taxRate,
		double roadPercent, double firePercent, double policePercent)
	{
		synchronized (city) {
			int oldTax = city.cityTax;
			double oldRoad = city.roadPercent;
			double oldFire = city.firePercent;
			double oldPolice = city.policePercent;
			try {
				city.cityTax = clampTax(taxRate);
				city.roadPercent = clamp01(roadPercent);
				city.firePercent = clamp01(firePercent);
				city.policePercent = clamp01(policePercent);
				return city.generateBudget();
			} finally {
				city.cityTax = oldTax;
				city.roadPercent = oldRoad;
				city.firePercent = oldFire;
				city.policePercent = oldPolice;
			}
		}
	}

	/** Commit the player's tax rate (0..20) and funding percents (0..1) to the engine. */
	public static void apply(Micropolis city, int taxRate,
		double roadPercent, double firePercent, double policePercent)
	{
		synchronized (city) {
			city.cityTax = clampTax(taxRate);
			city.roadPercent = clamp01(roadPercent);
			city.firePercent = clamp01(firePercent);
			city.policePercent = clamp01(policePercent);
		}
	}

	private static int clampTax(int t)
	{
		if (t < MIN_TAX) return MIN_TAX;
		if (t > MAX_TAX) return MAX_TAX;
		return t;
	}

	private static double clamp01(double v)
	{
		if (v < 0.0) return 0.0;
		if (v > 1.0) return 1.0;
		return v;
	}
}
