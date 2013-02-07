package in.animeshpathak.nextbus.favorites;

import in.animeshpathak.nextbus.Constants;

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

	private static final long serialVersionUID = 0L;
	private String lineName;
	private String stopName;

	public Favorite(String line, String stop) {
		this.lineName = line;
		this.stopName = stop;
	}

	public String getLine() {
		return lineName;
	}

	public String getStop() {
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
