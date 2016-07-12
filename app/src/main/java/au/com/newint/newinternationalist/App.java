package au.com.newint.newinternationalist;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by New Internationalist on 4/06/15.
 */
public class App extends Application {

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
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(MainActivity.applicationContext);
        if (allowAnonymousStatistics) {
            // Now using Firebase.
            firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        } else {
            firebaseAnalytics.setAnalyticsCollectionEnabled(false);
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
