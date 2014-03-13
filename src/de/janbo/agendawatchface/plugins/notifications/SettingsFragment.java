package de.janbo.agendawatchface.plugins.notifications;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsFragment extends PreferenceFragment {
	int nextRuleNum = 0;
	PreferenceScreen root = null;
	SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			Intent intent = new Intent(getActivity().getApplicationContext(), AgendaNotificationService.class);
			intent.setAction(AgendaNotificationService.INTENT_ACTION_REFRESH);
			getActivity().startService(intent);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		root = getPreferenceScreen();
		final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		
		nextRuleNum = prefs.getInt("last_rule_num", 1)+1;
		findPreference("pref_key_add_rule").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Set<String> stringset = prefs.getStringSet("rules", new HashSet<String>());
				stringset.add(String.valueOf(nextRuleNum));
				
				Editor edit = prefs.edit();
				edit.putStringSet("rules", stringset);
				edit.putInt("last_rule_num", nextRuleNum++);
				edit.commit();
				
				recreateRules();
				return true;
			}
		});
		
		recreateRules();
	}
	
	private void recreateRules() {
		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		Set<String> stringset = prefs.getStringSet("rules", new HashSet<String>());
		String[] rules = new String[stringset.size()];
		rules = stringset.toArray(rules);
		Arrays.sort(rules);
		//Now, "rules" contains the rules from the settings sorted and ready. Add Preferences for them
		PreferenceCategory rule_category = (PreferenceCategory) findPreference("pref_category_rules");
		rule_category.removeAll();
		for (String rule : rules) {
			PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());
			screen.setDefaultValue(Boolean.TRUE);
			screen.setTitle(prefs.getString("pref_rule_"+rule+"_name", "no name"));
			populatePrefScreen(screen, rule);

			rule_category.addPreference(screen);
		}
	}
	
	private void populatePrefScreen(final PreferenceScreen screen, final String rule) {
		final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		
		//Name
		EditTextPreference namePref = new EditTextPreference(getActivity());
		namePref.setKey("pref_rule_"+rule+"_name");
		namePref.setTitle("Name");
		namePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				prefs.edit().putString("pref_rule_"+rule+"_name", (String) newValue).apply();
				recreateRules();
				return true;
			}
		});
		screen.addPreference(namePref);
		
		//Delete rule
		Preference removePref = new Preference(getActivity());
		removePref.setTitle("Remove rule");
		removePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Set<String> stringset = prefs.getStringSet("rules", new HashSet<String>());
				stringset.remove(rule);
				
				Editor edit = prefs.edit();
				edit.putStringSet("rules", stringset);
				edit.apply();
				
				recreateRules();
				
				screen.getDialog().dismiss();
				return true;
			}
		});
		screen.addPreference(removePref);
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
		recreateRules();
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
	}
}
