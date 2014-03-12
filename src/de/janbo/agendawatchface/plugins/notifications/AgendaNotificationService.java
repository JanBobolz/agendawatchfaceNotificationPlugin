package de.janbo.agendawatchface.plugins.notifications;

import java.util.ArrayList;
import java.util.regex.Pattern;

import de.janbo.agendawatchface.api.AgendaItem;
import de.janbo.agendawatchface.api.TimeDisplayType;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class AgendaNotificationService extends NotificationListenerService {
	public static final String INTENT_ACTION_REFRESH = "de.janbo.agendawatchface.plugins.notifications.intent.action.refresh";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int returnVal = super.onStartCommand(intent, flags, startId);
		if (INTENT_ACTION_REFRESH.equals(intent.getAction())) {
			publishNotificationsToAgenda();
		}
		return returnVal;
	}

	private void publishNotificationsToAgenda() {
		//Compile regex pattern for inclusion
		Pattern pattern = Pattern.compile(".*(whatsapp|kaiten).*", Pattern.CASE_INSENSITIVE);
		
		//Collect notification items
		ArrayList<AgendaItem> items = new ArrayList<AgendaItem>();
		NotificationProvider provider = new NotificationProvider();
		try {
			for (StatusBarNotification notification : getActiveNotifications()) {
				if (!pattern.matcher(notification.getPackageName()).matches()) //skip those that don't match
					continue;
				
				AgendaItem item = new AgendaItem(provider.getPluginId());
				if (notification.getNotification().tickerText != null)
					item.line1.text = notification.getNotification().tickerText.toString();
				else
					item.line1.text = notification.getNotification().extras.getString(Notification.EXTRA_TITLE, "")+": "+notification.getNotification().extras.getString(Notification.EXTRA_TEXT, "");
				item.line1.timeDisplay = TimeDisplayType.NONE;
				item.line2 = null;

				items.add(item);
			}

			provider.publishData(getApplicationContext(), items);
		} catch (RuntimeException e) {
			Log.e("AgendaNotificationService", "Error retrieving notifications. Try re-allowing the app to read notifications", e);
			provider.publishData(getApplicationContext(), new ArrayList<AgendaItem>());
		}
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		publishNotificationsToAgenda();
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		publishNotificationsToAgenda();
	}

}
