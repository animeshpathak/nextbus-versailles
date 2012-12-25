package in.animeshpathak.nextbus.timetable.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BusLine implements Comparable<BusLine> {
	/** The name of the line (e.g., RATP 171) */
	private String name;
	
	/** The code of the line (e.g., B171) */
	private String code;
	
	/** Ordered list of bus-stops of this line */
	private Map<String, BusStop> stops = new TreeMap<String, BusStop>();
	
	public BusLine(String name, String code, Map<String, BusStop> stops) {
		this.name = name;
		this.code = code;
		if(stops != null)
			this.stops.putAll(stops);
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
		List<BusStop> orderedStops = new ArrayList<BusStop>(stops.values());
		Collections.sort(orderedStops);
		return orderedStops;
	}
	
	@Override
	public String toString(){
		return this.name;
	}

	@Override
	public int compareTo(BusLine another) {
		return this.getName().compareTo(another.getName());
	}
}
