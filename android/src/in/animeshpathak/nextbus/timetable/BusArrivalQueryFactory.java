package in.animeshpathak.nextbus.timetable;

import java.io.IOException;

import org.json.JSONException;

import android.content.Context;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusNetwork;
import in.animeshpathak.nextbus.timetable.data.BusStop;

public class BusArrivalQueryFactory {
	public static BusArrivalQuery getInstance(BusLine bl, BusStop bs){
		if(bl.getCompany().startsWith("RATP")){
			return new RatpArrivalQuery(bl, bs);
		} else if(bl.getCompany().startsWith("VEOLIA")){
			return new VeoliaArrivalQuery(bl, bs);
		} else {
			return new PhebusArrivalQuery(bl, bs);
		}
	}
	
	/**
	 * Get a query instance based on string parameters.
	 * 
	 * @param lineCode
	 * @param stopCode
	 * @param directionName
	 * @return
	 * @throws JSONException 
	 * @throws IOException 
	 */
	public static BusArrivalQuery getInstance(Context context, String lineCode, String stopCode) throws IOException{
		try {
			BusNetwork busNet = BusNetwork.getInstance(context);
			BusLine qLine = busNet.getLineByCode(lineCode);
			BusStop qStop = busNet.getStopByCode(stopCode);
			
			if(qLine == null || qStop == null){
				throw new IOException("Invalid line/stop String parameters. ");
			}
			
			return getInstance(qLine, qStop);
		} catch (JSONException e) {
			throw new IOException(e.getMessage());
		}
	}
}
