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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
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

	//The response from the server
	private String serverResponse;

	//The textview that holds the view
	private TextView busTimingsView;


	//TODO these arrays can come from XML files
	final String[] stopNameArray = {"Berthier","Bourg","C.Commercial Parly 2","Cisterciens","Europe","Gare des Chantiers","Hôtel de Ville","I.N.R.I.A","Jasmin","La Celle","La Muette","Lamartine","Les Comtesses","Les trois Fontaines","Louis Pelin","Mairie","Moines","Noailles","Notre Dame","Passy","Pl du Marché Notre Dame","Place Simart","Place de la Loi","Ploix","Porchefontaine","Pottier","Providence","Préfecture","Redingote","Réservoirs","Sarraut","Stade A. Brunot",
	};
	//{"I.N.R.I.A","Versailles Chantiers"};
	
	final String[] stopCodeArray = {"BERTH","BOURG","PARLY","CISTE","EUROP","GCHAN","HDVER","INRIA","JASMI","CELLE","MUETT","LMART","FNAC","L3F","PELLI","HORLO","MOINE","NOAIL","NDAME","PASSY","HRICH","PLSIM","PLLOI","PLOIX","PRCHE","POTTI","PVD","PREF","GENDA","RESER","SARRA","BRUNO"
	}; //{"INRIA","GCHAN"};

	//ID into above arrays to find the stop
	private int selectedStopID = 0;
	
	
	/**
	 * The Handler that will run the runnable which will then update the bus timings console
	 */
	final Handler uiHandler =  new Handler();

	final Runnable showServerResponse = new Runnable(){
		public void run(){
			dismissDialog(DIALOG_GETTING_BUS_INFO);

			Log.d(LOG_TAG,"\nUpload Complete. The Server said...\n");
			Log.d(LOG_TAG, serverResponse);

			busTimingsView.append("\nLast Updated at: " 
					+ DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date())
					+ "\nThe server said...\n");
			CharSequence serverResponseInChars = Html.fromHtml(serverResponse);
			busTimingsView.append(serverResponseInChars);
		}
	};


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		busTimingsView = (TextView)findViewById(R.id.bus_timings);

		//get the button
		//set handler to launch a processing dialog
		Button updateButton = (Button)findViewById(R.id.update_button);
		updateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				busTimingsView.setText("");
				//start the new thread
				new BusInfoGetter().start();
				//show the progress dialog
				showDialog(DIALOG_GETTING_BUS_INFO);

			}
		});

		
		Spinner spinner = (Spinner) findViewById(R.id.stop_spinner);
	    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, stopNameArray);
	    	
//	    	ArrayAdapter.createFromResource(
//	            this, R.array.planets_array, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    
	    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> view, View arg1,
					int pos, long id) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, "Position = "+pos);
				Log.d(LOG_TAG,"ID = " +id);
				selectedStopID = (int)id;
				
				Log.d(LOG_TAG,"I hope that the selected item "+view.getItemAtPosition(pos)+" is the same as " 
						+ stopNameArray[selectedStopID]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				//do nothing
			}
	    	
		});
	    
		
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch(id){
		case DIALOG_GETTING_BUS_INFO:
			Dialog d = new ProgressDialog(NextBusMain.this);
			d.setTitle("Getting latest bus timings. Please wait");
			return d;
		default:
			return null;
		}
	}

	class BusInfoGetter extends Thread{

		//This executes a POST and gets the actual info from the website
		//Thanks to http://www.androidsnippets.org/snippets/36/
		private String getBusTimings(){
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://www.phebus.tm.fr/sites/all/themes/mine/phebus/itineraire/code_temps_reel.php");

			String errorMessage = "";
			
			try {
				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				Log.d(LOG_TAG, "Getting info for stop id " + selectedStopID + ", " +stopCodeArray[selectedStopID]);
				nameValuePairs.add(new BasicNameValuePair("arret", stopCodeArray[selectedStopID]));
				nameValuePairs.add(new BasicNameValuePair("ligne", "B"));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				Log.d(LOG_TAG,"starting POST request now");
				// Execute HTTP Post Request
				HttpResponse response = httpclient.execute(httppost);
				Log.d(LOG_TAG,"response received");

				ByteArrayOutputStream myBaos = new ByteArrayOutputStream();

				response.getEntity().writeTo(myBaos);
				String escapedString = myBaos.toString(); 
				return StringEscapeUtils.unescapeJavaScript(escapedString);

			} catch (Exception e) {
				Log.e(LOG_TAG,e.getMessage(),e);
				errorMessage = e.getMessage();
			}

			return "An error (\""+errorMessage+"\") happenned in the query. Please see logs.";
		}

		@Override
		public void run() {
			// upload File and get server response
			serverResponse = getBusTimings();
			//dismiss the dialog
			Log.d(LOG_TAG, "Done doing my stuff. " +
					"\nGot response: " + serverResponse +
			"Sending message for dismissing dialog now.");

			uiHandler.post(showServerResponse);
		}

	}


}