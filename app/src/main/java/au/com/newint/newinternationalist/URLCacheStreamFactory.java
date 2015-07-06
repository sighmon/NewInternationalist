package au.com.newint.newinternationalist;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by pix on 27/02/15.
 */
public class URLCacheStreamFactory extends CacheStreamFactory {
    //private final URL sourceURL;
    private final HttpUriRequest sourceURIRequest;

    URLCacheStreamFactory(URL sourceURL) {
        this(sourceURL,null,null);
    }

    //TODO: can this be DRYer without angering the compiler?

    // fallback will normally be null
    URLCacheStreamFactory(URL sourceURL, CacheStreamFactory fallback, String name) {
        super(fallback, name == null ? "net" : name);

        HttpUriRequest httpUriRequest;

        try {
            httpUriRequest = new HttpGet(sourceURL.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            httpUriRequest = null;
        }

        this.sourceURIRequest = httpUriRequest;
    }

    URLCacheStreamFactory(HttpUriRequest sourceURIRequest, CacheStreamFactory fallback, String name) {
        super(fallback, name==null?"net":name);

        this.sourceURIRequest = sourceURIRequest;
    }

    @Override
    public String toString() {
        return "URLCacheStreamFactory["+sourceURIRequest+"]";
    }

    // Q: should we automatically fail if on the UI thread? is returning null enough?
    // eg. catch (NetworkOnMainThreadException e)
    // A: bad idea unless we need it, as it might hide design flaws
    @Override
    protected InputStream createCacheInputStream() {
        Log.i("URLCacheStreamFactory", "createCacheInputStream() ["+sourceURIRequest+"]");

        HttpURLConnection urlConnection = null;

        if (sourceURIRequest==null) {
            Log.e("URLCacheStreamFactory","sourceURIRequest is null");
            return null;
        }

        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpContext ctx = new BasicHttpContext();
            // Hack while we move away from deprecated DefaultHttpClient:
            // Don't save the cookies here, as they're only for GET requests
            if (sourceURIRequest.getMethod().equals("POST")) {
                ctx.setAttribute(ClientContext.COOKIE_STORE, Publisher.INSTANCE.cookieStore);
            }
            HttpResponse response = httpclient.execute(sourceURIRequest,ctx);
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode>=200 && statusCode<300) {
                return response.getEntity().getContent();
            } else {
                Log.e("ArticleBody", "Failed with code: " + statusCode + " and response: " + response.getStatusLine());
                return null;
            }

        } catch (IOException e) {
            Log.e("URLCacheStreamFactory", "Error opening source URL: " + sourceURIRequest.toString());
            //e.printStackTrace();
        }
        return null;
    }

    @Override
    protected OutputStream createCacheOutputStream() {
        return null;
    }

    @Override
    protected void invalidateCache() {
    }

}