package in.animeshpathak.nextbus.analytics;

import in.animeshpathak.nextbus.ConfidentialConstants;
import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.R;
import in.animeshpathak.nextbus.timetable.ResponseStats;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Analytics {

	private static final String USER_QUERY = "busArrivalQuery";
	private static final String NOTIF_QUERY = "notifServiceQuery";
	private static Analytics instance;

	private Context context;
	private static final String LOG_TAG = Constants.LOG_TAG;

	private List<ResponseStats> respStats = new ArrayList<ResponseStats>();
	private List<ResponseStats> notifStats = new ArrayList<ResponseStats>();

	private Analytics() {
	}

	public static Analytics getInstance() {
		if (null == instance)
			instance = new Analytics();
		return instance;
	}

	public void setContext(Context ctx) {
		this.context = ctx;
	}

	public void busArrivalQuery(ResponseStats stats) {
		synchronized (respStats) {
			respStats.add(stats);
		}
	}

	public void notifServiceQuery(ResponseStats stats) {
		synchronized (notifStats) {
			notifStats.add(stats);
		}
	}

	/**
	 * Send the stored analytics.
	 * 
	 * @param theStats
	 * @param queryType
	 */
	protected void send(List<ResponseStats> theStats, String queryType) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost;

		try {
			String getUri = String.format("%s?tok=%s&z=%s", ConfidentialConstants.ANALYTICS_URI,
					ConfidentialConstants.ANALYTICS_TOKEN, queryType);
			httppost = new HttpPost(getUri);
			StringBuffer strBuf = new StringBuffer();
			long sendTime = System.currentTimeMillis();
			if (theStats.isEmpty())
				return;
			synchronized (theStats) {
				for (ResponseStats stats : theStats) {
					strBuf.append(stats.getBusLine().getCode() + ",\t");
					strBuf.append(stats.getBusStop().getCode() + ",\t");
					strBuf.append(stats.getBusStop().getName() + ",\t");
					strBuf.append(stats.isValid() + ",\t");
					strBuf.append(stats.getResponseMs() + ",\t");
					strBuf.append(stats.getParsingMs() + ",\t");
					strBuf.append(stats.getQueryTime() + ",\t");
					strBuf.append(sendTime + "\r\n");
				}
				theStats.clear();
			}
			httppost.setEntity(new StringEntity(strBuf.toString()));
			httpclient.execute(httppost);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error (\"" + e.getMessage() + "\").", e);
		}
	}

	/**
	 * Request that all data be pushed using a separate Service
	 */
	public void onPush() {
		SharedPreferences sharedPrefs = null;
		try{
			// Android seems to throw NullPointerException here
			sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		} catch (NullPointerException e){
			Log.e(LOG_TAG, "Error getting preferences: " + e.getMessage());
			return;
		}
		
		boolean analyticsEanbled = sharedPrefs.getBoolean("analytics_checkbox",
				false);

		if (!analyticsEanbled) {
			respStats.clear();
			notifStats.clear();
			return;
		}

		Intent pushService = new Intent(context, AnalyticsService.class);
		context.startService(pushService);
	}

	protected void pushInternal() {
		send(respStats, USER_QUERY);
		send(notifStats, NOTIF_QUERY);
	}
}
