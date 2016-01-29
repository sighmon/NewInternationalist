package au.com.newint.newinternationalist;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.ViewGroup;


import java.io.File;
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

    static ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        progressDialog = new ProgressDialog(this);
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

            // Send Google Analytics if the user allows it
            Helpers.sendGoogleAnalytics(getResources().getString(R.string.title_activity_settings));

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

            // If preference change is to external storage, move directory over.
            String externalStorageKey = MainActivity.applicationContext.getResources().getString(R.string.use_external_storage);
            if (key.equals(externalStorageKey)) {
                // Get user choice of storage location
                boolean userRequestsExternalStorage = Helpers.getFromPrefs(externalStorageKey, false);
                File originalDir = Helpers.getStorageDirectory(!userRequestsExternalStorage);
                File destinationDir = Helpers.getStorageDirectory(userRequestsExternalStorage);

                // Alert user to tell them how it went
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                if (originalDir.equals(destinationDir)) {
                    // No external storage.
                    Helpers.debugLog("MoveStorage", "No external storage!");
                    resetPreference(key, !userRequestsExternalStorage);
                    builder.setMessage(R.string.move_storage_no_external_dialog_message).setTitle(R.string.move_storage_no_external_dialog_title);
                    builder.setPositiveButton(R.string.move_storage_dialog_ok_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                } else {
                    // Check if there's space available
                    float spaceNeeded = Helpers.directorySize(originalDir) / 1024 / 1024;
                    float destinationSpaceAvailable = Helpers.bytesAvailable(destinationDir) / 1024 / 1024;
                    Helpers.debugLog("MoveStorage", "Space needed: " + spaceNeeded + "MB. Space available: " + destinationSpaceAvailable + "MB.");
                    boolean spaceAvailable = destinationSpaceAvailable > spaceNeeded;

                    if (spaceAvailable) {
                        // Move magazine data
                        progressDialog.setTitle(getResources().getString(R.string.move_directory_progress_title));
                        progressDialog.setMessage(getResources().getString(R.string.move_directory_progress_message));
                        progressDialog.show();
                        boolean moveSuccessful = Helpers.moveDirectoryToDirectory(originalDir, destinationDir);
                        progressDialog.dismiss();

                        if (moveSuccessful) {
                            // Send success alert
                            Helpers.debugLog("MoveStorage", "Move successful!");
                            builder.setMessage(R.string.move_storage_success_dialog_message).setTitle(R.string.move_storage_success_dialog_title);
                            builder.setPositiveButton(R.string.move_storage_dialog_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();

                            // Reset app to home screen to avoid crashes of changed object file locations
                            Intent intent = new Intent(MainActivity.applicationContext, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } else {
                            // Failed to move files, so reset pref back
                            resetPreference(key, !userRequestsExternalStorage);

                            // Send failure alert
                            Helpers.debugLog("MoveStorage", "FAILED TO MOVE FILES!");
                            builder.setMessage(R.string.move_storage_failed_dialog_message).setTitle(R.string.move_storage_failed_dialog_title);
                            builder.setPositiveButton(R.string.move_storage_dialog_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    } else {
                        // No space available, so switch pref back...
                        resetPreference(key, !userRequestsExternalStorage);

                        // Send failure alert
                        Helpers.debugLog("MoveStorage", "No space available!");
                        builder.setMessage(R.string.move_storage_failed_no_space_dialog_message + " Space needed: " + spaceNeeded + "MB. Space available: " + destinationSpaceAvailable + "MB.").setTitle(R.string.move_storage_failed_no_space_dialog_title);
                        builder.setPositiveButton(R.string.move_storage_dialog_ok_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
            }
        }

        public void resetPreference(String key, boolean originalSetting) {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            Helpers.saveToPrefs(key, originalSetting);
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            // Reset switch back in the UI
            setPreferenceScreen(null);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
