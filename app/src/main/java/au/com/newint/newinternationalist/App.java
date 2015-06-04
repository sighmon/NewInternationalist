package au.com.newint.newinternationalist;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.parse.Parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by New Internationalist on 4/06/15.
 */
public class App extends Application {

    // Avoid a crash when you receive a push notification while the app is shut

    @Override
    public void onCreate() {
        super.onCreate();

        // Setup push notifications
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getVariableFromConfig("PARSE_APP_ID"), getVariableFromConfig("PARSE_CLIENT_KEY"));
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
