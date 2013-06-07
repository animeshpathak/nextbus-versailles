package in.animeshpathak.nextbus;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	/** Called when the activity is first started. */
	@Override
	public void onCreate(Bundle bundle) {
		SettingsActivity.setTheme(this);
		super.onCreate(bundle);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_preferences);

		// ListPreference themeList = (ListPreference)
		// findPreference("notif_ui_theme_list");
		// themeList.setEntries(getThemes()[0]);
		// themeList.setEntryValues(getThemes()[1]);
		// sBindPreferenceSummaryToValueListener.onPreferenceChange(themeList,
		// themeList.getValue());
		// themeList.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		ListPreference notifUpdateFreq = (ListPreference) findPreference("notif_frequency_list");
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				notifUpdateFreq, notifUpdateFreq.getValue());
		notifUpdateFreq
				.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	@TargetApi(value = Build.VERSION_CODES.HONEYCOMB)
	CharSequence[][] getThemes() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			CharSequence[][] themeEntries = {
					{ "Default", "Black", "Light" },
					{ "-1", android.R.style.Theme_Holo + "",
							android.R.style.Theme_Holo_Light + "" } };
			return themeEntries;
		} else {
			CharSequence[][] themeEntries = {
					{ "Default", "Black", "Light" },
					{ "-1", android.R.style.Theme_Black + "",
							android.R.style.Theme_Light + "" } };
			return themeEntries;
		}
	}

	public static void setTheme(Activity act) {
		// SharedPreferences sharedPrefs =
		// PreferenceManager.getDefaultSharedPreferences(act);
		// String theTheme = sharedPrefs.getString("notif_ui_theme_list", null);
		// if(theTheme != null){
		// try{
		// int themeId = Integer.parseInt(theTheme);
		// if(themeId != -1)
		// act.setTheme(themeId);
		// } catch (NumberFormatException nf){
		// // ignoring, and not setting any theme
		// }
		// }
	}
}
