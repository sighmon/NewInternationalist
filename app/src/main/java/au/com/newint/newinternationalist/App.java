package au.com.newint.newinternationalist;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import androidx.multidex.MultiDexApplication;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

/**
 * Created by New Internationalist on 4/06/15.
 */
public class App extends MultiDexApplication {

    // Avoid a crash when you receive a push notification while the app is shut

    // Google Analytics
//    public static GoogleAnalytics analytics;
//    public static Tracker tracker;

    @Override
    public void onCreate() {
        super.onCreate();

        MainActivity.applicationContext = getApplicationContext();
        MainActivity.applicationResources = getResources();

        // Setup push notifications
//        Parse.enableLocalDatastore(this);
//        Parse.initialize(this, getVariableFromConfig("PARSE_APP_ID"), getVariableFromConfig("PARSE_CLIENT_KEY"));

        // Setup google analytics if the user has allowed it
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean allowAnonymousStatistics = sharedPreferences.getBoolean(getResources().getString(R.string.allow_anonymous_statistics_key), true);

        if (allowAnonymousStatistics) {
            // Now using Crashlytics.
            Fabric.with(this, new Crashlytics());
        } else {
            // TODO: do we still need to initialise here?
        }
    }

    private String getVariableFromConfig(String string) {
        Resources resources = getResources();
        AssetManager assetManager = resources.getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(string);
        } catch (IOException e) {
            Log.e("Properties", "Failed to open config property file");
            return null;
        }
    }
}
