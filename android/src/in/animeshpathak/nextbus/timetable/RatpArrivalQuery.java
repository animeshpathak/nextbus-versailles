package in.animeshpathak.nextbus.timetable;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
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

public class RatpArrivalQuery extends BusArrivalQuery {

	private static String LOG_TAG = "NEXTBUS_RatpArrivalQuery";
	private String RATP_SERVICE_URI = "http://www.ratp.fr/horaires/fr/ratp/bus/prochains_passages/PP/";

	private CharSequence fallbackResult;
	private boolean valid = false;
	private Map<String, BusArrivalInfo> queryResult;

	private static final NamedPattern minutesPattern = NamedPattern
			.compile("[^\\d]*(?<minutes>[0-9]+) mn.*");
	private static final NamedPattern directionPattern = NamedPattern
			.compile(".*Direction : (?<direction>.+)");

	public RatpArrivalQuery(String lineCode, String stopCode) {
		// removing RATP-
		if (!lineCode.startsWith("RATP-") || lineCode.length() < 5) {
			Log.e(LOG_TAG, "Wrong lineCode: " + lineCode);
			return;
		}
		this.lineCode = lineCode.substring(5);
		this.stopCode = stopCode;
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
		binfo.direction = direction.get(0).text();
		NamedMatcher dm = directionPattern.matcher(direction.get(0).text());
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

		binfo.mention = new int[tr.size()];
		binfo.arrivalMillis = new int[tr.size()];

		for (int i = 0; i < tr.size(); i++) {
			Elements td = tr.get(i).getElementsByTag("td");
			if (td == null || td.size() < 2)
				continue;

			binfo.mention[i] = 0;

			if (td.get(0).text().toLowerCase().contains("estime")) {
				binfo.mention[i] |= BusArrivalInfo.MENTION_THEORETICAL;
			} else if (td.get(0).text().toLowerCase().contains("indispo")
					|| td.get(0).text().toLowerCase()
							.contains("pas de service")) {
				binfo.mention[i] |= BusArrivalInfo.MENTION_UNKNOWN;
			}

			NamedMatcher m = minutesPattern.matcher(td.get(1).text());

			if (td.get(0).text().toLowerCase().contains("a l'arret")) {
				binfo.arrivalMillis[i] = 1;
			} else if (m.matches()) {
				String mins = m.group("minutes");
				binfo.arrivalMillis[i] = Integer.parseInt(mins) * 60 * 1000;
				if (binfo.arrivalMillis[i] == 0) {
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
	private String getBusTimings(String suffix) {
		// Create a new HttpClient and Get Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httppost = new HttpGet(RATP_SERVICE_URI + "/" + lineCode + "/"
				+ stopCode + "/" + suffix);

		String errorMessage = "";

		try {
			Log.d(LOG_TAG, "starting POST request now");
			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			Log.d(LOG_TAG, "response received");

			ByteArrayOutputStream myBaos = new ByteArrayOutputStream();

			response.getEntity().writeTo(myBaos);
			return myBaos.toString();

		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			errorMessage = e.getMessage();
		}

		return "An error (\"" + errorMessage
				+ "\") happenned in the query. Please see logs.";
	}

	@Override
	public boolean postQuery() {
		Map<String, BusArrivalInfo> map = new HashMap<String, BusArrivalQuery.BusArrivalInfo>();
		parseResult(map, getBusTimings("A"));
		parseResult(map, getBusTimings("R"));

		this.queryResult = map;
		this.valid = (map != null && map.size() > 0);
		return this.valid;
	}
}
