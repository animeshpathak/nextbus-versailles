package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.NotificationService;
import in.animeshpathak.nextbus.R;
import in.animeshpathak.nextbus.timetable.BusArrivalQuery.BusArrivalInfo;
import in.animeshpathak.nextbus.timetable.data.BusLine;
import in.animeshpathak.nextbus.timetable.data.BusStop;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.CharacterStyle;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class BusArrivalQuery {

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
		for (int i = 0; i < busInfo.arrivalMillis.length; i++) {
			int totalMillis = busInfo.arrivalMillis[i];

			ssb.append("\t");

			// If bus is not approaching we print the time
			if ((busInfo.mention[i] & (BusArrivalInfo.MENTION_APPROACHING | BusArrivalInfo.MENTION_UNKNOWN)) == 0) {
				appendWithStyle(
						ssb,
						" " + formatTimeDelta(c, totalMillis),
						new TextAppearanceSpan(c, R.style.BlueBusTextAppearance));
			}
			String mention = "";
			if ((busInfo.mention[i] & BusArrivalInfo.MENTION_APPROACHING) != 0) {
				mention += " " + c.getString(R.string.bus_text_approaching);
			}
			if ((busInfo.mention[i] & BusArrivalInfo.MENTION_UNKNOWN) != 0) {
				mention += " " + c.getString(R.string.bus_text_unavailable);
			}
			if ((busInfo.mention[i] & BusArrivalInfo.MENTION_THEORETICAL) != 0) {
				mention += " " + c.getString(R.string.bus_text_theoretical);
			}
			if ((busInfo.mention[i] & BusArrivalInfo.MENTION_LAST) != 0) {
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
		public int arrivalMillis[];
		public int mention[];
	}

	public SpannableStringBuilder appendWithStyle(
			SpannableStringBuilder builder, CharSequence text, CharacterStyle c) {
		SpannableString styled = new SpannableString(text);
		styled.setSpan(c, 0, styled.length(), 0);
		return builder.append(styled);
	}

	/**
	 * Thanks to Lorne Laliberte
	 * http://codereview.stackexchange.com/questions/3099
	 * /android-remove-useless-whitespace-from-styled-string
	 * 
	 * @param source
	 * @return
	 */
	public static CharSequence removeExcessBlankLines(CharSequence source) {

		if (source == null)
			return "";

		int newlineStart = -1;
		int nbspStart = -1;
		int consecutiveNewlines = 0;
		SpannableStringBuilder ssb = new SpannableStringBuilder(source);
		for (int i = 0; i < ssb.length(); ++i) {
			final char c = ssb.charAt(i);
			if (c == '\n') {
				if (consecutiveNewlines == 0)
					newlineStart = i;

				++consecutiveNewlines;
				nbspStart = -1;
			} else if (c == '\u00A0') {
				if (nbspStart == -1)
					nbspStart = i;
			} else if (consecutiveNewlines > 0) {

				// note: also removes lines containing only whitespace,
				// or nbsp; except at the beginning of a line
				if (!Character.isWhitespace(c) && c != '\u00A0') {

					// we've reached the end
					if (consecutiveNewlines > 2) {
						// replace the many \n with one
						ssb.replace(newlineStart,
								nbspStart > newlineStart ? nbspStart : i, "\n");
						i -= i - newlineStart;
					}

					consecutiveNewlines = 0;
					nbspStart = -1;
				}
			}
		}

		return ssb;
	}

	public BusLine getBusLine() {
		return busLine;
	}

	public BusStop getBusStop() {
		return busStop;
	}
}
