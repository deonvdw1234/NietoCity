/*
 * NietoCity - shared city-evaluation report.
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * A read-only snapshot of the engine's yearly city evaluation (CityEval), taken
 * under the engine lock, formatted for the evaluation dialog: the mayor's
 * approval, the city score and its change, the population and its change, the
 * city-size class, and the top (up to four) problems with their vote counts.
 * Pure Java 8; both platforms render the same report.
 */
package za.co.nieto.nietocity.game;

import micropolisj.engine.CityEval;
import micropolisj.engine.CityProblem;
import micropolisj.engine.Micropolis;

public final class EvaluationReport
{
	public final int approveYes;   // % approving
	public final int approveNo;    // % disapproving
	public final int score;        // city score 0..1000
	public final int scoreDelta;   // change since last evaluation
	public final int population;   // city population
	public final int populationDelta;
	public final String cityClass; // village/town/.../megalopolis
	public final String[] problems; // top problems, "Name (votes)", may be empty

	private EvaluationReport(int approveYes, int approveNo, int score, int scoreDelta,
		int population, int populationDelta, String cityClass, String[] problems)
	{
		this.approveYes = approveYes;
		this.approveNo = approveNo;
		this.score = score;
		this.scoreDelta = scoreDelta;
		this.population = population;
		this.populationDelta = populationDelta;
		this.cityClass = cityClass;
		this.problems = problems;
	}

	/** Read and format the current evaluation. */
	public static EvaluationReport of(Micropolis city)
	{
		synchronized (city) {
			CityEval e = city.evaluation;
			CityProblem[] order = e.problemOrder;
			int n = Math.min(4, order != null ? order.length : 0);
			String[] probs = new String[n];
			for (int i = 0; i < n; i++) {
				CityProblem p = order[i];
				Integer votes = e.problemVotes.get(p);
				probs[i] = GameStrings.problemName(p) + " (" + (votes != null ? votes : 0) + ")";
			}
			return new EvaluationReport(
				e.cityYes, e.cityNo,
				e.cityScore, e.deltaCityScore,
				e.cityPop, e.deltaCityPop,
				GameStrings.cityClassName(e.cityClass),
				probs);
		}
	}
}
