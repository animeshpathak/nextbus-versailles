package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

public class ResponseStats {
	private long queryTime = System.currentTimeMillis();
	private long responseMs;
	private long parsingMs;
	private BusLine busLine;
	private BusStop busStop;
	private boolean isValid = false;

	public long getQueryTime() {
		return queryTime;
	}

	public void setQueryTime(long queryTime) {
		this.queryTime = queryTime;
	}

	public long getResponseMs() {
		return responseMs;
	}

	public void setResponseMs(long responseMs) {
		this.responseMs = responseMs;
	}

	public long getParsingMs() {
		return parsingMs;
	}

	public void setParsingMs(long parsingMs) {
		this.parsingMs = parsingMs;
	}
	
	public BusLine getBusLine() {
		return busLine;
	}

	public void setBusLine(BusLine busLine) {
		this.busLine = busLine;
	}

	public BusStop getBusStop() {
		return busStop;
	}

	public void setBusStop(BusStop busStop) {
		this.busStop = busStop;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
}
