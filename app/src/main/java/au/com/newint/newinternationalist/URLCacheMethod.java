package au.com.newint.newinternationalist;

import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pix on 29/01/15.
 */
public abstract class URLCacheMethod<PayloadType> extends CacheMethod<PayloadType>{

    public String name;
    public URL sourceURL;

    public URLCacheMethod(URL sourceURL, String name) {
        this.name = name;
        this.sourceURL = sourceURL;
    }

    public URLCacheMethod(URL sourceURL) {
        this(sourceURL, "net");
    }

    public CacheHit<PayloadType> read() {

        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) sourceURL.openConnection();
            InputStream urlConnectionInputStream = urlConnection.getInputStream();
            

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
