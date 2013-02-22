package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.util.Log;

import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;

public class PhebusArrivalQuery extends BusArrivalQuery {

	private static String LOG_TAG = "NEXTBUS_PhebusArrivalQuery";
	private static String PHEBUS_SERVICE_URI = "http://www.phebus.tm.fr/sites/all/themes/mine/phebus/itineraire/code_temps_reel.php";

	private CharSequence fallbackResult;
	private boolean valid = false;
	private static final NamedPattern arrivalPattern = NamedPattern
			.compile("[^\\d\\*\\w]*" + "(?<lastbus>Dernier bus\\s*)?"
					+ "(?<theoretical>[\\*]+)?" + "(?:"
					+ "(?:(?<minutes>\\d+) min)|" + "(?<static>\\d+h\\d+)|"
					+ "(?<approaching>Bus en approche)" + ").*");
	private static final NamedPattern directionPattern = NamedPattern
			.compile("(?:.*En direction de)\\s*(?<direction>.+)");

	protected PhebusArrivalQuery(BusLine bl, BusStop bs) {
		this.busLine = bl;
		this.busStop = bs;
		this.busLineCode = bl.getCode();
		fallbackResult = "";
	}

	private void parseResult(Map<String, BusArrivalInfo> map, String string) {
		Document doc = Jsoup.parse(string);
		Elements prochains_bus = doc.getElementsByTag("font");
		if (prochains_bus == null || prochains_bus.size() <= 0) {
			Log.w(LOG_TAG, "<prochains_bus> Tag elements not found on html");
			return;
		}

		for (int i = 0; i < prochains_bus.size(); i++) {
			String directionTxt = prochains_bus.get(i).ownText();
			NamedMatcher m = directionPattern.matcher(directionTxt);
			if (m.matches() && m.group("direction") != null) {
				directionTxt = m.group("direction");
				BusArrivalInfo binfo = new BusArrivalInfo();
				Element dirParent = prochains_bus.get(i).parent();
				List<Node> dirChildren = dirParent.childNodes();
				for (int k = 0; k < dirChildren.size(); k++) {
					if (dirChildren.get(k) == null
							|| !(dirChildren.get(k) instanceof TextNode))
						continue;

					TextNode ch = (TextNode) dirChildren.get(k);
					String arrivalTxt = ch.text();
					NamedMatcher m2 = arrivalPattern.matcher(arrivalTxt);
					if (m2.matches()) {
						if (m2.group("minutes") != null
								|| m2.group("static") != null
								|| m2.group("approaching") != null) {
							int mention = 0;
							int arrMillis = 0;

							mention |= (m2.group("theoretical") != null) ? BusArrivalInfo.MENTION_THEORETICAL
									: 0;
							mention |= (m2.group("lastbus") != null) ? BusArrivalInfo.MENTION_LAST
									: 0;
							if ((m2.group("approaching") != null)) {
								mention |= BusArrivalInfo.MENTION_APPROACHING;
								arrMillis = 0;
							}
							if (m2.group("minutes") != null)
								try {
									arrMillis = Integer.parseInt(m2
											.group("minutes")) * 60 * 1000;
								} catch (NumberFormatException e) {
									mention = BusArrivalInfo.MENTION_UNKNOWN;
									Log.e(LOG_TAG,
											"Could not parse Phebus minute: "
													+ m2.group("minutes"));
								}
							if (m2.group("static") != null)
								try {
									arrMillis = frenchHourToArrivalMillis(m2
											.group("static"));
								} catch (ParseException e) {
									mention = BusArrivalInfo.MENTION_UNKNOWN;
									Log.e(LOG_TAG,
											"Could not parse Phebus french Hour format: "
													+ m2.group("static"));
								}
							binfo.addArrival(arrMillis, mention);
						}
					}
				}

				binfo.direction = directionTxt;
				map.put(binfo.direction, binfo);
			}
		}
	}

	/**
	 * In the Phebus service, the time is either represented in minutes
	 * (relative to bus arrival) e.g., "14 min", or in an absolute "Time-table"
	 * fashion, e.g., "9h31"
	 * 
	 * This method will transform the "time-table" format into a relative
	 * "arrival time"
	 * 
	 * @return
	 * @throws ParseException
	 */
	private int frenchHourToArrivalMillis(String st) throws ParseException {
		DateFormat format = new SimpleDateFormat("hh'h'mm", Locale.FRANCE);
		Calendar currentTime = Calendar.getInstance();
		Calendar foundTime = Calendar.getInstance();
		foundTime.setTime(format.parse(st));
		foundTime
				.set(currentTime.get(Calendar.YEAR),
						currentTime.get(Calendar.MONTH),
						currentTime.get(Calendar.DATE));
		if (foundTime.compareTo(currentTime) < 0) {
			foundTime.add(Calendar.DATE, 1);
		}
		return (int) (foundTime.getTimeInMillis() - System.currentTimeMillis());
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
	private byte[] getBusTimingsData() {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(PHEBUS_SERVICE_URI);

		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			Log.d(LOG_TAG, "Getting info for stop id " + busStop);
			nameValuePairs.add(new BasicNameValuePair("arret", busStop
					.getCode()));
			nameValuePairs.add(new BasicNameValuePair("ligne", busLine
					.getCode()));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

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
		Map<String, BusArrivalInfo> map = new HashMap<String, BusArrivalQuery.BusArrivalInfo>();
		long initTime = System.currentTimeMillis();
		byte[] serverResponse = getBusTimingsData();

		ResponseStats stats = new ResponseStats();
		stats.setBusLine(this.busLine);
		stats.setBusStop(this.busStop);
		stats.setResponseTime(System.currentTimeMillis() - initTime);

		if (serverResponse == null) {
			this.valid = false;
			return stats;
		}

		String escapedString = StringEscapeUtils.unescapeJavaScript(new String(
				serverResponse));
		parseResult(map, escapedString);

		this.queryResult = map;
		this.valid = (map != null && map.size() > 0);

		if (!this.valid) {
			this.fallbackResult = Html.fromHtml(escapedString, null,
					new TagHandler() {
						@Override
						public void handleTag(boolean opening, String tag,
								Editable output, XMLReader xmlReader) {
							if (tag.equals("td") && !opening)
								output.append("\r\n");
						}
					});
		}

		stats.setParsingTime(System.currentTimeMillis() - initTime
				- stats.getResponseTime());
		stats.setValid(this.valid);
		return stats;
	}
}
