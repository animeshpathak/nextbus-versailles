package in.animeshpathak.nextbus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * The class to show an about screen. Nothing more.
 * 
 * @author pathak
 * 
 */
public class AboutActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SettingsActivity.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		try {
			TextView versionInfoView = (TextView) findViewById(R.id.versionInfoText);
			AssetManager assetManager = getResources().getAssets();
			String versionInfoFile = getString(R.string.versioninfo_asset);
			InputStreamReader reader = new InputStreamReader(
					assetManager.open(versionInfoFile), "UTF-8");
			BufferedReader br = new BufferedReader(reader);
			StringBuffer sbuf = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				sbuf.append(line);
				sbuf.append("\r\n");
			}
			versionInfoView.setText(sbuf.toString());
		} catch (IOException e) {
			Log.e(Constants.LOG_TAG, e.getMessage(), e);
		}
	}
}
