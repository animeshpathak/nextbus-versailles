package in.animeshpathak.nextbus;

import in.animeshpathak.nextbus.analytics.Analytics;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BusInfoGetterTask extends AsyncTask<Void, Void, Void> {

	private ProgressDialog progressDialog = null;
	private final Activity mainActivity;
	private boolean showWaitDialog = true;
	private BusArrivalQuery query;
	private static final String LOG_TAG = "NEXTBUS";

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

	public BusInfoGetterTask(Activity main, boolean showWaitDialog,
			BusArrivalQuery query) {
		this.mainActivity = main;
		this.showWaitDialog = showWaitDialog;
		this.query = query;

		if (this.showWaitDialog) {
			this.progressDialog = new ProgressDialog(main);
			this.progressDialog.setMessage(main
					.getString(R.string.getting_latest_times) + "...");
			this.progressDialog.setCancelable(true);
			this.progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					cancel(true);
				}
			});
		}
	}

	@Override
	protected void onPreExecute() {
		if (showWaitDialog)
			progressDialog.show();

		LinearLayout busTimingsScroll = (LinearLayout) mainActivity
				.findViewById(R.id.bus_section);
		busTimingsScroll.addView(query.getInitialView(mainActivity));
	}

	@Override
	protected void onCancelled() {
		if (showWaitDialog)
			progressDialog.dismiss();
	}

	@Override
	protected Void doInBackground(Void... params) {
		Analytics.busArrivalQuery(query.postQuery());
		Log.d(LOG_TAG, "Done doing my stuff. " + "\nGot response: "
				+ "Sending message for dismissing dialog now.");
		return null;
	}

	@Override
	protected void onPostExecute(Void param) {
		super.onPostExecute(param);

		try {
			if (showWaitDialog)
				progressDialog.dismiss();

			Log.d(LOG_TAG, "\nUpload Complete. The Server said...\n");

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
				Log.e(LOG_TAG, "" + e.getMessage());
				this.execute();
			}
		} else {
			// launch task
			this.execute();
		}
	}
}
