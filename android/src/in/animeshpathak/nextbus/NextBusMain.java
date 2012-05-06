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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
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
import android.widget.Spinner;
import android.widget.TextView;

public class NextBusMain extends Activity {

	protected static final int DIALOG_GETTING_BUS_INFO = 0;

	private static String LOG_TAG = "NEXTBUS";

	// The response from the server
	private String serverResponse;

	// The textview that holds the view
	private TextView busTimingsView;

	// To get new bus-lines use (e.g.): curl
	// http://www.phebus.tm.fr/sites/all/themes/mine/phebus/itineraire/AppelArret.php
	// --data "ligne=00JLB" > 0JLB.json
	// Holds the labels of the known bus lines
	private String[] busLines = {};

	// Holds the file-names of the bus-line data
	private String[] busLineAssets = {};

	private String[] stopNameArray = {};

	private String[] stopCodeArray = {};

	/** The currently selected line */
	private int selectedLineID;

	// ID into above arrays to find the stop
	private int selectedStopID;
	
	
	//UI elements
	Spinner lineSpinner;
	ArrayAdapter<CharSequence> lineAdapter;
	Spinner stopSpinner;
	ArrayAdapter<CharSequence> stopAdapter;
	

	/**
	 * The Handler that will run the runnable which will then update the bus
	 * timings console
	 */
	final Handler uiHandler = new Handler();

	final Runnable showServerResponse = new Runnable() {
		public void run() {
			dismissDialog(DIALOG_GETTING_BUS_INFO);

			Log.d(LOG_TAG, "\nUpload Complete. The Server said...\n");
			Log.d(LOG_TAG, serverResponse);

			busTimingsView.append("\nLast Updated at: "
					+ DateFormat.getDateTimeInstance(DateFormat.SHORT,
							DateFormat.SHORT).format(new Date())
					+ "\nThe server said...\n");
			CharSequence serverResponseInChars = Html.fromHtml(serverResponse, null, new TagHandler() {
				@Override
				public void handleTag(boolean opening, String tag, Editable output,
						XMLReader xmlReader) {
					if(tag.equals("td") && !opening)
						output.append("\r\n");
				}
			});
			busTimingsView.append(removeExcessBlankLines(serverResponseInChars));
		}
	};
	
	/**
	 *  Thanks to Lorne Laliberte
	 *  http://codereview.stackexchange.com/questions/3099/android-remove-useless-whitespace-from-styled-string
	 *  
	 * @param source
	 * @return
	 */
	public static CharSequence removeExcessBlankLines(CharSequence source) {

	    if(source == null)
	        return "";

	    int newlineStart = -1;
	    int nbspStart = -1;
	    int consecutiveNewlines = 0;
	    SpannableStringBuilder ssb = new SpannableStringBuilder(source);
	    for(int i = 0; i < ssb.length(); ++i) {
	        final char c = ssb.charAt(i);
	        if(c == '\n') {
	            if(consecutiveNewlines == 0)
	                newlineStart = i;

	            ++consecutiveNewlines;
	            nbspStart = -1;
	        }
	        else if(c == '\u00A0') {
	            if(nbspStart == -1)
	                nbspStart = i;
	        }
	        else if(consecutiveNewlines > 0) {

	            // note: also removes lines containing only whitespace,
	            //       or nbsp; except at the beginning of a line
	            if( !Character.isWhitespace(c) && c != '\u00A0') {

	                // we've reached the end
	                if(consecutiveNewlines > 2) {
	                    // replace the many \n with one
	                    ssb.replace(newlineStart, nbspStart > newlineStart ? nbspStart : i, "\n");
	                    i -= i - newlineStart;
	                }

	                consecutiveNewlines = 0;
	                nbspStart = -1;
	            }
	        }
	    }

	    return ssb;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG,"entering onCreate()");

		try {
			Resources resources = getResources();
			AssetManager assetManager = resources.getAssets();
			InputStream inputStream;
			inputStream = assetManager.open("buslines.properties");
			Properties properties = new Properties();
			properties.load(inputStream);

			int setSize = properties.size();
			busLines = properties.keySet().toArray(new String[setSize]);
			Arrays.sort(busLines, String.CASE_INSENSITIVE_ORDER);
			busLineAssets = new String[setSize];
			for (int i = 0; i < setSize; i++) {
				busLineAssets[i] = (String) properties.get(busLines[i]);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}

		setContentView(R.layout.main);

		busTimingsView = (TextView) findViewById(R.id.bus_timings);

		// get the button
		// set handler to launch a processing dialog
		Button updateButton = (Button) findViewById(R.id.update_button);
		updateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				busTimingsView.setText("");
				// start the new thread
				new BusInfoGetter().start();
				// show the progress dialog
				showDialog(DIALOG_GETTING_BUS_INFO);

			}
		});

		Button feedbackButton = (Button) findViewById(R.id.feedback_button);
		feedbackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("message/rfc822"); // use from live device
				// TODO pull these strings into a constants file
				i.putExtra(Intent.EXTRA_EMAIL,
						new String[] { "animesh@gmail.com" });
				i.putExtra(Intent.EXTRA_SUBJECT,
						"[nextbus-versailles] Feedback");
				// TODO localize these strings
				i.putExtra(Intent.EXTRA_TEXT, "Bonjour,\n");
				startActivity(Intent.createChooser(i,
						"Select your preferred email application."));
			}
		});

		lineSpinner = (Spinner) findViewById(R.id.line_spinner);
		lineAdapter = new ArrayAdapter<CharSequence>(
				this, android.R.layout.simple_spinner_item, busLines);

		lineAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		lineSpinner.setAdapter(lineAdapter);
		lineSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> view, View arg1, int pos,
					long id) {
				Log.d(LOG_TAG,"lineSpinnerItemSelected!");
				Log.d(LOG_TAG, "Line Position = " + pos);
				Log.d(LOG_TAG, "Line ID = " + id);
				//set the line ID
				selectedLineID = (int) id;
				//reset the stop ID
				selectedStopID = 0; // not required, changing the adapter will also do this

				Log.d(LOG_TAG,
						"I hope that the selected item "
								+ view.getItemAtPosition(pos)
								+ " is the same as " + busLines[selectedLineID]);

				showAndSelectStopSpinner();

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}
		});

		lineSpinner = (Spinner) findViewById(R.id.stop_spinner);
		stopSpinner = (Spinner) findViewById(R.id.stop_spinner);

		stopSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> view, View arg1, int pos,
					long id) {
				Log.d(LOG_TAG,"stopSpinnerItemSelected!");
				Log.d(LOG_TAG, "Stop Position = " + pos);
				Log.d(LOG_TAG, "Stop ID = " + id);
				selectedStopID = (int) id;

				Log.d(LOG_TAG,
						"I hope that the selected item "
								+ view.getItemAtPosition(pos)
								+ " is the same as "
								+ stopNameArray[selectedStopID]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}

		});
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG,"entering onResume()");

		// at this time, the UI elements have been created. Need to read
		// It is best not to reuse references to views after pause (Android seems to create new objects)
		lineSpinner = (Spinner) findViewById(R.id.line_spinner);
		// the shared preferences, and store them in local variables.
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		selectedLineID = prefs.getInt(Constants.SELECTED_LINE, 0);
		// then call the dropdowns to be properly displayed
		showAndSelectLineSpinner();
		
		stopSpinner = (Spinner) findViewById(R.id.stop_spinner);
		// Previous call to showAndSelectLineSpinner() has set selectedStopID to 0, need to reset it
		selectedStopID = prefs.getInt(Constants.SELECTED_STOP, 0);
		// The adapter was already recreated in the previous call, doing this again will trigger position to change to 0 again
		//showAndSelectStopSpinner();
		stopSpinner.setSelection(stopAdapter.getPosition(stopNameArray[selectedStopID]), true);
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG,"entering onPause()");
		super.onPause();

		// at this time, store the local variables pointing to the indices
		// of the line and stop, then return
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(Constants.SELECTED_LINE, selectedLineID);
		editor.putInt(Constants.SELECTED_STOP, selectedStopID);
		editor.commit();

	}
	
	private void showAndSelectLineSpinner(){
		// now check if the stopName is not null. If so, set it properly.
		// apparently this does not work if animation parameter not set
		// I think animation makes the change happen after the view is displayed (and not before). So it is an order problem
		lineSpinner.setSelection(lineAdapter.getPosition(busLines[selectedLineID]), true);
	}

	/**
	 * Populates the stop spinner with the list of stops for a particular line,
	 * using the Object variables for selected line and stop ID
	 */
	private void showAndSelectStopSpinner() {
		try {
			Resources resources = getResources();
			AssetManager assetManager = resources.getAssets();
			BusLine bl = BusLine.parseJson(busLines[selectedLineID],
					assetManager.open(busLineAssets[selectedLineID]));
			stopNameArray = bl.getNameArray();
			stopCodeArray = bl.getCodeArray();

			stopAdapter = new ArrayAdapter<CharSequence>(
					NextBusMain.this, android.R.layout.simple_spinner_item,
					stopNameArray);
			stopAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			stopSpinner.setAdapter(stopAdapter);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_GETTING_BUS_INFO:
			Dialog d = new ProgressDialog(NextBusMain.this);
			d.setTitle("Getting latest bus timings. Please wait");
			return d;
		default:
			return null;
		}
	}

	// TODO use AsyncTask
	class BusInfoGetter extends Thread {

		// This executes a POST and gets the actual info from the website
		// Thanks to http://www.androidsnippets.org/snippets/36/
		private String getBusTimings() {
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(
					"http://www.phebus.tm.fr/sites/all/themes/mine/phebus/itineraire/code_temps_reel.php");

			String errorMessage = "";

			try {
				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				Log.d(LOG_TAG, "Getting info for stop id " + selectedStopID
						+ ", " + stopCodeArray[selectedStopID]);
				nameValuePairs.add(new BasicNameValuePair("arret",
						stopCodeArray[selectedStopID]));
				nameValuePairs.add(new BasicNameValuePair("ligne",
						busLines[selectedLineID]));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				Log.d(LOG_TAG, "starting POST request now");
				// Execute HTTP Post Request
				HttpResponse response = httpclient.execute(httppost);
				Log.d(LOG_TAG, "response received");

				ByteArrayOutputStream myBaos = new ByteArrayOutputStream();

				response.getEntity().writeTo(myBaos);
				String escapedString = myBaos.toString();
				return StringEscapeUtils.unescapeJavaScript(escapedString);

			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage(), e);
				errorMessage = e.getMessage();
			}

			return "An error (\"" + errorMessage
					+ "\") happenned in the query. Please see logs.";
		}

		@Override
		public void run() {
			// upload File and get server response
			serverResponse = getBusTimings();
			// dismiss the dialog
			Log.d(LOG_TAG, "Done doing my stuff. " + "\nGot response: "
					+ serverResponse
					+ "Sending message for dismissing dialog now.");

			uiHandler.post(showServerResponse);
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