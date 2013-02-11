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
	 * Maximum Hamming distance to consider that two bus stops have identical names
	 */
	public static final int MAX_STOPNAME_ERROR = 2;

	/** The name of the line (e.g., RATP 171) */
	private String name;

	/** The code of the line (e.g., B171) */
	private String code;

	/** Ordered set of bus-stops of this line, filtered by stop name */
	private Set<BusStop> stops = new TreeSet<BusStop>(
			new Comparator<BusStop>() {
				@Override
				public int compare(BusStop lhs, BusStop rhs) {
					int edDist = computeHammingDistance(lhs.getName(),
							rhs.getName());

					if (edDist <= MAX_STOPNAME_ERROR) {
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
			int edDist = computeHammingDistance(stopName, busStop.getName());
			if (edDist <= MAX_STOPNAME_ERROR)
				return busStop;
		}
		return null;
	}

	public static int computeHammingDistance(String s1, String s2) {
		if (s1.length() != s2.length())
			return Integer.MAX_VALUE;
		int errors = 0;
		for (int k = 0; k < s1.length(); ++k) {
			if (s1.charAt(k) != s2.charAt(k))
				errors++;
		}
		return errors;
	}

}
