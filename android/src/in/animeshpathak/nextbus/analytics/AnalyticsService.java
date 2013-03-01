package in.animeshpathak.nextbus.analytics;

import android.app.IntentService;
import android.content.Intent;

public class AnalyticsService extends IntentService {

	public AnalyticsService() {
		super("AnalyticsService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Analytics.getInstance().pushInternal();
	}
}
