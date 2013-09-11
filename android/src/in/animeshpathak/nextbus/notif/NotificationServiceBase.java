package in.animeshpathak.nextbus.notif;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.NextBusMain;
import in.animeshpathak.nextbus.R;
import in.animeshpathak.nextbus.analytics.Analytics;
import in.animeshpathak.nextbus.compat.Collections;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery.BusArrivalInfo;

import java.util.Calendar;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.util.Log;

public abstract class NotificationServiceBase extends IntentService {

	public NotificationServiceBase() {
		super("NextBus-Versailles-Notif");
	}

	protected static String LOG_TAG = Constants.LOG_TAG;

	public static final String ACTION_NEW = "new-notif";
	public static final String ACTION_UPDATE = "update-notif";
	
	// just inform the app that the notification is pending
	public static final String ACTION_PING = "ping-notif"; 
	public static final String ACTION_DELETE = "delete-notif";

	public static final String EXTRA_LINE = "extra-line";
	public static final String EXTRA_STOP = "extra-stop";
	public static final String EXTRA_DIRECTION = "extra-direction";
	public static final String EXTRA_WHEN_UPDATE = "extra-when-update";
	public static final String EXTRA_HASH_CODE = "notif-hash-code";
	public static final String EXTRA_ALARM_ID = "ping-alarm-id";
	
	protected static int updateFrequency = -1;
	
	protected static Set<Integer> activeNotifCodes = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

	protected static int[] timeIcons = { R.drawable.bus_00_x,
			R.drawable.bus_01_x, R.drawable.bus_02_x, R.drawable.bus_03_x,
			R.drawable.bus_04_x, R.drawable.bus_05_x, R.drawable.bus_06_x,
			R.drawable.bus_07_x, R.drawable.bus_08_x, R.drawable.bus_09_x,
			R.drawable.bus_10_x, R.drawable.bus_11_x, R.drawable.bus_12_x,
			R.drawable.bus_13_x, R.drawable.bus_14_x, R.drawable.bus_15_x };

	
	protected void publishNotificationCompat(boolean isPopulated,
			boolean isError, BusArrivalQuery query, String directionName,
			long when, int hashCode) {
		final NotificationManager mNotifManager;
		mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final NotificationCompat.Builder nBuild = new NotificationCompat.Builder(this);

		SpannableStringBuilder ssb = new SpannableStringBuilder();
		String notifTitle = query.getBusLine().getName();

		int iconRes = R.drawable.bus_icon_newer_x;

		ssb.append(query.getBusStop().getName() + " -> ");
		ssb.append(directionName);

		if (!isPopulated) {
			// do nothing
		} else if (isError) {
			iconRes = R.drawable.bus_update_error;
			ssb.append(getString(R.string.bus_text_unavailable));
		} else {
			// the query is Populated and Valid
			BusArrivalInfo arrival = query.getNextArrivals().get(directionName);
			int minutesLeft = arrival.getMillis(0) / 60000;
			// The arrival time is less than 16 minutes
			if (minutesLeft < 16) {
				iconRes = timeIcons[minutesLeft];
			}
			notifTitle = query.getBusLine().getName()
					+ " "
					+ BusArrivalQuery.formatTimeDelta(this,
							arrival.getMillis(0));
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
		nBuild.setWhen(when);
		Intent deleteIntent = new Intent(this, NotificationServiceRemove.class);
		deleteIntent.putExtra(ACTION_DELETE, true);
		deleteIntent.putExtra(EXTRA_LINE, query.getBusLine().getCode());
		deleteIntent.putExtra(EXTRA_STOP, query.getBusStop().getCode());
		deleteIntent.putExtra(EXTRA_DIRECTION, directionName);
		deleteIntent.putExtra(EXTRA_HASH_CODE, hashCode);
		nBuild.setContentTitle(notifTitle);
		nBuild.setContentText(ssb.toString());
		nBuild.setContentIntent(contentIntent);
		nBuild.setSmallIcon(iconRes);
		nBuild.setDeleteIntent(PendingIntent.getService(this, hashCode,
				deleteIntent, 0));
		mNotifManager.notify(hashCode, nBuild.build());
	}
	
	public void updateNotification(BusArrivalQuery query, String directionName,
			long notifWhen, int hashCode) {
		try {
			if(!checkNotificationIsActive(hashCode)){
				return;
			}

			String notifCode = getLineStopDirString(query, directionName);
			Thread.currentThread().setName("NotifUpdater_" + notifCode);
			long updateStartTime = System.currentTimeMillis();
			boolean updateError = true;

			Analytics.getInstance().notifServiceQuery(query.postQuery());
			Map<String, BusArrivalInfo> resp = query.getNextArrivals();
			BusArrivalInfo arrival = resp.get(directionName);
			if (query.isValid()
					&& null != arrival
					&& (arrival.getMention(0) & BusArrivalInfo.MENTION_UNKNOWN) == 0) {
				arrival = resp.get(arrival.direction);
				updateError = false;
			} else {
				updateError = true;
			}

			if(checkNotificationIsActive(hashCode)){
				publishNotificationCompat(true, updateError, query, directionName,
					notifWhen, hashCode);
			} else {
				return;
			}

			long timeToWait = 0;
			
			if(updateError || arrival == null){
				
			} else {
				timeToWait = refreshFrequencyMillis(arrival.getMillis(0));
			}
			
			// time already passed while making the previous query
			// we should subtract this from waiting for the next query.
			long elapsedSinceLastUpdate = System.currentTimeMillis()
					- updateStartTime;

			if (elapsedSinceLastUpdate + 5000 < timeToWait) {
				timeToWait -= elapsedSinceLastUpdate;
			} else {
				timeToWait = 5000; // wait at least 5s
			}

			setNextUpdateAlarm(timeToWait, query, directionName, notifWhen, hashCode);
		} catch (Exception e) {
			Log.e(LOG_TAG, "" + e.getMessage(), e);
			// dismiss notification
			terminate(hashCode);
		}
	}
	
	protected void setNextUpdateAlarm(long timeToWait, BusArrivalQuery query,
			String directionName, long notifWhen, int notifHashCode) {
		// set-up an ALARM for updating the notification
		Intent intent = new Intent(this, NotificationServiceUpdate.class);
		intent.putExtra(ACTION_UPDATE, true);
		intent.putExtra(EXTRA_LINE, query.getBusLine().getCode());
		intent.putExtra(EXTRA_STOP, query.getBusStop().getCode());
		intent.putExtra(EXTRA_DIRECTION, directionName);
		intent.putExtra(EXTRA_HASH_CODE, notifHashCode);
		intent.putExtra(EXTRA_WHEN_UPDATE, notifWhen);
		PendingIntent pintent = PendingIntent.getService(
				this,
				new Random(System.currentTimeMillis()).nextInt(), intent, 0);
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MILLISECOND, (int) timeToWait);
		alarm.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pintent);
		Log.w(LOG_TAG,
				"Next ALARM for " + getLineStopDirString(query, directionName)
						+ " in " + timeToWait);
	}
	
	protected void setRepeatedPingAlarm(long timeToWait, BusArrivalQuery query,
			String directionName, long notifWhen, int notifHashCode) {
		// set-up an ALARM for updating the notification
		Intent intent = new Intent(this, NotificationServiceUpdate.class);
		intent.putExtra(ACTION_PING, true);
		intent.putExtra(EXTRA_LINE, query.getBusLine().getCode());
		intent.putExtra(EXTRA_STOP, query.getBusStop().getCode());
		intent.putExtra(EXTRA_DIRECTION, directionName);
		intent.putExtra(EXTRA_HASH_CODE, notifHashCode);
		intent.putExtra(EXTRA_WHEN_UPDATE, notifWhen);
		
		int alarmId = new Random(System.currentTimeMillis()).nextInt();
		
		intent.putExtra(EXTRA_ALARM_ID, alarmId);
		PendingIntent pintent = PendingIntent.getService(
				this,
				alarmId, intent, 0);
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MILLISECOND, (int) timeToWait);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), NotificationServiceUpdate.NOTIFICATION_PING_MILLIS, pintent);
	}
	
	/**
	 * Return how many milliseconds to sleep depending on next arrival. This
	 * allows to refresh frequently when there is little time left until
	 * arrival, while saving battery if the bus will arrive much later.
	 * 
	 * @param msUntilArrival
	 * @return
	 */
	public static int refreshFrequencyMillis(int msUntilArrival) {
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
	
	protected boolean checkNotificationIsActive(int hashCode){
		if (!activeNotifCodes.contains(hashCode)) {
			// query was removed, ignoring update
			Log.w(LOG_TAG, "Notification dismissed. No more updates for: "
					+ hashCode);
			terminate(hashCode);
			return false;
		}
		
		return true;
	}

	protected void terminate(int hashCode) {
		NotificationManager mNotifManager;
		mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifManager.cancel(hashCode);
		Analytics.getInstance().onPush();
	}

	public static boolean notifExists(BusArrivalQuery query, String direction) {
		assert (null != query);
		assert (null != direction);
		String lineStopDir = getLineStopDirString(query, direction);
		return activeNotifCodes.contains(lineStopDir.hashCode());
	}

	protected static String getLineStopDirString(BusArrivalQuery query,
			String directionName) {
		String lineStopDir = String.format("%s-%s-%s", query.getBusLine()
				.getCode(), query.getBusStop().getCode(), directionName);
		return lineStopDir;
	}
}
