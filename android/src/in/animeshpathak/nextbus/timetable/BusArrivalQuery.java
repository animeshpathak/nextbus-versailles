package in.animeshpathak.nextbus.timetable;

import in.animeshpathak.nextbus.R;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.CharacterStyle;
import android.text.style.TextAppearanceSpan;

public abstract class BusArrivalQuery {
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
	public CharSequence getFormattedText(Context c) {
		if (!isValid())
			return "";

		SpannableStringBuilder ssb = new SpannableStringBuilder();
		Set<Entry<String, BusArrivalInfo>> arrivalSet = getNextArrivals()
				.entrySet();
		for (Entry<String, BusArrivalInfo> entry : arrivalSet) {
			appendWithStyle(ssb, c.getString(R.string.bus_text_direction) + " " + entry.getKey() + "\n",
					new TextAppearanceSpan(c, R.style.DefaultBusTextAppearance));
			for (int i = 0; i < entry.getValue().arrivalMillis.length; i++) {
				int totalMillis = entry.getValue().arrivalMillis[i];
				
				ssb.append("\t");
				
				// If bus is not approaching we print the time
				if((entry.getValue().mention[i] & (BusArrivalInfo.MENTION_APPROACHING | BusArrivalInfo.MENTION_UNKNOWN)) == 0){
					appendWithStyle(
							ssb,
							" " + formatTimeDelta(totalMillis),
							new TextAppearanceSpan(c, R.style.BlueBusTextAppearance));
				}
				String mention = "";
				if((entry.getValue().mention[i] & BusArrivalInfo.MENTION_APPROACHING) != 0){
					mention += " " + c.getString(R.string.bus_text_approaching);
				}
				if((entry.getValue().mention[i] & BusArrivalInfo.MENTION_UNKNOWN) != 0){
					mention += " " + c.getString(R.string.bus_text_unavailable);
				}
				if((entry.getValue().mention[i] & BusArrivalInfo.MENTION_THEORETICAL) != 0){
					mention += " " + c.getString(R.string.bus_text_theoretical);
				}
				if((entry.getValue().mention[i] & BusArrivalInfo.MENTION_LAST) != 0){
					mention += " " + c.getString(R.string.bus_text_last);
				}
				appendWithStyle(ssb, mention + "\n",
						new TextAppearanceSpan(c, R.style.RedBusTextAppearance));
			}
			appendWithStyle(ssb, "\n", new TextAppearanceSpan(c,
					R.style.DefaultBusTextAppearance));
		}
		return ssb;
	}
	
	// Thanks to Android for having relative time Localized
	public CharSequence formatTimeDelta(int totalMillis){
		long now = System.currentTimeMillis();
		return DateUtils.getRelativeTimeSpanString(now + totalMillis, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
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

}
