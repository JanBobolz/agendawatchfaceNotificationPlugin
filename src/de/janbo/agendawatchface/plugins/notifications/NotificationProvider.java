package de.janbo.agendawatchface.plugins.notifications;

import android.content.Context;
import android.content.Intent;
import de.janbo.agendawatchface.api.AgendaWatchfacePlugin;

public class NotificationProvider extends AgendaWatchfacePlugin {

	@Override
	public String getPluginId() {
		return "de.janbo.agendawatchface.plugins.notifications";
	}

	@Override
	public String getPluginDisplayName() {
		return "Notifications";
	}

	@Override
	public void onRefreshRequest(Context context) { //we delegate the fetching of AgendaItems to the NotificationService
		Intent intent = new Intent(context, AgendaNotificationService.class);
		intent.setAction(AgendaNotificationService.INTENT_ACTION_REFRESH);
		context.startService(intent);
	}

	@Override
	public void onShowSettingsRequest(Context context) {
		Intent intent = new Intent(context, SettingsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}
