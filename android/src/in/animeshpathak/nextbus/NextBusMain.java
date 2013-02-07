/*  This file is part of Next Bus Versailles. 
 * (c) Animesh Pathak, www.animeshpathak.in
 * Next Bus Versailles is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Next Bus Versailles is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Next Bus Versailles.  If not, see <http://www.gnu.org/licenses/>.
 */

package in.animeshpathak.nextbus;

import in.animeshpathak.nextbus.favorites.Favorite;
import in.animeshpathak.nextbus.favorites.FavoriteDialog;
import in.animeshpathak.nextbus.favorites.FavoriteDialog.OnFavoriteSelectedListener;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.PhebusArrivalQuery;
import in.animeshpathak.nextbus.timetable.RatpArrivalQuery;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusNetwork;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class NextBusMain extends Activity {
	private static String LOG_TAG = "NEXTBUS";

	// To get new bus-lines use (e.g.): curl
	// http://www.phebus.tm.fr/sites/all/themes/mine/phebus/itineraire/AppelArret.php
	// --data "ligne=00JLB" > 0JLB.json

	// Contains all data regarding bus lines and stops
	private BusNetwork busNet;

	// UI elements
	Spinner lineSpinner;
	ArrayAdapter<BusLine> lineAdapter;
	Spinner stopSpinner;
	ArrayAdapter<BusStop> stopAdapter;

	// Executor used for parallel AsyncTasks
	ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(10, 20, 10,
			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20));

	/** Called when the activity is first started. */
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Log.d(LOG_TAG, "entering onCreate()");
		setContentView(R.layout.main);

		try {
			busNet = new BusNetwork(this);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			return;
		}

		// get the button
		// set handler to launch a processing dialog
		Button updateButton = (Button) findViewById(R.id.update_button);
		updateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				BusLine selectedLine = lineAdapter.getItem(lineSpinner
						.getSelectedItemPosition());
				BusStop selectedStop = stopAdapter.getItem(stopSpinner
						.getSelectedItemPosition());
				getBusTimings(selectedLine, selectedStop);
			}
		});

		Button feedbackButton = (Button) findViewById(R.id.feedback_button);
		feedbackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("message/rfc822"); // use from live device
				i.putExtra(Intent.EXTRA_EMAIL,
						new String[] { Constants.FEEDBACK_EMAIL_ADDRESS });
				i.putExtra(Intent.EXTRA_SUBJECT,
						Constants.FEEDBACK_EMAIL_SUBJECT);
				i.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_hello));
				startActivity(Intent.createChooser(i,
						getString(R.string.select_email_app)));
			}
		});

		lineSpinner = (Spinner) findViewById(R.id.line_spinner);
		lineAdapter = new ArrayAdapter<BusLine>(this,
				android.R.layout.simple_spinner_item, busNet.getLines());
		lineAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		lineSpinner.setAdapter(lineAdapter);

		stopSpinner = (Spinner) findViewById(R.id.stop_spinner);
		stopAdapter = new ArrayAdapter<BusStop>(this,
				android.R.layout.simple_spinner_item);
		stopAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stopSpinner.setAdapter(stopAdapter);

		Button favoriteButton = (Button) findViewById(R.id.favorites_button);
		favoriteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				new FavoriteDialog(
						NextBusMain.this,
						new OnFavoriteSelectedListener() {
							@Override
							public void favoriteSelected(Favorite fav) {
								BusLine bl = busNet.getLineByName(fav.getLine());
								BusStop bs = bl.getFirstStopWithName(fav.getStop(), 0); 
								if (bl == null || bs == null) {
									Log.e(LOG_TAG, "Favorite not found!");
									return;
								}
								updateSpinners(bl, bs);
								getBusTimings(bl, bs);
							}
						}, lineSpinner.getSelectedItem().toString(),
						stopSpinner.getSelectedItem().toString());
			}
		});
	}

	OnItemSelectedListener lineSpinnerListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> view, View arg1, int pos,
				long id) {
			Log.d(LOG_TAG, "lineSpinnerItemSelected!");
			Log.d(LOG_TAG, "Line Position = " + pos);
			Log.d(LOG_TAG, "Line ID = " + id);

			BusStop oldSelection = stopAdapter.getItem(stopSpinner
					.getSelectedItemPosition());
			stopAdapter.clear();
			List<BusStop> stops = lineAdapter.getItem(pos).getStops();
			for (BusStop busStop : stops) {
				// addAll method is unavailable in old APIs
				stopAdapter.add(busStop);
			}
			stopAdapter.notifyDataSetChanged();

			// The stop exists in the new selection
			if (stops.contains(oldSelection)) {
				stopSpinner.setSelection(stopAdapter.getPosition(oldSelection));
			} else {
				stopSpinner.setSelection(0);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// do nothing
		}
	};

	private void updateSpinners(BusLine line, BusStop stop) {
		// disable the selection listener
		lineSpinner.setOnItemSelectedListener(null);
		lineSpinner.setSelection(lineAdapter.getPosition(line), false);
		stopAdapter.clear();
		List<BusStop> stops = line.getStops();
		for (BusStop busStop : stops) {
			// addAll method is unavailable in old APIs
			stopAdapter.add(busStop);
		}
		stopAdapter.notifyDataSetChanged();
		stopSpinner.setSelection(stopAdapter.getPosition(stop), false);
		lineSpinner.setOnItemSelectedListener(lineSpinnerListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "entering onResume()");
		// at this time, the UI elements have been created. Need to read
		// It is best not to reuse references to views after pause (Android
		// seems to create new objects)
		lineSpinner = (Spinner) findViewById(R.id.line_spinner);
		stopSpinner = (Spinner) findViewById(R.id.stop_spinner);
		// the shared preferences, and store them in local variables.
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		int selectedLineID = prefs.getInt(Constants.SELECTED_LINE, 0);
		int selectedStopID = prefs.getInt(Constants.SELECTED_STOP, 0);

		if(selectedLineID < 0)
			selectedLineID = 0;
		
		if(selectedStopID < 0)
			selectedStopID = 0;
		
		BusLine bl = busNet.getLines().get(selectedLineID);
		BusStop bs = bl.getStops().get(selectedStopID);
		updateSpinners(bl, bs);
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "entering onPause()");
		// at this time, store the local variables pointing to the indices
		// of the line and stop, then return
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		BusLine bl = (BusLine) lineSpinner.getSelectedItem();
		BusStop bs = (BusStop) stopSpinner.getSelectedItem();

		editor.putInt(Constants.SELECTED_LINE, busNet.getLines().indexOf(bl));
		editor.putInt(Constants.SELECTED_STOP, bl.getStops().indexOf(bs));
		editor.commit();
		super.onPause();
	}

	private void getBusTimings(BusLine selectedLine, BusStop selectedStop) {
		try {
			// Clearing old data
			// TextView busTimingsView = (TextView) this
			// .findViewById(R.id.bus_timings);
			// busTimingsView.setText(this.getString(R.string.bus_times));
			LinearLayout busTimingsViewContainer = (LinearLayout) this
					.findViewById(R.id.bus_section);
			busTimingsViewContainer.removeAllViews();
			BusArrivalQuery query = null;

			if (selectedLine.getName().startsWith("*")) {
				getMultipleBusTimings(selectedStop);
				return;
			} else if (selectedLine.getName().startsWith("RATP")) {
				query = new RatpArrivalQuery(selectedLine.getCode(),
						selectedStop.getCode());
			} else {
				query = new PhebusArrivalQuery(selectedLine.getCode(),
						selectedStop.getCode());
			}

			// launch task
			new BusInfoGetterTask(this, false, query).execute();
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
	}

	/**
	 * Makes multiple queries for obtaining all buses arriving at the selected
	 * station
	 * 
	 * @param selectedStop
	 *            The chosen stopId.
	 * @throws JSONException
	 * @throws IOException
	 */
	private void getMultipleBusTimings(BusStop selectedStop)
			throws IOException, JSONException {
		// Get all lines which have this stop in their path
		// Highly inefficient, but Ok for testing
		Map<BusLine, BusStop> linesToSearch = new TreeMap<BusLine, BusStop>();
		List<BusLine> allLines = busNet.getLines();
		for (BusLine cLine : allLines) {
			if (cLine.getName().startsWith("*"))
				continue;

			/*
			 * Warning: stops might have identical names but different codes.
			 * This is why we must always query the BusLine object which has
			 * reference to the correct BusStop object.
			 */
			BusStop foundStop = cLine.getFirstStopWithName(
					selectedStop.getName(), 1);
			if (foundStop != null) {
				linesToSearch.put(cLine, foundStop);
			}
		}

		for (Entry<BusLine, BusStop> qLine : linesToSearch.entrySet()) {
			BusArrivalQuery query;

			if (qLine.getKey().getName().contains("RATP")) {
				query = new RatpArrivalQuery(qLine.getKey().getCode(), qLine
						.getValue().getCode());
			} else {
				query = new PhebusArrivalQuery(qLine.getKey().getCode(), qLine
						.getValue().getCode());
			}

			BusInfoGetterTask big = new BusInfoGetterTask(this, false, query);
			big.execute(taskExecutor);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.about_menu:
			startActivity(new Intent(NextBusMain.this, AboutActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}