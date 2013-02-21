package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.timetable.data.BusLine;
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
}
