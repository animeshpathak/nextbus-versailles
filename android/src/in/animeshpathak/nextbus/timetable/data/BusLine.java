package in.animeshpathak.nextbus.timetable.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class BusLine implements Comparable<BusLine>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 235084322448996426L;

	/**
	 * Maximum edit-distance to consider that two bus stops have identical names
	 */
	public static final int MAX_STOPNAME_EDIT_DISTANCE = 2;

	/** The name of the line (e.g., RATP 171) */
	private String name;

	/** The code of the line (e.g., B171) */
	private String code;

	/** Ordered set of bus-stops of this line, filtered by stop name */
	private Set<BusStop> stops = new TreeSet<BusStop>(
			new Comparator<BusStop>() {
				@Override
				public int compare(BusStop lhs, BusStop rhs) {
					int edDist = computeEditDistance(lhs.getName(),
							rhs.getName());

					if (edDist <= MAX_STOPNAME_EDIT_DISTANCE) {
						// Considering edit distance 2 as identical stops
						return 0;
					} else {
						return lhs.getName().compareTo(rhs.getName());
					}
				}
			});

	public BusLine(String name, String code, Collection<BusStop> stops) {
		this.name = name;
		this.code = code;
		this.stops.addAll(stops);
	}

	/**
	 * @return Returns the name of the line
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the code of the line, as required by its web-service
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return Returns an ordered list of bus stop references
	 */
	public List<BusStop> getStops() {
		return new ArrayList<BusStop>(stops);
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int compareTo(BusLine another) {
		return this.getName().compareTo(another.getName());
	}

	/**
	 * Finds if a stop with a specific name exists on the line. An Max edit
	 * distance allows finding stops with small errors in their name (e.g.,
	 * "Gabriel Peri" vs. "Gabriel P\u00e9ri" with accent)
	 * 
	 * @param stopName
	 * @return
	 */
	public BusStop getFirstStopWithSimilarName(String stopName) {
		Collection<BusStop> stops = getStops();
		for (BusStop busStop : stops) {
			int edDist = computeEditDistance(stopName, busStop.getName());
			if (edDist <= MAX_STOPNAME_EDIT_DISTANCE)
				return busStop;
		}
		return null;
	}

	/**
	 * Computes the Levenshtein distance between the two string parameters
	 * Function obtained from:
	 * http://rosettacode.org/wiki/Levenshtein_distance#Java
	 * 
	 * @param s1
	 * @param s2
	 * @return The edit distance.
	 */
	public static int computeEditDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue),
									costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}
}
