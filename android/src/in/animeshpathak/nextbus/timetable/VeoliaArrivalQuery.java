package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;

public class VeoliaArrivalQuery extends BusArrivalQuery {

	private static String LOG_TAG = Constants.LOG_TAG;
	private static String VEOLIA_SERVICE_URI = "http://www.idf.veoliatransdev.com";

	private CharSequence fallbackResult;
	private boolean valid = false;

	private static final NamedPattern minutesPattern = NamedPattern
			.compile("[^\\d]*(?:(?<hours>[0-9]+) heure)?[^\\d]*(?<minutes>[0-9]+) minute.*");
	private static final NamedPattern directionPattern = NamedPattern
			.compile("(?:.*Direction\\s*:\\s*(?:</strong>)?)?\\s*(?<direction>[^/]+).*");

	protected VeoliaArrivalQuery(BusLine bl, BusStop bs) {
		this.busLine = bl;
		this.busStop = bs;
		this.busLineCode = bl.getCode();

		// removing VEOLIA-
		if (!busLineCode.startsWith("VEOLIA-") || busLineCode.length() < 8) {
			Log.e(LOG_TAG, "Wrong lineCode: " + busLineCode);
			return;
		}

		this.busLineCode = busLineCode.substring(7);
		fallbackResult = "";
	}

	private void parseResult(Map<String, BusArrivalInfo> map, String string) {
		Document doc = Jsoup.parse(string);
		Elements prochains_bus = doc.getElementsByClass("prochains_bus");
		if (prochains_bus == null || prochains_bus.size() <= 0) {
			Log.w(LOG_TAG, "<prochains_bus> Class elements not found on html");
			return;
		}

		Elements direction = prochains_bus.get(0).getElementsByClass(
				"direction");
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

		for (int i = 0; i < attenteSpans.size(); i++) {
			Element busArrival = attenteSpans.get(i);
			if (busArrival == null)
				continue;

			int mention = 0;
			int arrivalMillis = 0;

			if (busArrival.className().equals("temps_theorique")) {
				mention |= BusArrivalInfo.MENTION_THEORETICAL;
			}

			String busTimings = busArrival.ownText();
			NamedMatcher m = minutesPattern.matcher(busTimings);

			if (m.matches()) {
				int hoursMs = 0;
				int minMs = 0;
				String hours = m.group("hours");
				String mins = m.group("minutes");
				try {
					if (hours != null) {
						hoursMs = Integer.parseInt(hours) * 60 * 60 * 1000;
					}

					if (mins != null) {
						minMs = Integer.parseInt(mins) * 60 * 1000;
					}
				} catch (NumberFormatException e) {
					Log.w(LOG_TAG, "" + e.getMessage());
					continue;
				}

				arrivalMillis = hoursMs + minMs;
				if (arrivalMillis < 1500 * 60) {
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

	private byte[] getBusTimings(String dirStopCode) {
		try {
			String uri = VEOLIA_SERVICE_URI + "/horaire-arret-" + busLineCode
					+ "-" + dirStopCode;
			HttpResponse response = doHttpGet(uri);
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

		if (busStop != null && !busStop.getCode().contains("-")
				&& busStop.getCode().length() > 0) {
			byte[] d = getBusTimings(busStop.getCode());
			if (d != null)
				responseData.add(d);
		} else {
			String[] dirCodes = busStop.getCode().split("-");
			for (String dirCode : dirCodes) {
				byte[] d = getBusTimings(dirCode);
				if (d != null)
					responseData.add(d);
			}
		}

		ResponseStats stats = new ResponseStats();
		stats.setBusLine(this.busLine);
		stats.setBusStop(this.busStop);
		stats.setResponseMs(System.currentTimeMillis() - initTime);

		if (responseData.size() <= 0) {
			this.valid = false;
			return stats;
		}

		// Parsing the results
		for (byte[] rd : responseData) {
			parseResult(map, new String(rd));
		}

		this.queryResult = map;
		this.valid = (map != null && map.size() > 0);
		stats.setParsingMs(System.currentTimeMillis() - initTime
				- stats.getResponseMs());
		stats.setValid(this.valid);
		return stats;
	}
}
