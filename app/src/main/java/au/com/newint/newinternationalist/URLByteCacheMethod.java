package au.com.newint.newinternationalist;

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
public abstract class URLByteCacheMethod extends ByteCacheMethod {

    public String name;
    public URL sourceURL;

    public URLByteCacheMethod(URL sourceURL, String name) {
        this.name = name;
        this.sourceURL = sourceURL;
    }

    public URLByteCacheMethod(URL sourceURL) {
        this(sourceURL, "net");
    }

    public ByteCacheHit read() {

        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) sourceURL.openConnection();
            InputStream urlConnectionInputStream = urlConnection.getInputStream();
            int payloadLength = urlConnection.getContentLength();

            if (payloadLength>=0) payloadLength=1024;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(payloadLength);

            IOUtils.copy(urlConnectionInputStream, baos);

            // TODO: not 0, but now.. sigh
            Date timestamp = new Date(urlConnection.getHeaderFieldDate("Last-modified"),0);

            return new ByteCacheHit(baos.toByteArray())

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
