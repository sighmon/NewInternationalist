package au.com.newint.newinternationalist;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;


import java.util.List;
import java.util.Map;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    /*** App Preferences Fragment ***/
    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

        }
        @Override
        public void onResume() {
            super.onResume();

            // Set the summary to the current values
            Map<String, ?> sharedPreferencesMap = getPreferenceScreen().getSharedPreferences().getAll();
            for (Map.Entry<String, ?> entry : sharedPreferencesMap.entrySet()) {
                // Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
                Preference pref = findPreference(entry.getKey());
                if (pref instanceof EditTextPreference) {
                    pref.setSummary(entry.getValue().toString());
                }
            }

            // Register for changes in settings
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            Preference pref = findPreference(key);
            // Set summary to be the user-description for the selected value
            if (pref instanceof EditTextPreference) {
                EditTextPreference editTextPref = (EditTextPreference) pref;
                pref.setSummary(editTextPref.getText());
            }
        }
    }
}
