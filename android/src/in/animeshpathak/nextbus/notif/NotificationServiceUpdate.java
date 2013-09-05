package in.animeshpathak.nextbus.notif;

import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.BusArrivalQueryFactory;

import java.io.IOException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationServiceUpdate extends NotificationServiceBase {

	public static final int NOTIFICATION_PING_MILLIS = 15 * 1000;

	@Override
	protected void onHandleIntent(Intent intent) {
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

		long notifWhen = intent.getLongExtra(EXTRA_WHEN_UPDATE,
				System.currentTimeMillis());
		int notifHashCode = intent.getIntExtra(EXTRA_HASH_CODE, 0);
		assert (0 != notifHashCode);

		if (intent.getBooleanExtra(ACTION_UPDATE, false)) {
			Log.w(LOG_TAG, "UPDATE Notification: " + query.toString());
			updateNotification(query, directionName, notifWhen, notifHashCode);
		} else if (intent.getBooleanExtra(ACTION_PING, false)) {
			int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, 0);
			// a notification is pending
			if(!checkNotificationIsActive(notifHashCode)){
				
				// disable ping ALARM for this notification
				Log.w(LOG_TAG, "Disabling PING ALARM for ID: " + alarmId);
				AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				PendingIntent pintent = PendingIntent.getService(
						this,
						alarmId, intent, 0);
				alarm.cancel(pintent);
			} else {
				Log.w(LOG_TAG, "PING ALARM for ID: " + alarmId);
			}
		}
	}

}
