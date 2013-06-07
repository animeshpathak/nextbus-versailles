package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.NotificationService;
import in.animeshpathak.nextbus.R;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.CharacterStyle;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class BusArrivalQuery {

	private static String LOG_TAG = Constants.LOG_TAG;
	private View contentView = null;

	protected BusLine busLine;
	protected BusStop busStop;

	protected String busLineCode;
	protected Map<String, BusArrivalInfo> queryResult;

	/**
	 * Executes the HTTP POSTs. This is the "heavy" method that should be called
	 * in a background thread.
	 * 
	 * @return
	 */
	public abstract ResponseStats postQuery();

	/**
	 * Verifies that this query result is valid
	 * 
	 * @return True if valid
	 */
	public abstract boolean isValid();

	/**
	 * Returns the query result in the default formatting
	 * 
	 * @return Formatted query result
	 */
	public CharSequence getFormattedText(Context c, BusArrivalInfo busInfo) {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		appendWithStyle(ssb, c.getString(R.string.bus_text_direction) + " "
				+ busInfo.direction + "\n", new TextAppearanceSpan(c,
				R.style.DefaultBusTextAppearance));
		for (int i = 0; i < busInfo.size(); i++) {
			int totalMillis = busInfo.getMillis(i);

			ssb.append("\t");

			// If bus is not approaching we print the time
			if ((busInfo.getMention(i) & (BusArrivalInfo.MENTION_APPROACHING | BusArrivalInfo.MENTION_UNKNOWN)) == 0) {
				appendWithStyle(
						ssb,
						" " + formatTimeDelta(c, totalMillis),
						new TextAppearanceSpan(c, R.style.BlueBusTextAppearance));
			}
			String mention = "";
			if ((busInfo.getMention(i) & BusArrivalInfo.MENTION_APPROACHING) != 0) {
				mention += " " + c.getString(R.string.bus_text_approaching);
			}
			if ((busInfo.getMention(i) & BusArrivalInfo.MENTION_UNKNOWN) != 0) {
				mention += " " + c.getString(R.string.bus_text_unavailable);
			}
			if ((busInfo.getMention(i) & BusArrivalInfo.MENTION_THEORETICAL) != 0) {
				mention += " " + c.getString(R.string.bus_text_theoretical);
			}
			if ((busInfo.getMention(i) & BusArrivalInfo.MENTION_LAST) != 0) {
				mention += " " + c.getString(R.string.bus_text_last);
			}
			appendWithStyle(ssb, mention + "\n", new TextAppearanceSpan(c,
					R.style.RedBusTextAppearance));
		}
		appendWithStyle(ssb, "\n", new TextAppearanceSpan(c,
				R.style.DefaultBusTextAppearance));
		// removing last new-line
		if (ssb.length() > 0)
			ssb.delete(ssb.length() - 1, ssb.length());
		return ssb;
	}

	public View getInflatedView(final Context context) {
		if (contentView == null)
			contentView = getInitialView(context);

		LinearLayout txtContent = (LinearLayout) contentView
				.findViewById(R.id.textArrBoxContent);

		if (isValid()) {
			Set<Entry<String, BusArrivalInfo>> arrivalSet = getNextArrivals()
					.entrySet();
			for (final Entry<String, BusArrivalInfo> entry : arrivalSet) {
				TextView theText = new TextView(context);
				theText.setText(getFormattedText(context, entry.getValue()));
				txtContent.addView(theText);
				theText.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						// Line+Stop+Direction
						String lsd = busLine.getCode() + busStop.getCode()
								+ entry.getValue().direction;
						if (NotificationService.notifExists(lsd)) {
							return true;
						}

						Intent notifIntent = new Intent(context,
								NotificationService.class);
						notifIntent.putExtra("LineStopDirection", lsd);
						notifIntent.putExtra("direction",
								entry.getValue().direction);
						NotificationService.putQuery(lsd, BusArrivalQuery.this);
						context.startService(notifIntent);
						return true;
					}
				});
			}

		} else {
			TextView theText = new TextView(context);
			theText.setText(getFallbackText());
			txtContent.addView(theText);
			contentView.setBackgroundColor(0xFF6E1800);
		}
		return contentView;
	}

	public View getInitialView(Context context) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		contentView = inflater.inflate(R.layout.bus_arrival_box, null);
		TextView txtLine = (TextView) contentView
				.findViewById(R.id.textArrBoxLine);
		txtLine.setText(busLineCode);
		return contentView;
	}

	// Thanks to Android for having relative time Localized
	public static CharSequence formatTimeDelta(Context c, int totalMillis) {
		long now = System.currentTimeMillis();
		if (totalMillis < DateUtils.HOUR_IN_MILLIS) {
			return DateUtils.getRelativeTimeSpanString(now + totalMillis, now,
					DateUtils.MINUTE_IN_MILLIS,
					DateUtils.FORMAT_ABBREV_RELATIVE);
		} else {
			return DateUtils.getRelativeTimeSpanString(c, now + totalMillis);
		}
	}

	/**
	 * In case validation fails, or whatever other reason, use this
	 * 
	 * @return
	 */
	public abstract CharSequence getFallbackText();

	/**
	 * @return A Map of direction and BusArrivalInfo
	 */
	public abstract Map<String, BusArrivalQuery.BusArrivalInfo> getNextArrivals();

	public class BusArrivalInfo {
		public static final int MENTION_THEORETICAL = 1;
		public static final int MENTION_APPROACHING = 2;
		public static final int MENTION_LAST = 4;
		public static final int MENTION_UNKNOWN = 8;

		public String direction;
		private List<Integer> arrivalMillis = new ArrayList<Integer>();
		private List<Integer> mentions = new ArrayList<Integer>();

		public void addArrival(int arrMillis, int mention) {
			arrivalMillis.add(arrMillis);
			mentions.add(mention);
		}

		public int size() {
			return arrivalMillis.size();
		}

		public int getMention(int i) {
			return mentions.get(i);
		}

		public int getMillis(int i) {
			return arrivalMillis.get(i);
		}
	}

	public SpannableStringBuilder appendWithStyle(
			SpannableStringBuilder builder, CharSequence text, CharacterStyle c) {
		SpannableString styled = new SpannableString(text);
		styled.setSpan(c, 0, styled.length(), 0);
		return builder.append(styled);
	}

	public BusLine getBusLine() {
		return busLine;
	}

	public BusStop getBusStop() {
		return busStop;
	}
	
	public static HttpClient createHttpClient(){
		final HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, Constants.HTTP_TIMEOUT_MILLIS);
	    HttpConnectionParams.setSoTimeout(httpParams, Constants.HTTP_TIMEOUT_MILLIS);
		HttpClient httpclient = new DefaultHttpClient(httpParams);
		return httpclient;
	}
	
	// This executes a POST and gets the actual info from the website
	// Thanks to http://www.androidsnippets.org/snippets/36/
	protected HttpResponse doHttpPost(String uri, List<NameValuePair> params) throws IOException{
		HttpClient httpclient = createHttpClient();
		HttpPost httppost = new HttpPost(uri);
		Log.d(LOG_TAG, String.format("POST request for %s - %s", busLine, busStop));
		httppost.setEntity(new UrlEncodedFormEntity(params));
		HttpResponse response = httpclient.execute(httppost);
		Log.d(LOG_TAG, String.format("Response for %s - %s", busLine, busStop));
		return response;
	}
	
	protected HttpResponse doHttpGet(String uri) throws IOException{
		HttpClient httpclient = createHttpClient();
		HttpGet httpget = new HttpGet(uri);
		Log.d(LOG_TAG, String.format("GET request for %s - %s", busLine, busStop));
		HttpResponse response = httpclient.execute(httpget);
		Log.d(LOG_TAG, String.format("Response for %s - %s", busLine, busStop));
		return response;
	}
}
