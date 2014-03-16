package de.janbo.agendawatchface.plugins.notifications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

import de.janbo.agendawatchface.api.AgendaItem;
import de.janbo.agendawatchface.api.TimeDisplayType;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class AgendaNotificationService extends NotificationListenerService {
	public static final String INTENT_ACTION_REFRESH = "de.janbo.agendawatchface.plugins.notifications.intent.action.refresh";
	private boolean vibrate = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null)
			return super.onStartCommand(intent, flags, startId);
		
		int returnVal = super.onStartCommand(intent, flags, startId);
		if (INTENT_ACTION_REFRESH.equals(intent.getAction())) {
			publishNotificationsToAgenda();
		}
		return returnVal;
	}

	private void publishNotificationsToAgenda() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		ArrayList<String> rules = new ArrayList<String>(prefs.getStringSet("rules", new HashSet<String>()));
		Collections.sort(rules);
		
		//Compile regex patterns for inclusion
		ArrayList<Pattern> patterns = new ArrayList<Pattern>();
		for (int i=0; i<rules.size(); i++)
			patterns.add(Pattern.compile(".*("+prefs.getString("pref_rule_"+rules.get(i)+"_package_regex", "")+").*", Pattern.CASE_INSENSITIVE));
		
		//Collect notification items
		ArrayList<AgendaItem> items = new ArrayList<AgendaItem>();
		NotificationProvider provider = new NotificationProvider();
		try {
			for (StatusBarNotification notification : getActiveNotifications()) {
				//Choose first fitting rule
				String rule = "noRule";
				int i;
				for (i=0;i<rules.size();i++) {
					rule = rules.get(i);
					if (patterns.get(i).matcher(notification.getPackageName()).matches()) //rule doesn't fit
						break;
				}
				
				//Handle notification according to rule
				if (i<rules.size()) { //i and rule contain first matching rule
					if (prefs.getString("pref_rule_"+rule+"_action", "show").equals("ignore"))
						continue;
					
					AgendaItem item = new AgendaItem(provider.getPluginId());
					Bundle extras = notification.getNotification().extras;
					item.line1.text = extras.getCharSequence(Notification.EXTRA_TITLE, "").toString();
					item.line1.timeDisplay = TimeDisplayType.NONE;
					item.line1.text = item.line1.text.replace('\n', ' ');
					
					if (!extras.getCharSequence(Notification.EXTRA_TEXT, "").toString().isEmpty()) {
						item.line2 = new AgendaItem.Line();
						item.line2.text = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString();
						item.line2.timeDisplay = TimeDisplayType.NONE;
						item.line2.text = item.line2.text.replace('\n', ' '); //Pebble doesn't seem to handle newlines well...
					}
					
					item.priority = 100;
					items.add(item);
				}
			}

			provider.publishData(getApplicationContext(), items, vibrate); //vibrates only if something changed
			vibrate = false; //don't vibrate next time unless a new notification is posted. 
		} catch (RuntimeException e) {
			Log.e("AgendaNotificationService", "Error retrieving notifications. Try re-allowing the app to read notifications", e);
			AgendaItem errorItem = new AgendaItem(provider.getPluginId());
			errorItem.line1.text = "Plugin needs notification access";
			errorItem.line2 = new AgendaItem.Line();
			errorItem.line2.text = "Please visit the settings";
			errorItem.line1.timeDisplay = errorItem.line2.timeDisplay = TimeDisplayType.NONE;
			
			provider.publishData(getApplicationContext(), new ArrayList<AgendaItem>(Arrays.asList(new AgendaItem[] {errorItem})), false);
		}
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		vibrate = PreferenceManager.getDefaultSharedPreferences(getApplication()).getBoolean("pref_key_vibrate", false); //want to vibrate if this causes a change in the notification data
		publishNotificationsToAgenda();
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		publishNotificationsToAgenda();
	}

}
