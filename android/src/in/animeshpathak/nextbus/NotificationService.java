package in.animeshpathak.nextbus;

import in.animeshpathak.nextbus.analytics.Analytics;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery.BusArrivalInfo;

import java.util.HashMap;
import java.util.Map;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.util.Log;

public class NotificationService extends IntentService implements Runnable {

	private static String LOG_TAG = Constants.LOG_TAG;

	private BusArrivalQuery query;
	private BusArrivalInfo arrival;
	private boolean isRunning;
	private static HashMap<String, NotificationService> notifications = new HashMap<String, NotificationService>();
	private static HashMap<String, BusArrivalQuery> queries = new HashMap<String, BusArrivalQuery>();

	// LineStopDirection
	private String lsd;
	private boolean updateError = false;
	private int updateFrequency = -1;

	private static int[] timeIcons = { R.drawable.bus_00, R.drawable.bus_01,
			R.drawable.bus_02, R.drawable.bus_03, R.drawable.bus_04,
			R.drawable.bus_05, R.drawable.bus_06, R.drawable.bus_07,
			R.drawable.bus_08, R.drawable.bus_09, R.drawable.bus_10,
			R.drawable.bus_11, R.drawable.bus_12, R.drawable.bus_13,
			R.drawable.bus_14, R.drawable.bus_15 };

	private long when = System.currentTimeMillis();

	public NotificationService() {
		super("NextBus-Versailles");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this.getApplicationContext());
		String updateFrequencyStr = sharedPrefs.getString(
				"notif_frequency_list", "-1");
		try {
			updateFrequency = Integer.parseInt(updateFrequencyStr);
		} catch (NumberFormatException nfe) {
			Log.e(LOG_TAG, "" + nfe.getMessage(), nfe);
		}

		if (!intent.getBooleanExtra("deletekey", false)
				&& intent.hasExtra("LineStopDirection")
				&& intent.hasExtra("direction")) {
			this.lsd = intent.getStringExtra("LineStopDirection");
			if (lsd != null) {
				this.query = queries.get(lsd);
				this.arrival = query.getNextArrivals().get(
						intent.getStringExtra("direction"));
				notifications.put(lsd, this);
				if (!query.isValid()
						|| (arrival.getMention(0) & BusArrivalInfo.MENTION_UNKNOWN) != 0) {
					this.updateError = true;
				}

				new Thread(this).start();
			}
		} else {
			String lsd = intent.getStringExtra("LineStopDirection");
			if (lsd != null) {
				NotificationService serviceToRemove = notifications.remove(lsd);
				queries.remove(lsd);
				if (serviceToRemove != null)
					serviceToRemove.terminate();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void publishNotification() {
		NotificationManager mNotifManager;
		mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notif = new Notification();

		SpannableStringBuilder ssb = new SpannableStringBuilder();
		String notifTitle;

		if (!updateError) {
			notif.icon = R.drawable.bus_icon_newer;
			int minutesLeft = arrival.getMillis(0) / 60000;
			// The arrival time is less than 16 minutes
			if (minutesLeft < 16) {
				notif.icon = timeIcons[minutesLeft];
			}
			ssb.append(query.getBusStop().getName() + " -> ");
			ssb.append(arrival.direction);
			notifTitle = query.getBusLine().getName()
					+ " "
					+ BusArrivalQuery.formatTimeDelta(this,
							arrival.getMillis(0));
		} else {
			notif.icon = R.drawable.bus_update_error;
			notifTitle = query.getBusLine().getName();
			ssb.append(getString(R.string.bus_text_unavailable));
		}

		/*
		 * Tip from Dianne Hackborn,
		 * 
		 * Bringing application to front from outside of the context of an
		 * existing activity (e.g., from a Notification).
		 * https://groups.google.com
		 * /forum/?fromgroups=#!topic/android-developers/DibTfwnfZ-s
		 * 
		 * Basically acting as the default application launcher. This avoids the
		 * duplication of activities.
		 */
		Intent notificationIntent = new Intent(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		notificationIntent.setComponent(new ComponentName(this,
				NextBusMain.class));
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notif.contentIntent = contentIntent;
		notif.when = this.when;
		Intent deleteIntent = new Intent(this, NotificationService.class);
		deleteIntent.putExtra("deletekey", true);
		deleteIntent.putExtra("LineStopDirection", lsd);
		notif.deleteIntent = PendingIntent.getService(this, lsd.hashCode(),
				deleteIntent, 0);

		notif.setLatestEventInfo(this, notifTitle, ssb.toString(),
				contentIntent);
		mNotifManager.notify(lsd.hashCode(), notif);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void run() {
		try {
			Thread.currentThread().setName("NotifUpdater_" + lsd);
			this.isRunning = true;
			while (this.isRunning) {
				publishNotification();

				try {
					if ((arrival.getMention(0) & BusArrivalInfo.MENTION_UNKNOWN) == 0)
						Thread.sleep(refreshFrequencyMillis(arrival
								.getMillis(0)));
					else
						Thread.sleep(60000);
				} catch (InterruptedException e) {

				}

				if (!this.isRunning) {
					break;
				}

				Analytics.getInstance().notifServiceQuery(query.postQuery());
				Map<String, BusArrivalInfo> resp = query.getNextArrivals();
				if (query.isValid()
						&& resp.containsKey(arrival.direction)
						&& (arrival.getMention(0) & BusArrivalInfo.MENTION_UNKNOWN) == 0) {
					arrival = resp.get(arrival.direction);
					this.updateError = false;
				} else {
					this.updateError = true;
				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "" + e.getMessage(), e);
		} finally {
			NotificationManager mNotifManager;
			mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotifManager.cancel(lsd.hashCode());
		}
	}

	private void terminate() {
		NotificationManager mNotifManager;
		mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifManager.cancel(lsd.hashCode());
		this.isRunning = false;
		Analytics.getInstance().onPush();
	}

	public static void putQuery(String lsd, BusArrivalQuery qu) {
		queries.put(lsd, qu);
	}

	public static boolean notifExists(String lsd) {
		return notifications.containsKey(lsd);
	}

	/**
	 * Return how many ms to sleep depending on next arrival. This allows to
	 * refresh frequently when there is little time left until arrival, while
	 * saving battery if the bus will arrive much later.
	 * 
	 * @param msUntilArrival
	 * @return
	 */
	public int refreshFrequencyMillis(int msUntilArrival) {
		if (updateFrequency >= 0)
			return updateFrequency;

		if (msUntilArrival < 3 * 60 * 1000) {
			return 30 * 1000; // 30s
		} else if (msUntilArrival < 15 * 60 * 1000) {
			return 60 * 1000; // 1min
		} else {
			return 5 * 60 * 1000; // 5min
		}
	}
}
