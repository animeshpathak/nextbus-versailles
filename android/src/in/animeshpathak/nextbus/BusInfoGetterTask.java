package in.animeshpathak.nextbus;

import in.animeshpathak.nextbus.analytics.Analytics;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BusInfoGetterTask extends AsyncTask<Void, Void, Void> {
	private final Activity mainActivity;
	private BusArrivalQuery query;
	private int index;
	private static final String LOG_TAG = Constants.LOG_TAG;
	private static final int ANIMATION_MILLIS = 200;

	// We only do this statically because reflection can be costly
	private static Method executorMethod = executorMethodInit();

	private static Method executorMethodInit() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& executorMethod == null) {
			try {
				return BusInfoGetterTask.class.getMethod("executeOnExecutor",
						Executor.class, Object[].class);
			} catch (Exception e) {
				Log.e(LOG_TAG, "" + e.getMessage());
			}
		}
		return null;
	}

	public BusInfoGetterTask(Activity main, BusArrivalQuery query, int index) {
		this.mainActivity = main;
		this.query = query;
		this.index = index;
	}

	@Override
	protected void onPreExecute() {
		LinearLayout busTimingsScroll = (LinearLayout) mainActivity
				.findViewById(R.id.bus_section);
		View contentView = query.getInitialView(mainActivity);
		Animation a1 = new ScaleAnimation(1, 1, 0, 1, 0, 0);
		a1.setStartOffset(index * ANIMATION_MILLIS);
		a1.setDuration(ANIMATION_MILLIS);
		a1.setFillAfter(true);
		contentView.clearAnimation();
		contentView.startAnimation(a1);
		busTimingsScroll.addView(contentView);
	}

	@Override
	protected Void doInBackground(Void... params) {
		Analytics.getInstance().busArrivalQuery(query.postQuery());
		return null;
	}

	@Override
	protected void onPostExecute(Void param) {
		super.onPostExecute(param);
		try {
			if (query != null) {
				View itemView = query.getInflatedView(mainActivity);
				// make progress bar invisible
				itemView.findViewById(R.id.progressArrBox).setVisibility(
						View.INVISIBLE);

				TextView busTimingsView = (TextView) itemView
						.findViewById(R.id.bus_timings_item);
				busTimingsView.setText("\n"
						+ mainActivity.getString(R.string.last_updated_at)
						+ ": "
						+ DateFormat.getDateTimeInstance(DateFormat.SHORT,
								DateFormat.SHORT).format(new Date()));

				itemView.invalidate();
			}
		} catch (IllegalArgumentException e) {
			// If we try to dismiss a a dialog that was not created.
			Log.d(LOG_TAG,
					"We tried to dismiss a a dialog which was not created."
							+ e.getMessage());
		}
	}

	public void execute(ThreadPoolExecutor tpe) {
		if (executorMethod != null) {
			try {
				executorMethod.invoke(this, tpe, new Void[0]);
			} catch (Exception e) {
				Log.e(LOG_TAG, "" + e.getMessage(), e);
				this.execute();
			}
		} else {
			// launch task
			this.execute();
		}
	}
}
