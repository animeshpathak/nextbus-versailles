package in.animeshpathak.nextbus.notif;

import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.BusArrivalQueryFactory;

import java.io.IOException;

import android.content.Intent;
import android.util.Log;

public class NotificationServiceRemove extends NotificationServiceBase {
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

		String lineStopDir = getLineStopDirString(query, directionName);
		if (intent.getBooleanExtra(ACTION_DELETE, false)) {
			Log.w(LOG_TAG, "STOP Notification: " + query.toString());
			terminate(lineStopDir.hashCode());

			int notifHashCode = intent.getIntExtra(EXTRA_HASH_CODE, 0);
			assert (0 != notifHashCode);

			activeNotifCodes.remove(notifHashCode);
		}
	}
}
