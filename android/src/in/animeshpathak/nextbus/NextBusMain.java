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

import in.animeshpathak.nextbus.analytics.Analytics;
import in.animeshpathak.nextbus.favorites.Favorite;
import in.animeshpathak.nextbus.favorites.FavoriteDialog;
import in.animeshpathak.nextbus.favorites.FavoriteDialog.OnFavoriteSelectedListener;
import in.animeshpathak.nextbus.news.PhebusNewsLoader;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery;
import in.animeshpathak.nextbus.timetable.BusArrivalQueryFactory;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusNetwork;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public class NextBusMain extends Activity {
	private static String LOG_TAG = Constants.LOG_TAG;

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
		Log.d(LOG_TAG, "entering onCreate()");
		SettingsActivity.setTheme(this);
		super.onCreate(bundle);
		setContentView(R.layout.main);

		try {
			busNet = new BusNetwork(this);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			return;
		}

		// get the button
		// set handler to launch a processing dialog
		ImageButton updateButton = (ImageButton) findViewById(R.id.update_button);
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

		ImageButton feedbackButton = (ImageButton) findViewById(R.id.feedback_button);
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

		ImageButton phebusinfoButton = (ImageButton) findViewById(R.id.phebusinfo_button);
		phebusinfoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog alertDialog = new AlertDialog.Builder(
						NextBusMain.this).create();
				WebView wv = new WebView(NextBusMain.this);
				new PhebusNewsLoader(wv, NextBusMain.this).execute();
				alertDialog.setView(wv);
				alertDialog.show();
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

		ImageButton favoriteButton = (ImageButton) findViewById(R.id.favorites_button);
		favoriteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				new FavoriteDialog(
						NextBusMain.this,
						new OnFavoriteSelectedListener() {
							@Override
							public void favoriteSelected(Favorite fav) {
								BusLine bl = busNet.getLineByName(fav.getLine());
								BusStop bs = bl.getFirstStopWithSimilarName(fav
										.getStop());
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

			BusLine newLine = lineAdapter.getItem(pos);
			Collection<BusStop> stops = newLine.getStops();
			for (BusStop busStop : stops) {
				// addAll method is unavailable in old APIs
				stopAdapter.add(busStop);
			}
			stopAdapter.notifyDataSetChanged();

			// Find if there exists a stop with similar name on this line
			BusStop newStop = newLine.getFirstStopWithSimilarName(oldSelection
					.getName());
			if (newStop != null) {
				stopSpinner.setSelection(stopAdapter.getPosition(newStop));
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
		Collection<BusStop> stops = line.getStops();
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

		List<BusLine> theLines = busNet.getLines();

		if (selectedLineID < 0 || theLines.size() <= selectedLineID) {
			selectedLineID = 0;
			Log.w(LOG_TAG, "onResume() selectedLineID is out of bounds: "
					+ selectedLineID);
		}

		BusLine bl = theLines.get(selectedLineID);
		List<BusStop> lineStops = bl.getStops();

		if (selectedStopID < 0 || lineStops.size() <= selectedStopID) {
			selectedStopID = 0;
			Log.w(LOG_TAG, "onResume() selectedStopID is out of bounds: "
					+ selectedStopID);
		}

		BusStop bs = lineStops.get(selectedStopID);
		updateSpinners(bl, bs);

		// Show notification on the first run
		if (!prefs.getBoolean(getString(R.string.version_name), false)) {
			try {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(getString(R.string.version_name), true);
				showVersionInfoDialog();
				editor.commit();
			} catch (UnsupportedEncodingException e) {
				Log.e(LOG_TAG,
						"onResume() problem in versioninfo file encoding.", e);
			} catch (IOException e) {
				Log.e(LOG_TAG,
						"onResume() problem in versioninfo file encoding.", e);
			}
		}

		// Show red button if Phebus posted new alerts
		new PhebusNewsLoader(null, NextBusMain.this).execute();

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

		int lineIndex = busNet.getLines().indexOf(bl);
		int stopIndex = bl.getStops().indexOf(bs);

		if (lineIndex < 0 || stopIndex < 0) {
			lineIndex = 0;
			stopIndex = 0;
			Log.w(LOG_TAG, "onPause() lineIndex/stopIndex invalid");
		}

		editor.putInt(Constants.SELECTED_LINE, lineIndex);
		editor.putInt(Constants.SELECTED_STOP, stopIndex);
		editor.commit();
		super.onPause();
	}

	private void getBusTimings(BusLine selectedLine, BusStop selectedStop) {
		try {
			// Clearing old data
			LinearLayout busTimingsViewContainer = (LinearLayout) this
					.findViewById(R.id.bus_section);
			busTimingsViewContainer.removeAllViews();

			if (selectedLine.getName().startsWith("*")) {
				getMultipleBusTimings(selectedStop);
			} else {
				BusArrivalQuery query = BusArrivalQueryFactory.getInstance(
						selectedLine, selectedStop);
				new BusInfoGetterTask(this, query, 0).execute(taskExecutor);
			}
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
			BusStop foundStop = cLine.getFirstStopWithSimilarName(selectedStop
					.getName());
			if (foundStop != null) {
				linesToSearch.put(cLine, foundStop);
			}
		}

		int idx = 0;
		for (Entry<BusLine, BusStop> qLine : linesToSearch.entrySet()) {
			BusArrivalQuery query = BusArrivalQueryFactory.getInstance(
					qLine.getKey(), qLine.getValue());
			BusInfoGetterTask big = new BusInfoGetterTask(this, query, idx++);
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
		case R.id.settings_menu:
			startActivity(new Intent(NextBusMain.this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Creates the Application Info dialog with clickable links.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private void showVersionInfoDialog() throws UnsupportedEncodingException,
			IOException {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle(getString(R.string.app_name) + " "
				+ getString(R.string.version_name));

		AssetManager assetManager = getResources().getAssets();
		String versionInfoFile = getString(R.string.versioninfo_asset);
		InputStreamReader reader = new InputStreamReader(
				assetManager.open(versionInfoFile), "UTF-8");
		BufferedReader br = new BufferedReader(reader);
		StringBuffer sbuf = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sbuf.append(line);
			sbuf.append("\r\n");
		}

		final ScrollView scroll = new ScrollView(this);
		final TextView message = new TextView(this);
		final SpannableString sWlinks = new SpannableString(sbuf.toString());
		Linkify.addLinks(sWlinks, Linkify.WEB_URLS);
		message.setText(sWlinks);
		message.setMovementMethod(LinkMovementMethod.getInstance());
		message.setPadding(15, 15, 15, 15);
		scroll.addView(message);
		alertDialog.setView(scroll);
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// nothing to do, just dismiss dialog
					}
				});
		alertDialog.show();
	}

	@Override
	public void onStart() {
		super.onStart();
		Analytics.getInstance().setContext(this.getApplicationContext());
	}

	@Override
	public void onStop() {
		super.onStop();
		Analytics.getInstance().onPush();
	}
}