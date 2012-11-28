package in.animeshpathak.nextbus;

import in.animeshpathak.nextbus.timetable.BusArrivalQuery;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class BusInfoGetterTask extends
		AsyncTask<BusArrivalQuery, Void, BusArrivalQuery> {

	private final ProgressDialog progressDialog;
	private final Activity mainActivity;
	private static final String LOG_TAG = "NEXTBUS";

	public BusInfoGetterTask(Activity main) {
		this.mainActivity = main;
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

	@Override
	protected void onPreExecute() {
		progressDialog.show();
		TextView busTimingsView = (TextView) mainActivity
				.findViewById(R.id.bus_timings);
		busTimingsView.setText(mainActivity.getString(R.string.bus_times));
	}

	@Override
	protected void onCancelled() {
		progressDialog.dismiss();
	}

	@Override
	protected BusArrivalQuery doInBackground(BusArrivalQuery... params) {
		if (params.length < 1 || params[0] == null)
			return null;

		Log.d(LOG_TAG, "Done doing my stuff. " + "\nGot response: " + params[0]
				+ "Sending message for dismissing dialog now.");

		if (params[0] != null && params[0].postQuery()) {
			return params[0];
		}

		return null;
	}

	@Override
	protected void onPostExecute(BusArrivalQuery serverResponse) {
		super.onPostExecute(serverResponse);
		
		if(serverResponse == null)
			return;
		
		try {
			progressDialog.dismiss();

			Log.d(LOG_TAG, "\nUpload Complete. The Server said...\n");

			TextView busTimingsView = (TextView) mainActivity
					.findViewById(R.id.bus_timings);
			busTimingsView.setText("\n"
					+ mainActivity.getString(R.string.last_updated_at)
					+ ": "
					+ DateFormat.getDateTimeInstance(DateFormat.SHORT,
							DateFormat.SHORT).format(new Date()) + "\n"
					+ mainActivity.getString(R.string.server_said) + "...\n");
			if (serverResponse != null && serverResponse.isValid())
				busTimingsView.append(serverResponse.getFormattedText(mainActivity));
			else if (serverResponse != null)
				busTimingsView.append(serverResponse.getFallbackText());
		} catch (IllegalArgumentException e) {
			// If we try to dismiss a a dialog that was not created.
			Log.d(LOG_TAG,
					"We tried to dismiss a a dialog which was not created."
							+ e.getMessage());
		}
	}
}
