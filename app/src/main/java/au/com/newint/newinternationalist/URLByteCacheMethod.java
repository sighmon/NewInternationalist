package au.com.newint.newinternationalist;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Created by pix on 29/01/15.
 */
public class URLByteCacheMethod extends ByteCacheMethod {

    public String name;
    public URL sourceURL;

    public URLByteCacheMethod(URL sourceURL, String name) {
        super(name);
        this.sourceURL = sourceURL;
    }

    public URLByteCacheMethod(URL sourceURL) {
        this(sourceURL, "url");
    }

    public ByteCacheHit read() {

        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) sourceURL.openConnection();
            InputStream urlConnectionInputStream = urlConnection.getInputStream();
            int payloadLength = urlConnection.getContentLength();

            if (payloadLength<=0) payloadLength=1024;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(payloadLength);

            IOUtils.copy(urlConnectionInputStream, baos);

            // make a date object, to turn it into a long, only to be converted back into a date *sigh*
            Date timestamp = new Date(urlConnection.getHeaderFieldDate("Last-modified",new Date().getTime()));

            Log.i("URLByteCacheMethod", "creating ByteCacheHit");
            return new ByteCacheHit(baos.toByteArray(), timestamp);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public void write(byte[] payload) {
        throw new NoSuchMethodError();
    }

}
