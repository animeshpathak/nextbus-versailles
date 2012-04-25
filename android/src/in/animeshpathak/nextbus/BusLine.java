package in.animeshpathak.nextbus;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BusLine {
	private String[] stopNameArray = {};
	private String[] stopCodeArray = {};
	
	private String name;
	
	public static BusLine parseJson(String lineName, String jsonString) throws JSONException{
		List<String> codes = new LinkedList<String>();
		List<String> names = new LinkedList<String>();
		
		JSONObject json = new JSONObject(jsonString);
		JSONArray jsonValue = json.getJSONArray("value");
		JSONArray jsonText = json.getJSONArray("text");
		
		int nbStops = Math.min(jsonValue.length(), jsonText.length());
		
		for (int i = 0; i < nbStops; i++) {
			String code = jsonValue.getString(i);
			String name = jsonText.getString(i);
			
			if(code != null && name != null && code.trim().length() > 0){
				codes.add(code.split("/")[0]);
				names.add(name);
			}
		}
		
		BusLine bl = new BusLine();
		bl.name = lineName;
		bl.stopCodeArray = codes.toArray(new String[codes.size()]);
		bl.stopNameArray = names.toArray(new String[names.size()]);
		
		return bl;
	}
	
	public String getName(){
		return name;
	}
	
	public String[] getNameArray(){
		return stopNameArray;
	}
	
	public String[] getCodeArray(){
		return stopCodeArray;
	}

	public static BusLine parseJson(String lineName, InputStream is) throws IOException, JSONException {
		// We guarantee that the available method returns the total
        // size of the asset...  of course, this does mean that a single
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
}
