package in.animeshpathak.nextbus.analytics;

import in.animeshpathak.nextbus.timetable.ResponseStats;

import java.util.List;

import android.content.Context;

public class Analytics {
	private static Analytics instance;

	private Analytics() {
	}

	public static Analytics getInstance() {
		if (null == instance)
			instance = new Analytics();
		return instance;
	}

	public void setContext(Context ctx) {
	}

	public void busArrivalQuery(ResponseStats stats) {
	}

	public void notifServiceQuery(ResponseStats stats) {
	}

	/**
	 * Send the stored analytics.
	 * 
	 * @param theStats
	 * @param queryType
	 */
	protected void send(List<ResponseStats> theStats, String queryType) {
	}

	/**
	 * Request that all data be pushed using a separate Service
	 */
	public void onPush() {
	}

	protected void pushInternal() {
	}
}
