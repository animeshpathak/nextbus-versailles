package in.animeshpathak.nextbus.timetable.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BusLine implements Comparable<BusLine>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 235084322448996426L;

	/** The name of the line (e.g., RATP 171) */
	private String name;

	/** The code of the line (e.g., B171) */
	private String code;

	/** Ordered list of bus-stops of this line */
	private Collection<BusStop> stops = new ArrayList<BusStop>();

	public BusLine(String name, String code, Collection<BusStop> stops) {
		this.name = name;
		this.code = code;
		this.stops.addAll(stops);
	}

	/**
	 * Avoid having multiple stops with the same name or similar.
	 * Not very efficient due to the use of the edit distance function
	 * (e.g., "Gabriel Peri" vs. "Gabriel P\u00e9ri" with accent)
	 * 
	 * @param theStops
	 */
	private void addUniqueStops(Collection<BusStop> theStops) {
		for (BusStop busStop : theStops) {
			boolean stopExists = false;
			for (BusStop busStop2 : this.stops) {
				int edDist = computeEditDistance(busStop2.getName(), busStop.getName());
				if(edDist <= 1)
					stopExists = true;
			}
			
			if(!stopExists)
				this.stops.add(busStop);
		}
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
		List<BusStop> orderedStops = new ArrayList<BusStop>(stops);
		Collections.sort(orderedStops);
		return orderedStops;
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
	 * Finds if a stop with a specific name exists on the line.
	 * An Max edit distance allows finding stops with small errors in their name
	 * (e.g., "Gabriel Peri" vs. "Gabriel P\u00e9ri" with accent)
	 * 
	 * @param stopName
	 * @return
	 */
	public BusStop getFirstStopWithName(String stopName, int maxEditDistance) {
		Collection<BusStop> stops = getStops();
		for (BusStop busStop : stops) {
			int edDist = computeEditDistance(stopName, busStop.getName());
			if(edDist <= maxEditDistance)
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
