/*
 * NietoCity - shared educational registry ("Explain" mode).
 * Created by Nieto Software. Licensed under the GNU General Public License v3.0.
 *
 * One place, used by both platforms, that maps every tool, map overlay and city
 * problem - plus the power / road-access / demand basics - to a short, plain-
 * language card: what it is and the REAL Micropolis engine mechanic behind it.
 * The wording is deliberately accurate to how the engine behaves (zones need
 * power, road access and positive demand to grow; industry and traffic raise
 * pollution, which lowers land value; crime rises where land value is low and
 * density high; and so on). Pure Java 8; no awt/android.
 */
package za.co.nieto.nietocity.game;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import micropolisj.engine.CityProblem;
import micropolisj.engine.MicropolisTool;
import za.co.nieto.nietocity.render.MapOverlay;

public final class Education
{
	/** A short explanatory card: a title and a plain-language body. */
	public static final class Card
	{
		public final String title;
		public final String body;

		public Card(String title, String body)
		{
			this.title = title;
			this.body = body;
		}
	}

	private Education() { }

	private static final Map<MicropolisTool, Card> TOOLS = new EnumMap<MicropolisTool, Card>(MicropolisTool.class);
	private static final Map<MapOverlay, Card> OVERLAYS = new EnumMap<MapOverlay, Card>(MapOverlay.class);
	private static final Map<CityProblem, Card> PROBLEMS = new EnumMap<CityProblem, Card>(CityProblem.class);

	/** The three "how it works" basics, also shown on the How-to-play screen. */
	public static final Card POWER = new Card("Power",
		"Every zone needs electricity to develop. Build a power plant and connect "
		+ "zones to it - a zone that touches a powered tile is powered, and power "
		+ "lines carry electricity across gaps. An unpowered zone shows a lightning "
		+ "bolt and will not grow.");
	public static final Card ROAD_ACCESS = new Card("Road access",
		"A zone needs a road or rail within about three tiles, or it will not "
		+ "develop. Connect every zone into your transport network.");
	public static final Card DEMAND = new Card("Demand (RCI)",
		"Your city has separate demand for Residential, Commercial and Industrial "
		+ "zones. A zone only grows when its demand is positive and it has power and "
		+ "road access. Balancing homes, shops and jobs keeps the city growing.");

	/** Tool display order for the reference screen. */
	private static final MicropolisTool[] TOOL_ORDER = {
		MicropolisTool.BULLDOZER, MicropolisTool.WIRE, MicropolisTool.ROADS, MicropolisTool.RAIL,
		MicropolisTool.PARK, MicropolisTool.RESIDENTIAL, MicropolisTool.COMMERCIAL,
		MicropolisTool.INDUSTRIAL, MicropolisTool.POLICE, MicropolisTool.FIRE,
		MicropolisTool.POWERPLANT, MicropolisTool.NUCLEAR, MicropolisTool.STADIUM,
		MicropolisTool.SEAPORT, MicropolisTool.AIRPORT, MicropolisTool.QUERY,
	};

	private static final MapOverlay[] OVERLAY_ORDER = {
		MapOverlay.POPULATION, MapOverlay.POLLUTION, MapOverlay.CRIME, MapOverlay.LAND_VALUE,
		MapOverlay.TRAFFIC, MapOverlay.POWER, MapOverlay.FIRE, MapOverlay.POLICE,
	};

	static {
		// --- tools (all 16) ---
		TOOLS.put(MicropolisTool.BULLDOZER, new Card("Bulldozer",
			"Clears terrain, trees, rubble and buildings one tile at a time. Clear a "
			+ "tile before building something new on it, and use it to tidy up after "
			+ "a fire or other disaster."));
		TOOLS.put(MicropolisTool.WIRE, new Card("Power lines",
			"Carry electricity from a power plant across the map. A zone only develops "
			+ "when it is connected to the grid - by touching a powered tile or by "
			+ "wires. Lines can cross water and other tiles at a higher cost."));
		TOOLS.put(MicropolisTool.ROADS, new Card("Road",
			"Roads give zones the access they need to grow: a zone with no road within "
			+ "about three tiles will not develop. Traffic flows along roads, and heavy "
			+ "traffic adds pollution, so spread the load across a network."));
		TOOLS.put(MicropolisTool.RAIL, new Card("Rail",
			"Rail carries commuters without adding road traffic, so it eases congestion "
			+ "and the pollution heavy traffic brings. Like roads it gives zones access, "
			+ "but it costs more to build."));
		TOOLS.put(MicropolisTool.PARK, new Card("Park",
			"Raises land value on the tiles around it, which encourages development and "
			+ "helps hold back crime. Parks add no pollution."));
		TOOLS.put(MicropolisTool.RESIDENTIAL, new Card("Residential zone",
			"Homes for your citizens. It grows when residential demand (the R bar) is "
			+ "positive and the zone has power and road access. More homes mean more "
			+ "people - and more demand for jobs and services."));
		TOOLS.put(MicropolisTool.COMMERCIAL, new Card("Commercial zone",
			"Shops and offices, the source of commercial jobs. It grows when the C "
			+ "demand bar is positive and the zone has power and road access. An airport "
			+ "boosts commercial demand once the city is large enough."));
		TOOLS.put(MicropolisTool.INDUSTRIAL, new Card("Industrial zone",
			"Factories, the source of industrial jobs. It grows when the I demand bar is "
			+ "positive and the zone has power and road access. Industry and its traffic "
			+ "create pollution, which lowers nearby land value, so keep it away from "
			+ "homes. A seaport boosts industrial demand."));
		TOOLS.put(MicropolisTool.POLICE, new Card("Police station",
			"Reduces crime in the area around it. Its reach and strength depend on how "
			+ "well you fund police in the budget. Crime is worst where land value is "
			+ "low and density is high. Placed once, then the tool returns to Pan."));
		TOOLS.put(MicropolisTool.FIRE, new Card("Fire station",
			"Lowers fire risk and helps put out fires around it. Its reach and strength "
			+ "depend on how well you fund fire services in the budget. Placed once, "
			+ "then the tool returns to Pan."));
		TOOLS.put(MicropolisTool.POWERPLANT, new Card("Coal power plant",
			"Supplies power to zones connected to it by adjacency or wires. It is "
			+ "cheaper than nuclear but pollutes the area around it. Placed once, then "
			+ "the tool returns to Pan."));
		TOOLS.put(MicropolisTool.NUCLEAR, new Card("Nuclear power plant",
			"Supplies a lot of power with almost no pollution, but it carries the risk "
			+ "of a meltdown that contaminates the land. Placed once, then the tool "
			+ "returns to Pan."));
		TOOLS.put(MicropolisTool.STADIUM, new Card("Stadium",
			"A stadium raises residential demand once your city is large enough to need "
			+ "one. It needs power and road access. Placed once, then the tool returns "
			+ "to Pan."));
		TOOLS.put(MicropolisTool.SEAPORT, new Card("Seaport",
			"A seaport raises industrial demand once the city is large enough. It works "
			+ "best next to water and needs power and road access. Placed once, then the "
			+ "tool returns to Pan."));
		TOOLS.put(MicropolisTool.AIRPORT, new Card("Airport",
			"An airport raises commercial demand once the city is large enough. It is "
			+ "large and expensive, and brings a small risk of a plane crash. Placed "
			+ "once, then the tool returns to Pan."));
		TOOLS.put(MicropolisTool.QUERY, new Card("Query",
			"Tap and hold a tile to read it: its zone or terrain type, population "
			+ "density, land value, and crime and pollution levels, plus how the zone is "
			+ "growing. Use it to see why a part of your city is thriving or struggling."));

		// --- overlays (all 8) ---
		OVERLAYS.put(MapOverlay.POPULATION, new Card("Population density",
			"Shows how many people live in each part of the city. Density rises as "
			+ "residential zones develop; high density brings more life but also more "
			+ "traffic and crime."));
		OVERLAYS.put(MapOverlay.POLLUTION, new Card("Pollution",
			"Shows where the air is dirtiest. Pollution comes mainly from industry, "
			+ "traffic and coal power. It lowers land value and makes zones less "
			+ "desirable, so keep dirty industry away from homes."));
		OVERLAYS.put(MapOverlay.CRIME, new Card("Crime",
			"Shows where crime is worst. Crime rises with high population density and "
			+ "low land value, and falls within the reach of a well-funded police "
			+ "station."));
		OVERLAYS.put(MapOverlay.LAND_VALUE, new Card("Land value",
			"Shows how desirable each area is. It is higher near the city centre, near "
			+ "water and parks, and where crime and pollution are low. High land value "
			+ "drives development and tax income."));
		OVERLAYS.put(MapOverlay.TRAFFIC, new Card("Traffic",
			"Shows how busy the roads and rails are. Traffic grows as zones develop and "
			+ "commuters travel between homes and jobs. Heavy traffic adds pollution; "
			+ "rail and a well-spread road network keep it down."));
		OVERLAYS.put(MapOverlay.POWER, new Card("Power grid",
			"Shows which zones are powered: powered zone centres in red, unpowered ones "
			+ "in blue, and bare power lines in grey. A zone with no power will not "
			+ "develop and shows a lightning bolt on the map."));
		OVERLAYS.put(MapOverlay.FIRE, new Card("Fire coverage",
			"Shows the area your fire stations protect. Better funding in the budget "
			+ "widens the coverage; areas outside it are more likely to lose buildings "
			+ "to fire."));
		OVERLAYS.put(MapOverlay.POLICE, new Card("Police coverage",
			"Shows the area your police stations protect. Better funding in the budget "
			+ "widens the coverage; crime is highest outside it."));

		// --- city problems (all 7) ---
		PROBLEMS.put(CityProblem.CRIME, new Card("Crime",
			"Citizens are worried about crime. Build police stations near the trouble "
			+ "and fund them well, and raise land value with parks and development - "
			+ "crime thrives where land value is low."));
		PROBLEMS.put(CityProblem.POLLUTION, new Card("Pollution",
			"The air is too dirty. It comes from industry, traffic and coal power. Move "
			+ "dirty industry away from homes, lean on rail to cut traffic, and consider "
			+ "cleaner nuclear power."));
		PROBLEMS.put(CityProblem.HOUSING, new Card("Housing costs",
			"Homes are too expensive because there are not enough of them for the "
			+ "demand. Zone more residential area, with power and road access, to give "
			+ "people places to live."));
		PROBLEMS.put(CityProblem.TAXES, new Card("Taxes",
			"Citizens feel the tax rate is too high. Lower it in the budget: a lower "
			+ "rate encourages growth, though it also reduces your income."));
		PROBLEMS.put(CityProblem.TRAFFIC, new Card("Traffic",
			"The roads are too congested. Add roads to spread the load, and build rail "
			+ "to move commuters without adding road traffic."));
		PROBLEMS.put(CityProblem.UNEMPLOYMENT, new Card("Unemployment",
			"There are not enough jobs for your residents. Zone more commercial and "
			+ "industrial area so people can find work; a seaport or airport can boost "
			+ "the demand for jobs."));
		PROBLEMS.put(CityProblem.FIRE, new Card("Fire",
			"The city is too exposed to fire. Build fire stations to cover built-up "
			+ "areas and fund fire services in the budget."));
	}

	public static Card forTool(MicropolisTool tool) { return tool == null ? null : TOOLS.get(tool); }
	public static Card forOverlay(MapOverlay overlay) { return overlay == null ? null : OVERLAYS.get(overlay); }
	public static Card forProblem(CityProblem problem) { return problem == null ? null : PROBLEMS.get(problem); }

	/** Unmodifiable views, for completeness tests. */
	public static Map<MicropolisTool, Card> tools() { return Collections.unmodifiableMap(TOOLS); }
	public static Map<MapOverlay, Card> overlays() { return Collections.unmodifiableMap(OVERLAYS); }
	public static Map<CityProblem, Card> problems() { return Collections.unmodifiableMap(PROBLEMS); }

	/**
	 * The whole reference as one formatted block, for the How-to-play / Educational
	 * screen (both platforms render the same text from this registry).
	 */
	public static String fullReference()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Grow a city from open land. Lay roads and power, then zone land for "
			+ "homes (R), shops (C) and industry (I). Zones grow when they have power, "
			+ "road access and demand. Watch the map overlays and your citizens' "
			+ "complaints to see what to fix next.\n\n");
		sb.append("Turn on \"Explain\" in the game menu to get a short card like these "
			+ "whenever you pick a tool, choose an overlay or query a tile.\n");

		section(sb, "THE BASICS");
		card(sb, POWER);
		card(sb, ROAD_ACCESS);
		card(sb, DEMAND);

		section(sb, "TOOLS");
		for (MicropolisTool t : TOOL_ORDER) {
			card(sb, TOOLS.get(t));
		}

		section(sb, "MAP OVERLAYS");
		for (MapOverlay o : OVERLAY_ORDER) {
			card(sb, OVERLAYS.get(o));
		}

		section(sb, "WHAT CITIZENS COMPLAIN ABOUT");
		for (CityProblem p : CityProblem.values()) {
			card(sb, PROBLEMS.get(p));
		}
		return sb.toString();
	}

	private static void section(StringBuilder sb, String title)
	{
		sb.append('\n').append(title).append('\n');
	}

	private static void card(StringBuilder sb, Card c)
	{
		if (c != null) {
			sb.append('\n').append(c.title).append('\n').append(c.body).append('\n');
		}
	}
}
