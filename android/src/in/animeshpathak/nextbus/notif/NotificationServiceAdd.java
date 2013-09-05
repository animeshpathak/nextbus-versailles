package in.animeshpathak.nextbus.notif;

import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.BusArrivalQueryFactory;

import java.io.IOException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationServiceAdd extends NotificationServiceBase {

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

		String lineCode = intent.getStringExtra(EXTRA_LINE);
		String stopCode = intent.getStringExtra(EXTRA_STOP);
		String directionName = intent.getStringExtra(EXTRA_DIRECTION);
		assert (lineCode != null);
		assert (stopCode != null);
		assert (directionName != null);

		BusArrivalQuery query;

		try {
			query = BusArrivalQueryFactory
					.getInstance(this, lineCode, stopCode);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Notification error: " + e.getMessage());
			return;
		}

		String lineStopDir = getLineStopDirString(query, directionName);

		if (intent.getBooleanExtra(ACTION_NEW, false)) {
			Log.w(LOG_TAG, "NEW Notification: " + query.toString());
			int notifHashCode = lineStopDir.hashCode();

			activeNotifCodes.add(notifHashCode);
			// initially show the notification
			long newWhen = System.currentTimeMillis();

			publishNotificationCompat(false, false, query, directionName,
					newWhen, notifHashCode);
			// set-up an ALARM to update the notification imediately
			setNextUpdateAlarm(0, query, directionName, newWhen, notifHashCode);

			// more frequent alarm to inform the application that the
			// notification is still displayed
			setRepeatedPingAlarm(
					NotificationServiceUpdate.NOTIFICATION_PING_MILLIS, query,
					directionName, newWhen, notifHashCode);
		}
	}
}
