/*
 * NietoCity - zone query report.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * The same zone query the original shows: a header and label/value pairs for the
 * zone name, density, land value, crime, pollution and growth. Built from a
 * ZoneStatus so both platforms display identical text. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

import micropolisj.engine.ZoneStatus;

public final class QueryReport
{
	public final String header;
	public final String[] labels;
	public final String[] values;

	private QueryReport(String header, String[] labels, String[] values)
	{
		this.header = header;
		this.labels = labels;
		this.values = values;
	}

	public static QueryReport of(ZoneStatus z)
	{
		String header = GameStrings.gui("notification.query_hdr");
		String[] labels = {
			GameStrings.gui("notification.zone_lbl"),
			GameStrings.gui("notification.density_lbl"),
			GameStrings.gui("notification.value_lbl"),
			GameStrings.gui("notification.crime_lbl"),
			GameStrings.gui("notification.pollution_lbl"),
			GameStrings.gui("notification.growth_lbl"),
		};
		String[] values = {
			z.building != -1 ? GameStrings.zoneName(z.building) : "",
			GameStrings.statusLine(z.popDensity),
			GameStrings.statusLine(z.landValue),
			GameStrings.statusLine(z.crimeLevel),
			GameStrings.statusLine(z.pollution),
			GameStrings.statusLine(z.growthRate),
		};
		return new QueryReport(header, labels, values);
	}
}
