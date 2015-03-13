package au.com.newint.newinternationalist;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by pix on 27/02/15.
 */
public abstract class CacheStreamFactory {
    private final String name;
    protected final CacheStreamFactory fallback;

    CacheStreamFactory(CacheStreamFactory fallback, String name) {
        this.fallback = fallback;
        this.name = name;
    }

    interface CachePreloadCallback {
        void onLoad(InputStream streamCache);
    }

    class PreloadTask extends AsyncTask<String,Integer,Boolean> {

        CachePreloadCallback callback;

        @Override
        protected Boolean doInBackground(String... params) {
            String startingAt = null;
            String stoppingAt = null;
            if(params.length>0) startingAt = params[0];
            if(params.length>1) startingAt = params[1];
            InputStream inputStream = createInputStream(startingAt,stoppingAt);
            try {
                IOUtils.copy(inputStream, new NullOutputStream());
                return Boolean.TRUE;
            } catch (IOException e) {
                //e.printStackTrace();
            }
            return Boolean.FALSE;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(aBoolean.equals(Boolean.TRUE)) {
                InputStream inputStream = createInputStream();
                if (inputStream != null && callback != null) {

                    callback.onLoad(inputStream);

                }
            }
        }
    }

    void preload(CachePreloadCallback callback) {
        PreloadTask preloadTask = new PreloadTask();
        preloadTask.callback = callback;
        preloadTask.execute();
    }

    InputStream createInputStream() {
        return createInputStream(null,null);
    }

    InputStream createInputStream(String startingAt, String stoppingAt) {
        if (stoppingAt != null && stoppingAt.equals(name)) {
            return null;
        }
        if (startingAt == null || startingAt.equals(name)) {
            InputStream cis = createCacheInputStream();
            if (cis != null) {
                return cis;
            } else {
                return wrappedFallbackStream(null,stoppingAt);
            }
        }
        return wrappedFallbackStream(startingAt,stoppingAt);
    }

    // try separating the cache stream generation from the public input stream generation
    abstract InputStream createCacheInputStream();

    abstract OutputStream createCacheOutputStream();

    private InputStream wrappedFallbackStream(String startingAt, String stoppingAt) {
        final InputStream fallbackInputStream = fallback.createInputStream(startingAt,stoppingAt);
        final OutputStream cacheOutputStream = createCacheOutputStream();
        return new InputStream() {

            @Override
            public int read() throws IOException {
                int b = fallbackInputStream.read();
                cacheOutputStream.write(b);
                return b;
            }

        };
    }

    byte[] read() {
        return read(null,null);
    }

    // convenience method to mimick old ByteCache
    byte[] read(String startingAt, String stoppingAt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(this.createInputStream(startingAt, stoppingAt), baos);
        } catch (IOException e) {
            Log.e("CacheStreamFactory", "IOException while reading stream to byte array");
            return baos.toByteArray();
        }
        return null;
    }
}
