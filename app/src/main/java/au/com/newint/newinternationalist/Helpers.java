package au.com.newint.newinternationalist;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by New Internationalist on 26/02/15.
 */
public class Helpers {

    public static final String LOGIN_USERNAME_KEY = "newintLogin" ;
    public static final String LOGIN_PASSWORD_KEY = "newintHarmless" ;

    public static RoundedBitmapDrawable roundDrawableFromBitmap(Bitmap bitmap) {
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(MainActivity.applicationResources, bitmap);
        roundedBitmapDrawable.setAntiAlias(true);
        roundedBitmapDrawable.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
        return roundedBitmapDrawable;
    }

    public static String getSiteURL() {
        return getVariableFromConfig("SITE_URL");
    }

    public static String getVariableFromConfig(String string) {
        Resources resources = MainActivity.applicationContext.getResources();
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

    public static void saveToPrefs(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.applicationContext);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key,value);
        editor.apply();
    }

    public static String getFromPrefs(String key, String defaultValue) {
        // Default value will be returned of no value found or error occurred.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.applicationContext);
        try {
            return sharedPrefs.getString(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue; // Nothing for key or error, defaultValue returned
        }
    }

    public static void savePassword(String key, String value) {
        // TODO: crypto here
        String encryptedValue = value;
        saveToPrefs(key, encryptedValue);
    }

    public static String getPassword(String key, String defaultValue) {
        return getFromPrefs(key, defaultValue);
    }
}
