package au.com.newint.newinternationalist;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pix on 27/02/15.
 */
public class URLCacheStreamFactory extends CacheStreamFactory {
    private final URL sourceURL;

    URLCacheStreamFactory(URL sourceURL) {
        this(sourceURL,null,null);
    }

    // fallback will normally be null
    URLCacheStreamFactory(URL sourceURL, CacheStreamFactory fallback, String name) {
        super(fallback, name==null?"net":name);

        this.sourceURL = sourceURL;
    }

    @Override
    InputStream createCacheInputStream() {

        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) sourceURL.openConnection();
            return urlConnection.getInputStream();
        } catch (IOException e) {
            Log.e("URLCacheStreamFactory", "Error opening source URL: " + sourceURL.toString());
            //e.printStackTrace();
        }
        return null;
    }

    @Override
    OutputStream createCacheOutputStream() {
        return null;
    }

}