package in.animeshpathak.nextbus.favorites;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Favorite implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6076230667905908819L;
	private BusLine lineName;
	private BusStop stopName;

	public Favorite(BusLine line, BusStop stop) {
		this.lineName = line;
		this.stopName = stop;
	}

	public BusLine getLine() {
		return lineName;
	}

	public BusStop getStop() {
		return stopName;
	}

	public static void saveFavorites(Activity context, List<Favorite> favorites)
			throws IOException {
		SharedPreferences prefs = context.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(favorites);
		oos.flush();
		baos.flush();

		editor.putString(Constants.FAVORITE_LIST,
				Base64Mod.encodeToString(baos.toByteArray(), Base64Mod.DEFAULT));
		editor.commit();
	}

	@SuppressWarnings("unchecked")
	public static List<Favorite> readFavorites(Activity context)
			throws IOException {
		SharedPreferences prefs = context.getPreferences(Context.MODE_PRIVATE);
		String listObjStr = prefs.getString(Constants.FAVORITE_LIST, null);

		if (listObjStr == null) {
			return new ArrayList<Favorite>();
		}
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(
					Base64Mod.decode(listObjStr, Base64Mod.DEFAULT));
			ObjectInputStream ois = new ObjectInputStream(bais);

			return (List<Favorite>) ois.readObject();
		} catch (Exception e) {
			Log.e(Constants.LOG_TAG, e.getMessage(), e);
			// Favorites are corrupt. deleting all favorites.
			prefs.edit().putString(Constants.FAVORITE_LIST, null);
			prefs.edit().commit();
			return new ArrayList<Favorite>();
		}
	}

	@Override
	public String toString() {
		return lineName + ":" + stopName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Favorite))
			return false;
		return ((Favorite) obj).toString().equals(this.toString());
	}
}
