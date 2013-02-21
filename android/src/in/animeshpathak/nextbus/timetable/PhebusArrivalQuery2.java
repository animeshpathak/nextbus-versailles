package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;

public class PhebusArrivalQuery2 extends BusArrivalQuery {

	private static String LOG_TAG = "NEXTBUS_PhebusArrivalQuery";
	private static String VEOLIA_SERVICE_URI = "http://www.phebus.tm.fr/sites/all/themes/mine/phebus/itineraire/code_temps_reel.php";

	private CharSequence fallbackResult;
	private boolean valid = false;
	private static final NamedPattern minutesPattern = NamedPattern
			.compile("[^\\d]*(?:(?<hours>[0-9]+) heure)?[^\\d]*(?<minutes>[0-9]+) minute.*");
	private static final NamedPattern directionPattern = NamedPattern
			.compile("(?:.*Direction\\s*:\\s*(?:</strong>)?)?\\s*(?<direction>[^/]+).*");

	protected PhebusArrivalQuery2(BusLine bl, BusStop bs)  {
		this.busLine = bl;
		this.busStop = bs;
		this.busLineCode = bl.getCode();
		fallbackResult = "";
	}

	private void parseResult(Map<String, BusArrivalInfo> map, String string) {
		Document doc = Jsoup.parse(string);
		Elements prochains_bus = doc.getElementsByClass("prochains_bus");
		if (prochains_bus == null || prochains_bus.size() <= 0) {
			Log.w(LOG_TAG, "<prochains_bus> Class elements not found on html");
			return;
		}

		Elements direction = prochains_bus.get(0).getElementsByClass("direction");
		if (direction == null || direction.size() <= 0) {
			Log.w(LOG_TAG, "<direction> class not found");
			return;
		}
		
		Elements theDirectionTd = direction.get(0).getElementsByTag("td");
		if (theDirectionTd == null || theDirectionTd.size() <= 0) {
			Log.w(LOG_TAG, "direction <td> TAG not found");
			return;
		}

		BusArrivalInfo binfo = new BusArrivalInfo();
		binfo.direction = theDirectionTd.get(0).ownText();
		NamedMatcher dm = directionPattern.matcher(binfo.direction);
		if (dm.matches()) {
			binfo.direction = dm.group("direction");
		}

		Elements attente = theDirectionTd.get(0).getElementsByClass("attente");
		if (attente == null || attente.size() == 0) {
			Log.w(LOG_TAG, "attente class tags not found");
			return;
		}

		Elements attenteSpans = attente.get(0).getElementsByTag("span");
		if (attenteSpans == null || attenteSpans.size() == 0) {
			Log.w(LOG_TAG, "attenteSpans <span> tags not found");
			return;
		}

		binfo.mention = new int[attenteSpans.size()];
		binfo.arrivalMillis = new int[attenteSpans.size()];

		for (int i = 0; i < attenteSpans.size(); i++) {
			Element busArrival = attenteSpans.get(i);
			if (busArrival == null)
				continue;

			binfo.mention[i] = 0;

			if(busArrival.className().equals("temps_theorique")) {
				binfo.mention[i] |= BusArrivalInfo.MENTION_THEORETICAL;
			}

			String busTimings = busArrival.ownText();
			NamedMatcher m = minutesPattern.matcher(busTimings);

			if (m.matches()) {
				int hoursMs = 0;
				int minMs = 0;
				String hours = m.group("hours");
				String mins = m.group("minutes");
				try{
					if(hours != null){
						hoursMs = Integer.parseInt(hours) * 60 * 60 * 1000;
					}
					
					if(mins != null){
						minMs = Integer.parseInt(mins) * 60 * 1000;
					}
				} catch (NumberFormatException e){
					Log.w(LOG_TAG, ""+e.getMessage());
					continue;
				}
				
				binfo.arrivalMillis[i] = hoursMs + minMs;
				if (binfo.arrivalMillis[i] < 1500 * 60) {
					binfo.mention[i] |= BusArrivalInfo.MENTION_APPROACHING;
				}
			} else {
				binfo.mention[i] |= BusArrivalInfo.MENTION_UNKNOWN;
			}
		}
		map.put(binfo.direction, binfo);
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public CharSequence getFallbackText() {
		return fallbackResult;
	}

	@Override
	public Map<String, BusArrivalInfo> getNextArrivals() {
		return queryResult;
	}

	// This executes a POST and gets the actual info from the website
	// Thanks to http://www.androidsnippets.org/snippets/36/
	private byte[] getBusTimings(String dirStopCode) {
		// Create a new HttpClient and Get Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httppost = new HttpGet(VEOLIA_SERVICE_URI + "/horaire-arret-" + busLineCode + "-"
				+ dirStopCode);

		String errorMessage = "";

		try {
			Log.d(LOG_TAG, "starting POST request now");
			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			Log.d(LOG_TAG, "response received");

			ByteArrayOutputStream myBaos = new ByteArrayOutputStream();

			response.getEntity().writeTo(myBaos);
			return myBaos.toByteArray();

		} catch (Exception e) {
			Log.e(LOG_TAG, "An error (\"" + e.getMessage()
					+ "\") happenned in the query.", e);
			return null;
		}
	}

	@Override
	public ResponseStats postQuery() {
		long initTime = System.currentTimeMillis();
		Map<String, BusArrivalInfo> map = new HashMap<String, BusArrivalQuery.BusArrivalInfo>();
		
		// Making requests
		List<byte[]> responseData = new ArrayList<byte[]>();
		
		if(busStop != null && !busStop.getCode().contains("-") && busStop.getCode().length() > 0){
			byte[] d = getBusTimings(busStop.getCode());
			if(d != null)
				responseData.add(d);
		} else {
			String[] dirCodes = busStop.getCode().split("-");
			for (String dirCode : dirCodes) {
				byte[] d = getBusTimings(dirCode);
				if(d != null)
					responseData.add(d);
			}
		}
		
		ResponseStats stats = new ResponseStats();
		stats.setBusLine(this.busLine);
		stats.setBusStop(this.busStop);
		stats.setResponseTime(System.currentTimeMillis() - initTime);
		
		if(responseData.size() <= 0){
			this.valid = false;
			return stats;
		}
		
		// Parsing the results
		for (byte[] rd : responseData) {
			parseResult(map, new String(rd));
		}
		
		this.queryResult = map;
		this.valid = (map != null && map.size() > 0);
		stats.setParsingTime(System.currentTimeMillis() - initTime - stats.getResponseTime());
		stats.setValid(this.valid);
		return stats;
	}
}
