package in.animeshpathak.nextbus;

import in.animeshpathak.nextbus.analytics.Analytics;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery.BusArrivalInfo;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.util.Log;

public class NotificationService extends IntentService {

	private static String LOG_TAG = Constants.LOG_TAG;
	
	public static final String ACTION_NEW = "new-notif";
	public static final String ACTION_UPDATE = "update-notif";
	public static final String ACTION_DELETE = "delete-notif";
	
	public static final String EXTRA_DIRECTION = "extra-direction";
	public static final String EXTRA_LINE_STOP_DIR = "extra-line-stop-direction";
	public static final String EXTRA_WHEN_UPDATE = "extra-when-update";

	private BusArrivalQuery query;
	private BusArrivalInfo arrival;
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private static HashMap<String, NotificationService> notifications = new HashMap<String, NotificationService>();
	private static HashMap<String, BusArrivalQuery> queries = new HashMap<String, BusArrivalQuery>();

	// LineStopDirection
	private String lsd;
	private boolean updateError = false;
	private int updateFrequency = -1;

	private static int[] timeIcons = { R.drawable.bus_00_x, R.drawable.bus_01_x,
			R.drawable.bus_02_x, R.drawable.bus_03_x, R.drawable.bus_04_x,
			R.drawable.bus_05_x, R.drawable.bus_06_x, R.drawable.bus_07_x,
			R.drawable.bus_08_x, R.drawable.bus_09_x, R.drawable.bus_10_x,
			R.drawable.bus_11_x, R.drawable.bus_12_x, R.drawable.bus_13_x,
			R.drawable.bus_14_x, R.drawable.bus_15_x };

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

		if (intent.getBooleanExtra(ACTION_NEW, false)) {
			this.lsd = intent.getStringExtra(EXTRA_LINE_STOP_DIR);
			if (lsd != null) {
				this.query = queries.get(lsd);
				this.arrival = query.getNextArrivals().get(
						intent.getStringExtra(EXTRA_DIRECTION));
				notifications.put(lsd, this);
				if (!query.isValid()
						|| (arrival.getMention(0) & BusArrivalInfo.MENTION_UNKNOWN) != 0) {
					this.updateError = true;
				}

				// initially show the notification
				publishNotificationCompat();
				
				// set-up an ALARM to update the notification 
				setNextAlarm(refreshFrequencyMillis(arrival.getMillis(0)));
			}
		} else if (intent.getBooleanExtra(ACTION_UPDATE, false)) {
			this.lsd = intent.getStringExtra(EXTRA_LINE_STOP_DIR);
			this.query = queries.get(lsd);
			
			if(null == query){
				Log.w(LOG_TAG, "STOP updating: " + this.lsd);
				NotificationService serviceToRemove = notifications.remove(lsd);
				if (serviceToRemove != null)
					serviceToRemove.terminate();
				return;
			}
			
			this.arrival = query.getNextArrivals().get(
					intent.getStringExtra(EXTRA_DIRECTION));
			this.when = intent.getLongExtra(EXTRA_WHEN_UPDATE, System.currentTimeMillis());
			updateNotification();
		} else if (intent.getBooleanExtra(ACTION_DELETE, false)) {
			String lsd = intent.getStringExtra(EXTRA_LINE_STOP_DIR);
			if (lsd != null) {
				NotificationService serviceToRemove = notifications.remove(lsd);
				queries.remove(lsd);
				if (serviceToRemove != null)
					serviceToRemove.terminate();
			}
		}
	}
	
	private void publishNotificationCompat() {
		final NotificationManager mNotifManager;
		mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final NotificationCompat.Builder nBuild = new NotificationCompat.Builder(this);

		SpannableStringBuilder ssb = new SpannableStringBuilder();
		String notifTitle;

		int iconRes = R.drawable.bus_icon_newer_x;
		
		if (!updateError) {
			int minutesLeft = arrival.getMillis(0) / 60000;
			// The arrival time is less than 16 minutes
			if (minutesLeft < 16) {
				iconRes = timeIcons[minutesLeft];
			}
			ssb.append(query.getBusStop().getName() + " -> ");
			ssb.append(arrival.direction);
			notifTitle = query.getBusLine().getName()
					+ " "
					+ BusArrivalQuery.formatTimeDelta(this,
							arrival.getMillis(0));
		} else {
			iconRes =  R.drawable.bus_update_error;
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
		nBuild.setAutoCancel(false);
		nBuild.setContentIntent(contentIntent);
		nBuild.setWhen(this.when);
		Intent deleteIntent = new Intent(this, NotificationService.class);
		deleteIntent.putExtra(ACTION_DELETE, true);
		deleteIntent.putExtra(EXTRA_LINE_STOP_DIR, lsd);
		nBuild.setContentTitle(notifTitle);
		nBuild.setContentText(ssb.toString());
		nBuild.setContentIntent(contentIntent);
		nBuild.setSmallIcon(iconRes);
		nBuild.setDeleteIntent(PendingIntent.getService(this, lsd.hashCode(), deleteIntent, 0));
		mNotifManager.notify(lsd.hashCode(), nBuild.build());

//		// hack to simulate an update animation
//		for (int i = 0; i < 3; i++) {
//			nBuild.setSmallIcon(R.drawable.bus_icon_gray);
//			if(null != queries.get(lsd))
//				mNotifManager.notify(lsd.hashCode(), nBuild.build());
//			
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				
//			}
//
//			nBuild.setSmallIcon(iconRes);
//			if(null != queries.get(lsd))
//				mNotifManager.notify(lsd.hashCode(), nBuild.build());
//			
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//			}
//		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	public void updateNotification() {
		try {
				Log.w(LOG_TAG, "Updating notification: " + lsd);
			
				if(!queries.containsKey(lsd)){
					// query was removed, ignoring update
					Log.w(LOG_TAG, "Notification dismissed. No more updates for: " + lsd);
					return;
				}
			
				Thread.currentThread().setName("NotifUpdater_" + lsd);

				long updateStartTime = System.currentTimeMillis();
				
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
				
				publishNotificationCompat();
				
				long timeToWait = refreshFrequencyMillis(arrival.getMillis(0));
				// time already passed while making the previous query
				// we should subtract this from waiting for the next query.
				long elapsedSinceLastUpdate = System.currentTimeMillis() - updateStartTime;
				
				if(elapsedSinceLastUpdate + 5000 < timeToWait){
					timeToWait -= elapsedSinceLastUpdate;
				} else {
					timeToWait = 5000; // wait at least 5s
				}
				
				setNextAlarm(timeToWait);
		} catch (Exception e) {
			Log.e(LOG_TAG, "" + e.getMessage(), e);
			NotificationManager mNotifManager;
			mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotifManager.cancel(lsd.hashCode());
		}
	}
	
	private void setNextAlarm(long timeToWait){
		// set-up an ALARM for updating the notification
		Intent intent = new Intent(this, NotificationService.class);
		intent.putExtra(ACTION_UPDATE, true);
		intent.putExtra(EXTRA_DIRECTION, arrival.direction);
		intent.putExtra(EXTRA_LINE_STOP_DIR, lsd);
		intent.putExtra(EXTRA_WHEN_UPDATE, this.when);
		PendingIntent pintent = PendingIntent.getService(NotificationService.this, new Random(System.currentTimeMillis()).nextInt(), intent, 0);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, (int) timeToWait);
		alarm.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pintent);
		Log.w(LOG_TAG, "Next update for  " + lsd + " in " + timeToWait);
	}

	private void terminate() {
		NotificationManager mNotifManager;
		mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifManager.cancel(lsd.hashCode());
		this.isRunning.set(false);
		Analytics.getInstance().onPush();
	}

	public static void putQuery(String lsd, BusArrivalQuery qu) {
		queries.put(lsd, qu);
	}

	public static boolean notifExists(String lsd) {
		return notifications.containsKey(lsd);
	}

	/**
	 * Return how many milliseconds to sleep depending on next arrival. This allows to
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
			return 3 * 60 * 1000; // 3min
		}
	}
}
