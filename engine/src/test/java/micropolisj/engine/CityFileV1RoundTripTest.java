/*
 * NietoCity - golden round-trip test for the classic binary .cty (v1) writer.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * The engine's own upstream reader (load_v1) is the independent check for our
 * writer (CityWriterV1). This is the highest-risk test in the project: a wrong
 * writer loses a city. It is deliberately tolerant of the transient bits that
 * load_v1 clears and rescans (ZONE/ANIM/BULL/BURN/COND + power), and strict
 * about everything that must survive.
 *
 * Invariants (on a seeded, developed, 200-tick city, power rescanned):
 *  - byte count is exactly 27120, no header;
 *  - load fixpoint: save(load(save(load(save(A))))) == save(load(save(A)))
 *    byte-for-byte (i.e. s2 == s3);
 *  - every tile's low ordinal (z & LOMASK) in A survives into B;
 *  - funds, resPop/comPop/indPop, cityTime, cityTax, gameLevel,
 *    evaluation.cityClass/cityScore and all six history arrays are equal in A, B.
 */
package micropolisj.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

public class CityFileV1RoundTripTest
{
	private static byte[] save(Micropolis c) throws IOException
	{
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		CityWriterV1.write(c, b);
		return b.toByteArray();
	}

	private static Micropolis load(byte[] bytes) throws IOException
	{
		Micropolis c = new Micropolis();
		c.load_v1(new ByteArrayInputStream(bytes));
		return c;
	}

	private static Micropolis developedCity()
	{
		Micropolis a = NietoDemo.newDemoCity();
		for (int i = 0; i < 200; i++) {
			NietoDemo.tick(a);
		}
		a.checkPowerMap();
		return a;
	}

	@Test
	public void byteCountIs27120() throws IOException
	{
		byte[] s = save(developedCity());
		assertEquals(27120, CityWriterV1.FILE_SIZE);
		assertEquals("v1 .cty must be exactly 27120 bytes", 27120, s.length);
	}

	@Test
	public void goldenRoundTrip() throws IOException
	{
		Micropolis a = developedCity();

		byte[] s1 = save(a);
		Micropolis b = load(s1);
		byte[] s2 = save(b);
		Micropolis c = load(s2);
		byte[] s3 = save(c);

		// Load fixpoint: once through the reader, re-saving is byte-stable.
		assertArrayEquals("save must reach a byte-exact fixpoint after one load", s2, s3);

		// Every tile's low ordinal survives A -> B (getTile masks to LOMASK).
		for (int y = 0; y < a.getHeight(); y++) {
			for (int x = 0; x < a.getWidth(); x++) {
				assertEquals("tile ordinal at " + x + "," + y, a.getTile(x, y), b.getTile(x, y));
			}
		}

		// Core state survives A -> B.
		assertEquals("funds", a.budget.totalFunds, b.budget.totalFunds);
		assertEquals("resPop", a.resPop, b.resPop);
		assertEquals("comPop", a.comPop, b.comPop);
		assertEquals("indPop", a.indPop, b.indPop);
		assertEquals("cityTime", a.cityTime, b.cityTime);
		assertEquals("cityTax", a.cityTax, b.cityTax);
		assertEquals("gameLevel", a.gameLevel, b.gameLevel);
		assertEquals("cityClass", a.evaluation.cityClass, b.evaluation.cityClass);
		assertEquals("cityScore", a.evaluation.cityScore, b.evaluation.cityScore);

		assertArrayEquals("res history", a.history.res, b.history.res);
		assertArrayEquals("com history", a.history.com, b.history.com);
		assertArrayEquals("ind history", a.history.ind, b.history.ind);
		assertArrayEquals("crime history", a.history.crime, b.history.crime);
		assertArrayEquals("pollution history", a.history.pollution, b.history.pollution);
		assertArrayEquals("money history", a.history.money, b.history.money);
	}
}
