// NietoCity - classic binary .cty (v1) writer.
// Created by Nieto Software. Licensed under the GNU General Public License v3.0.
//
// The engine only READS classic binary .cty (Micropolis.load_v1); it has no v1
// writer. This adds one, byte-for-byte compatible with that reader, so a saved
// city re-loads through the engine's own code (our independent check) and
// interchanges with the classic SimCity/Micropolis .cty format. It lives in the
// micropolisj.engine package so it can read the same package-private fields the
// reader writes; it does not modify any upstream file. Pure Java; no Android/JavaFX.
//
// Layout (big-endian, exactly 27120 bytes, no header): six 240-short history
// arrays (res, com, ind, crime, pollution, money), then 120 misc shorts in
// loadMisc_v1 order, then a DEFAULT_WIDTH x DEFAULT_HEIGHT column-major map of the
// full 16-bit tile chars. See CHANGES.md for the field-by-field layout.

package micropolisj.engine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class CityWriterV1
{
	/** The exact size of a classic binary v1 .cty stream. */
	public static final int FILE_SIZE = 27120;

	private CityWriterV1() { }

	/** Write the city as a classic binary v1 .cty stream. */
	public static void write(Micropolis city, OutputStream out) throws IOException
	{
		DataOutputStream d = new DataOutputStream(out);
		writeHistory(d, city.history.res);
		writeHistory(d, city.history.com);
		writeHistory(d, city.history.ind);
		writeHistory(d, city.history.crime);
		writeHistory(d, city.history.pollution);
		writeHistory(d, city.history.money);
		writeMisc(d, city);
		writeMap(d, city);
		d.flush();
	}

	private static void writeHistory(DataOutputStream d, int[] array) throws IOException
	{
		for (int i = 0; i < 240; i++) {
			d.writeShort(array[i]);
		}
	}

	private static void writeMisc(DataOutputStream d, Micropolis c) throws IOException
	{
		d.writeShort(0);                  // [0]  unused
		d.writeShort(0);                  // [1]  externalMarket (unused)
		d.writeShort(c.resPop);           // [2]
		d.writeShort(c.comPop);           // [3]
		d.writeShort(c.indPop);           // [4]
		d.writeShort(c.resValve);         // [5]
		d.writeShort(c.comValve);         // [6]
		d.writeShort(c.indValve);         // [7]
		d.writeInt(c.cityTime);           // [8-9]
		d.writeShort(c.crimeRamp);        // [10]
		d.writeShort(c.polluteRamp);      // [11]
		d.writeShort(c.landValueAverage); // [12]
		d.writeShort(c.crimeAverage);     // [13]
		d.writeShort(c.pollutionAverage); // [14]
		d.writeShort(c.gameLevel);        // [15]
		d.writeShort(c.evaluation.cityClass); // [16]
		d.writeShort(c.evaluation.cityScore); // [17]
		for (int i = 18; i < 50; i++) {   // [18-49] unused
			d.writeShort(0);
		}
		d.writeInt(c.budget.totalFunds);  // [50-51]
		d.writeShort(c.autoBulldoze ? 1 : 0); // [52]
		d.writeShort(c.autoBudget ? 1 : 0);   // [53]
		d.writeShort(c.autoGo ? 1 : 0);       // [54]
		d.writeShort(0);                  // [55] userSoundOn (unused)
		d.writeShort(c.cityTax);          // [56]
		d.writeShort(c.simSpeed.ordinal());   // [57]
		d.writeInt((int) Math.round(c.policePercent * 65536.0)); // [58-59]
		d.writeInt((int) Math.round(c.firePercent * 65536.0));   // [60-61]
		d.writeInt((int) Math.round(c.roadPercent * 65536.0));   // [62-63]
		for (int i = 64; i < 120; i++) {  // [64-119] unused
			d.writeShort(0);
		}
	}

	private static void writeMap(DataOutputStream d, Micropolis c) throws IOException
	{
		// Column-major: x outer, y inner, matching loadMap_v1.
		for (int x = 0; x < Micropolis.DEFAULT_WIDTH; x++) {
			for (int y = 0; y < Micropolis.DEFAULT_HEIGHT; y++) {
				d.writeShort(c.map[y][x]);
			}
		}
	}
}
