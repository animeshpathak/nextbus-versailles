package in.animeshpathak.nextbus.timetable.data;

import java.io.Serializable;

public class BusStop implements Comparable<BusStop>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4871584377720906881L;

	/** The name of the bus stop(e.g., Europe) */
	private String name;

	/** The code of the bus stop (e.g., EUROP) */
	private String code;

	public BusStop(String name, String code) {
		this.name = name;
		this.code = code;
	}

	/**
	 * @return Returns the name of the bus stop
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the code of the bus stop, as required by its web-service
	 */
	public String getCode() {
		return code;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int compareTo(BusStop another) {
		return this.getCode().compareTo(another.getCode());
	}
}
