package in.animeshpathak.nextbus.news;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.R;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ImageButton;

public class PhebusNewsLoader extends AsyncTask<Void, Void, String> {
	private static String LOG_TAG = Constants.LOG_TAG;
	public static final String newsUrl = "http://www.phebus.tm.fr/?q=actualites";
	private WebView theWebView;
	private Activity context;
	private static String cachedHtml;

	public PhebusNewsLoader(WebView mainWebView, Activity ctx) {
		this.theWebView = mainWebView;
		this.context = ctx;
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			Document doc = Jsoup.parse(new URL(newsUrl), 5000);
			// Getting the content part of the page
			Element newsScreen = doc.getElementById("form_content");

			// Rewriting all URLs to absolute values
			Elements links = newsScreen.select("a[href]");
			for (Element elemLink : links) {
				elemLink.attr("href", elemLink.absUrl("href"));
			}
			return newsScreen.html();
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return null;
	}

	public String sha1(String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("SHA1");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		return "";
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (cachedHtml != null && theWebView != null) {
			theWebView.loadData(cachedHtml, "text/html", "UTF-8");
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (result == null)
			return;

		ImageButton infoButton = (ImageButton) context
				.findViewById(R.id.phebusinfo_button);
		SharedPreferences prefs = context.getPreferences(Context.MODE_PRIVATE);
		String phebusInfoStoredHash = prefs.getString(
				Constants.PHEBUS_INFO_HASH, "");
		String phebusInfoCurrentHash = sha1(result);

		if (theWebView != null) {
			if (!result.equals(cachedHtml))
				theWebView.loadData(result, "text/html", "UTF-8");
			infoButton.setImageResource(R.drawable.info_selector);
			// storing the updated hash value
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(Constants.PHEBUS_INFO_HASH, phebusInfoCurrentHash);
			editor.commit();
		} else if (!phebusInfoCurrentHash.equals(phebusInfoStoredHash)) {
			// the hash did not match <=> Phebus updated their alerts page
			infoButton.setImageResource(R.drawable.info_selector_red);
		}
		cachedHtml = result;
		super.onPostExecute(result);
	}

}
