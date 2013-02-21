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
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.util.Log;

import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;

public class PhebusArrivalQuery extends BusArrivalQuery {

	private static final String[] DIR = { "direction1", "direction2" };
	private static final String[] STAR = { "star1", "star2", "star3", "star4" };
	private static final String[] MIN = { "minute1", "minute2", "minute3",
			"minute4" };
	private static final String[] STATIC = { "static1", "static2", "static3",
			"static4" };
	private static final String[] APP = { "approach1", "approach2",
			"approach3", "approach4" };
	private static final String[] LAST = { "dernier1", "dernier2", "dernier3",
			"dernier4" };

	// Limited to results having 2 arrivals
	private static final NamedPattern patt = NamedPattern.compile("" + ".*"
			+ "Code ligne : [\\w]+[^\\w]+"
			+ "(?:En direction de (?<direction1>[^\\n\\r\\*]+))"
			+ "[^\\d\\*\\w]*(?<dernier1>Dernier bus\\s*)?(?<star1>[\\*]+)?"
			+ "(?:(?:" + "(?<minute1>\\d+) min)|" + "(?<static1>\\d+h\\d+)|"
			+ "(?<approach1>Bus en approche))"
			+ "[^\\d\\*\\w]*(?<dernier2>Dernier bus\\s*)?(?<star2>[\\*]+)?"
			+ "(?:(?:" + "(?<minute2>\\d+) min)|" + "(?<static2>\\d+h\\d+)|"
			+ "(?<approach2>Bus en approche))" + "[^\\w]+" + "(?:"
			+ "(?:En direction de (?<direction2>[^\\n\\r\\*]+))"
			+ "[^\\d\\*\\w]*(?<dernier3>Dernier bus\\s*)?(?<star3>[\\*]+)?"
			+ "(?:(?:" + "(?<minute3>\\d+) min)|" + "(?<static3>\\d+h\\d+)|"
			+ "(?<approach3>Bus en approche))"
			+ "[^\\d\\*\\w]*(?<dernier4>Dernier bus\\s*)?(?<star4>[\\*]+)?"
			+ "(?:(?:" + "(?<minute4>\\d+) min)|" + "(?<static4>\\d+h\\d+)|"
			+ "(?<approach4>Bus en approche))" + ")?" + ".*",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	// Fallback limited to results having 1 arrival
	private static final NamedPattern patt2 = NamedPattern.compile("" + ".*"
			+ "Code ligne : [\\w]+[^\\w]+"
			+ "(?:En direction de (?<direction1>[^\\n\\r\\*]+))"
			+ "[^\\d\\*\\w]*(?<dernier1>Dernier bus\\s*)?(?<star1>[\\*]+)?"
			+ "(?:(?:" + "(?<minute1>\\d+) min)|" + "(?<static1>\\d+h\\d+)|"
			+ "(?<approach1>Bus en approche))" + "[^\\w]+" + "(?:"
			+ "(?:En direction de (?<direction2>[^\\n\\r\\*]+))"
			+ "[^\\d\\*\\w]*(?<dernier3>Dernier bus\\s*)?(?<star3>[\\*]+)?"
			+ "(?:(?:" + "(?<minute3>\\d+) min)|" + "(?<static3>\\d+h\\d+)|"
			+ "(?<approach3>Bus en approche))" + ")?" + ".*",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static String LOG_TAG = "NEXTBUS_PhebusArrivalQuery";
	private String PHEBUS_SERVICE_URI = "http://www.phebus.tm.fr/sites/all/themes/mine/phebus/itineraire/code_temps_reel.php";

	private CharSequence fallbackResult;
	private boolean valid = false;

	protected PhebusArrivalQuery(BusLine lineCode, BusStop stopCode) {
		this.busLine = lineCode;
		this.busStop = stopCode;
		this.busLineCode = busLine.getCode();
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

	/**
	 * Using the regex pattern with named capture-groups to get the data
	 * Unfortunately capture-groups cannot be repetitive in a regular
	 * expression. This is why the capture-group names must be unique.
	 * 
	 * @param queryRes
	 * @return
	 */
	private Map<String, BusArrivalInfo> parseResult(CharSequence queryRes) {
		try {
			Map<String, BusArrivalInfo> m = new HashMap<String, BusArrivalQuery.BusArrivalInfo>();

			String input = queryRes.toString();
			NamedMatcher matcher = patt.matcher(input);

			// Fallback for the case where the result has one bus arrival only
			int nbOfArrivals = 2;
			if (!matcher.matches()) {
				matcher = patt2.matcher(input);
				nbOfArrivals = 1;
			}

			if (matcher.matches()) {
				Map<String, String> groups = matcher.namedGroups();
				if (groups.get(DIR[0]) != null) {
					BusArrivalInfo b1 = createBusArrivalInfo(groups,
							nbOfArrivals, DIR[0], MIN[0], MIN[1], STATIC[0],
							STATIC[1], STAR[0], STAR[1], APP[0], APP[1],
							LAST[0], LAST[1]);
					m.put(b1.direction, b1);
				}
				if (groups.get(DIR[1]) != null) {
					BusArrivalInfo b2 = createBusArrivalInfo(groups,
							nbOfArrivals, DIR[1], MIN[2], MIN[3], STATIC[2],
							STATIC[3], STAR[2], STAR[3], APP[2], APP[3],
							LAST[2], LAST[3]);
					m.put(b2.direction, b2);
				}

				if (m.size() > 0)
					return m;
			}
		} catch (Exception e) {
			Log.d(LOG_TAG, e.getMessage());
		}
		return null;
	}

	/**
	 * Helper method to avoid code duplication
	 * 
	 * @param groups
	 * @param onlyOneBusPerLine
	 * @param dir1
	 * @param min1
	 * @param min2
	 * @param static1
	 * @param static2
	 * @param star1
	 * @param star2
	 * @param app2
	 * @param app1
	 * @param last2
	 * @param last1
	 * @return
	 * @throws ParseException
	 */
	private BusArrivalInfo createBusArrivalInfo(Map<String, String> groups,
			int arrivals, String dir1, String min1, String min2,
			String static1, String static2, String star1, String star2,
			String app1, String app2, String last1, String last2)
			throws ParseException {
		BusArrivalInfo b1 = new BusArrivalInfo();
		b1.direction = groups.get(dir1);
		int[] b1Arrivals = new int[arrivals];
		int[] b1Mentions = new int[arrivals];
		if (groups.get(min1) != null || groups.get(static1) != null
				|| groups.get(app1) != null) {
			b1Mentions[0] |= (groups.get(star1) != null) ? BusArrivalInfo.MENTION_THEORETICAL
					: 0;
			b1Mentions[0] |= (groups.get(last1) != null) ? BusArrivalInfo.MENTION_LAST
					: 0;
			if ((groups.get(app1) != null)) {
				b1Mentions[0] |= (groups.get(app1) != null) ? BusArrivalInfo.MENTION_APPROACHING
						: 0;
				b1Arrivals[0] = 0;
			}
			if (groups.get(min1) != null)
				b1Arrivals[0] = Integer.parseInt(groups.get(min1)) * 60 * 1000;
			if (groups.get(static1) != null)
				b1Arrivals[0] = frenchHourToArrivalMillis(groups.get(static1));
		}
		if (arrivals > 1)
			if (groups.get(min2) != null || groups.get(static2) != null
					|| groups.get(app2) != null) {
				b1Mentions[1] |= (groups.get(star2) != null) ? BusArrivalInfo.MENTION_THEORETICAL
						: 0;
				b1Mentions[1] |= (groups.get(last2) != null) ? BusArrivalInfo.MENTION_LAST
						: 0;
				if ((groups.get(app2) != null)) {
					b1Mentions[1] |= (groups.get(app2) != null) ? BusArrivalInfo.MENTION_APPROACHING
							: 0;
					b1Arrivals[1] = 0;
				}
				if (groups.get(min2) != null)
					b1Arrivals[1] = Integer.parseInt(groups.get(min2)) * 60 * 1000;
				if (groups.get(static2) != null)
					b1Arrivals[1] = frenchHourToArrivalMillis(groups
							.get(static2));
			}
		b1.mention = b1Mentions;
		b1.arrivalMillis = b1Arrivals;
		return b1;
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
	public ResponseStats postQuery() {
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
		CharSequence serverResponseInChars = Html.fromHtml(escapedString, null,
				new TagHandler() {
					@Override
					public void handleTag(boolean opening, String tag,
							Editable output, XMLReader xmlReader) {
						if (tag.equals("td") && !opening)
							output.append("\r\n");
					}
				});
		fallbackResult = removeExcessBlankLines(serverResponseInChars);
		this.queryResult = parseResult(fallbackResult);

		this.valid = (queryResult != null);
		stats.setParsingTime(System.currentTimeMillis() - initTime
				- stats.getResponseTime());
		stats.setValid(this.valid);
		return stats;
	}
}
