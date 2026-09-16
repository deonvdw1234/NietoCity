/*
 * NietoCity - immutable status snapshot.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * A consistent, read-only view of the values the status bar shows, captured under
 * the engine lock so the UI never reads a half-updated engine. Pure Java 8.
 */
package za.co.nieto.nietocity.game;

public final class StatusSnapshot
{
	public final String date;      // formatted game date, e.g. "Jan 1900"
	public final int funds;        // raw funds
	public final String fundsText; // formatted funds
	public final int population;   // city population
	public final String toolName;  // selected tool display name ("" if none)
	public final int toolCost;     // selected tool cost (0 if none)

	public StatusSnapshot(String date, int funds, String fundsText,
		int population, String toolName, int toolCost)
	{
		this.date = date;
		this.funds = funds;
		this.fundsText = fundsText;
		this.population = population;
		this.toolName = toolName;
		this.toolCost = toolCost;
	}
}
