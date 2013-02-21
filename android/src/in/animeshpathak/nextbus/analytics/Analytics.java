package in.animeshpathak.nextbus.analytics;

import in.animeshpathak.nextbus.timetable.ResponseStats;

public class Analytics {
	public static final int ANA_BUS_LINE_NAME = 1;
	public static final int ANA_BUS_STOP_NAME = 2;
	public static final int ANA_BUS_LINE_ID = 3;
	public static final int ANA_BUS_STOP_ID = 4;
	public static final int ANA_QUERY_HOUR = 5;
	public static final int ANA_QUERY_MINUTE = 6;

	public static final int ANA_METRIC_QUERY_HIT = 1;
	public static final int ANA_METRIC_RESPONSE_TIME = 2;
	public static final int ANA_METRIC_PARSE_TIME = 3;
	public static final int ANA_METRIC_VALID_RESPONSE = 4;

	public static void busArrivalQuery(ResponseStats stats) {
		// Adding BusArrivalQuery Custom Dimension Analytics
		// Tracker tr = EasyTracker.getTracker();
		// tr.setCustomMetric(Constants.ANA_METRIC_BUSQUERYHIT, (long) 1);
		// tr.setCustomMetric(1, (long) 1);
		// Map<Integer, String> parDim = new TreeMap<Integer, String>();
		// parDim.put(ANA_BUS_LINE_NAME, stats.getBusLine().getName());
		// parDim.put(ANA_BUS_STOP_NAME, stats.getBusStop().getName());
		// parDim.put(ANA_BUS_LINE_ID, stats.getBusLine().getCode());
		// parDim.put(ANA_BUS_STOP_ID, stats.getBusStop().getCode());
		// Calendar cal = Calendar.getInstance();
		// parDim.put(ANA_QUERY_HOUR, ""+cal.get(Calendar.HOUR_OF_DAY));
		// parDim.put(ANA_QUERY_MINUTE, ""+cal.get(Calendar.MINUTE));
		// Map<Integer, Long> parMet = new TreeMap<Integer, Long>();
		// parMet.put(ANA_METRIC_QUERY_HIT, 1L);
		// parMet.put(ANA_METRIC_RESPONSE_TIME, stats.getResponseTime());
		// parMet.put(ANA_METRIC_PARSE_TIME, stats.getParsingTime());
		// parMet.put(ANA_METRIC_VALID_RESPONSE, stats.isValid()?1L:0L);
		// tr.setCustomDimensionsAndMetrics(parDim, parMet);
		// tr.sendEvent("BusArrival", "Query", stats.getBusLine().getCompany(),
		// (long) 0);
		//
	}
}
