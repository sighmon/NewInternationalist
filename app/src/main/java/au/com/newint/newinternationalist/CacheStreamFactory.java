package au.com.newint.newinternationalist;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

    private PreloadTask preloadTask;

    CacheStreamFactory(CacheStreamFactory fallback, String name) {
        Log.i(this.getClass().getSimpleName(), "creating factory of type " + name + ", fallback is " + ((fallback != null) ? "not" : "") + " null");
        preloadTask = new PreloadTask();
        this.fallback = fallback;
        this.name = name;
    }

    interface CachePreloadCallback {
        void onLoad(InputStream streamCache);
    }

    class PreloadTask extends AsyncTask<Object,Integer,CachePreloadCallback> {

        @Override
        protected CachePreloadCallback doInBackground(Object... params) {

            CachePreloadCallback callback = null;
            String startingAt = null;
            String stoppingAt = null;
            Object lock;
            if (params.length > 0) {
                lock = params[0];
            } else {
                return null;
            }
            synchronized (lock) {
                if (params.length > 1) {
                    callback = (CachePreloadCallback) params[1];
                } else {
                    return null;
                }
                if (params.length > 2) startingAt = (String) params[2];
                if (params.length > 3) startingAt = (String) params[3];
                InputStream inputStream = createInputStream(startingAt, stoppingAt);
                if (inputStream == null) {
                    return null;
                }
                try {
                    IOUtils.copy(inputStream, new NullOutputStream());

                } catch (IOException e) {
                    //e.printStackTrace();
                }
                return callback;
            }
        }

        @Override
        protected void onPostExecute(CachePreloadCallback callback) {
            super.onPostExecute(callback);
            Log.i(this.getClass().getSimpleName(), "onPostExecute("+((callback==null)?"null":"not-null")+")");
            if(callback!=null) {
                InputStream inputStream = createInputStream();
                if (inputStream != null) {

                    callback.onLoad(inputStream);

                }
            }
        }
    }

    void preload(CachePreloadCallback callback) {
        Log.i(this.getClass().getSimpleName(), "preload()");
        PreloadTask preloadTask = new PreloadTask();
        //preloadTask.callback = callback;
        preloadTask.execute(this,callback,null,null);
    }

    InputStream createInputStream() {
        return createInputStream(null,null);
    }

    InputStream createInputStream(String startingAt, String stoppingAt) {
        Log.i(this.getClass().getSimpleName(),"createInputStream("+startingAt+","+stoppingAt+")");
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
    protected abstract InputStream createCacheInputStream();

    protected abstract OutputStream createCacheOutputStream();

    protected abstract void invalidateCache();

    public void invalidate() {
        invalidateCache();
        if (fallback!=null) {
            fallback.invalidate();
        }
    }

    private InputStream wrappedFallbackStream(String startingAt, String stoppingAt) {
        if (fallback==null) {
            return null;
        }
        final InputStream fallbackInputStream = new BufferedInputStream(fallback.createInputStream(startingAt, stoppingAt));
        final OutputStream cacheOutputStream = new BufferedOutputStream(createCacheOutputStream());

        return new InputStream() {

            @Override
            public int read() throws IOException {
                int b = fallbackInputStream.read();
                cacheOutputStream.write(b);
                return b;
            }

            @Override
            public long skip(long n) throws IOException {
                long toSkip = n;
                while (toSkip > 0) {
                    int b = read();
                    if(b<0) break;
                    toSkip--;
                }

                return n-toSkip;
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
            Log.e(this.getClass().getSimpleName(), "IOException while reading stream to byte array");
            return baos.toByteArray();
        }
        return null;
    }
}
