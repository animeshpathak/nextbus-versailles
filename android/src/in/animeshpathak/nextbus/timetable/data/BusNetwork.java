package in.animeshpathak.nextbus.timetable.data;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

/**
 * Class encapsulating methods for managing bus network meta-data (lines, stops,
 * etc...)
 */
public class BusNetwork {
	// (key: line-code, value: line-object)
	private Map<String, BusLine> busLines = new TreeMap<String, BusLine>();

	// (key: stop-code, value: stop-object)
	private Map<String, BusStop> busStops = new TreeMap<String, BusStop>();
	
	private static AtomicReference<BusNetwork> singleton = new AtomicReference<BusNetwork>();
	
	public static BusNetwork getInstance(Context context) throws IOException, JSONException{
		// instantiate if null
		singleton.compareAndSet(null, new BusNetwork(context));
		return singleton.get();
	}

	private BusNetwork(Context context) throws IOException, JSONException {
		Resources resources = context.getResources();
		AssetManager assetManager = resources.getAssets();
		InputStream inputStream;
		inputStream = assetManager.open(Constants.BUSLINES_PROPERTIES);
		Properties properties = new Properties();
		properties.load(inputStream);

		for (Object lineName : properties.keySet()) {
			Map<String, BusStop> lineStops = parseJson((String) lineName,
					assetManager.open((String) properties.get(lineName)));
			BusLine busLine = new BusLine((String) lineName, (String) lineName,
					lineStops.values());
			busLines.put((String) lineName, busLine);
		}

		String allLineName = String.format("* (%s)",
				context.getString(R.string.wildcard_bus_line));
		busLines.put("*", new BusLine(allLineName, "*", busStops.values()));
	}
	
	/**
	 * @return Returns an ordered list of all bus stops in the network
	 */
	public List<BusStop> getStops() {
		List<BusStop> orderedStops = new ArrayList<BusStop>(busStops.values());
		Collections.sort(orderedStops);
		return orderedStops;
	}

	/**
	 * @return Returns an ordered list of all bus lines in the network
	 */
	public List<BusLine> getLines() {
		List<BusLine> orderedLines = new ArrayList<BusLine>(busLines.values());
		Collections.sort(orderedLines);
		return orderedLines;
	}

	/**
	 * Loads new Stops from JSON files. Stops are only added to the busStops
	 * (class field) only if they are not already existing.
	 * 
	 * @param lineName
	 * @param jsonString
	 * @return Returns the map of stops associated to the given lineName
	 * @throws JSONException
	 */
	private Map<String, BusStop> parseJson(String lineName, String jsonString)
			throws JSONException {
		Map<String, BusStop> auxStops = new TreeMap<String, BusStop>();
		JSONObject json = new JSONObject(jsonString);
		JSONArray jsonValue = json.getJSONArray("value");
		JSONArray jsonText = json.getJSONArray("text");

		int nbStops = Math.min(jsonValue.length(), jsonText.length());
		for (int i = 0; i < nbStops; i++) {
			String code = jsonValue.getString(i);
			String name = jsonText.getString(i);

			if (code != null && name != null && code.trim().length() > 0) {
				String trimmedCode = code.trim();
				if (code.contains("/")) {
					trimmedCode = code.split("/")[0];
				}
				// never replace stop instances
				if (!busStops.containsKey(trimmedCode)) {
					BusStop bs = new BusStop(name, trimmedCode);
					busStops.put(trimmedCode, bs);
					auxStops.put(trimmedCode, bs);
				} else {
					auxStops.put(trimmedCode, busStops.get(trimmedCode));
				}
			}
		}
		return auxStops;
	}

	public Map<String, BusStop> parseJson(String lineName, InputStream is)
			throws IOException, JSONException {
		// We guarantee that the available method returns the total
		// size of the asset... of course, this does mean that a single
		// asset can't be more than 2 gigs.
		int size = is.available();

		// Read the entire asset into a local byte buffer.
		byte[] buffer = new byte[size];
		is.read(buffer);
		is.close();

		// Convert the buffer into a string.
		String jsonString = StringEscapeUtils.unescapeJava(new String(buffer));
		return parseJson(lineName, jsonString);
	}

	public BusLine getLineByName(String line) {
		if (line.startsWith("*")) {
			return busLines.get("*");
		}
		// Line code and name are the same for now
		return busLines.get(line);
	}
	
	/**
	 * Line code and name are the same for now
	 * @param lineCode
	 * @return
	 */
	public BusLine getLineByCode(String lineCode) {
		return getLineByName(lineCode);
	}

	public BusStop getStopByName(String stop) {
		for (BusStop bs : busStops.values()) {
			if (bs.getName().equals(stop)) {
				return bs;
			}
		}
		return null;
	}
	
	public BusStop getStopByCode(String stopCode) {
		return busStops.get(stopCode);
	}
}
