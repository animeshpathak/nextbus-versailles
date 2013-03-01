package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;

public class RatpArrivalQuery extends BusArrivalQuery {

	private static String LOG_TAG = Constants.LOG_TAG;
	private String RATP_SERVICE_URI = "http://www.ratp.fr/horaires/fr/ratp/bus/prochains_passages/PP";

	private CharSequence fallbackResult;
	private boolean valid = false;

	private static final NamedPattern minutesPattern = NamedPattern
			.compile("[^\\d]*(?<minutes>[0-9]+) mn.*");
	private static final NamedPattern directionPattern = NamedPattern
			.compile(".*Direction : (?<direction>.+)");

	protected RatpArrivalQuery(BusLine bl, BusStop bs) {
		this.busLine = bl;
		this.busStop = bs;
		this.busLineCode = bl.getCode();

		// removing RATP-
		if (!busLineCode.startsWith("RATP-") || busLineCode.length() < 6) {
			Log.e(LOG_TAG, "Wrong lineCode: " + bl);
			return;
		}

		this.busLineCode = busLineCode.substring(5);
		fallbackResult = "";
	}

	private void parseResult(Map<String, BusArrivalInfo> map, String string) {
		Document doc = Jsoup.parse(string);
		Element prochains_passages = doc.getElementById("prochains_passages");
		if (prochains_passages == null) {
			Log.w(LOG_TAG, "<prochains_passages> ID not found on html");
			return;
		}

		Elements fieldset = prochains_passages.getElementsByClass("bus");
		if (fieldset == null || fieldset.size() == 0) {
			Log.w(LOG_TAG, "<bus> class not found on <prochains_passages>");
			return;
		}

		Elements direction = fieldset.get(0).getElementsByClass("direction");
		if (direction == null || direction.size() == 0) {
			Log.w(LOG_TAG, "<direction> class not found in tag <fieldset>");
			return;
		}

		BusArrivalInfo binfo = new BusArrivalInfo();
		binfo.direction = direction.get(0).ownText();
		NamedMatcher dm = directionPattern.matcher(direction.get(0).ownText());
		if (dm.matches()) {
			binfo.direction = dm.group("direction");
		}

		Elements table = fieldset.get(0).getElementsByTag("table");
		if (table == null || table.size() == 0) {
			Log.w(LOG_TAG, "<table> tag not found in tag <fieldset>");
			return;
		}

		Elements tbody = table.get(0).getElementsByTag("tbody");
		if (tbody == null || tbody.size() == 0) {
			Log.w(LOG_TAG, "<tbody> tag not found in tag <table>");
			return;
		}

		Elements tr = tbody.get(0).getElementsByTag("tr");
		if (tr == null || tr.size() == 0) {
			Log.w(LOG_TAG, "<try> tag not found in tag <tbody>");
			return;
		}

		for (int i = 0; i < tr.size(); i++) {
			int mention = 0;
			int arrivalMillis = 0;

			Elements td = tr.get(i).getElementsByTag("td");
			if (td == null || td.size() < 2)
				continue;

			if (td.get(0).ownText().toLowerCase().contains("estime")) {
				mention |= BusArrivalInfo.MENTION_THEORETICAL;
			} else if (td.get(0).ownText().toLowerCase().contains("indispo")
					|| td.get(0).ownText().toLowerCase()
							.contains("pas de service")) {
				mention |= BusArrivalInfo.MENTION_UNKNOWN;
			}

			NamedMatcher m = minutesPattern.matcher(td.get(1).ownText());

			if (td.get(0).ownText().toLowerCase().contains("a l'arret")) {
				arrivalMillis = 1;
			} else if (m.matches()) {
				String mins = m.group("minutes");
				arrivalMillis = Integer.parseInt(mins) * 60 * 1000;
				if (arrivalMillis == 0) {
					mention |= BusArrivalInfo.MENTION_APPROACHING;
				}
			} else {
				mention |= BusArrivalInfo.MENTION_UNKNOWN;
			}

			binfo.addArrival(arrivalMillis, mention);
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
	private byte[] getBusTimings(String suffix) {
		// Create a new HttpClient and Get Header
		HttpClient httpclient = createHttpClient();
		HttpGet httppost = new HttpGet(RATP_SERVICE_URI + "/" + busLineCode
				+ "/" + busStop.getCode() + "/" + suffix);

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
		byte[] dataDirectionA = getBusTimings("A");
		byte[] dataDirectionR = getBusTimings("R");

		ResponseStats stats = new ResponseStats();
		stats.setBusLine(this.busLine);
		stats.setBusStop(this.busStop);
		stats.setResponseMs(System.currentTimeMillis() - initTime);

		if (dataDirectionA == null || dataDirectionR == null) {
			this.valid = false;
			return stats;
		}

		parseResult(map, new String(dataDirectionA));
		parseResult(map, new String(dataDirectionR));

		this.queryResult = map;
		this.valid = (map != null && map.size() > 0);
		stats.setParsingMs(System.currentTimeMillis() - initTime
				- stats.getResponseMs());
		stats.setValid(this.valid);
		return stats;
	}
}
