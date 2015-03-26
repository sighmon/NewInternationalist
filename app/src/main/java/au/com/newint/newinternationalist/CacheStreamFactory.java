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
        Log.i("CacheStreamFactory", "creating factory of type " + name + ", fallback is " + ((fallback != null) ? "not" : "") + " null");
        preloadTask = new PreloadTask();
        this.fallback = fallback;
        this.name = name;
    }

    public String toString() {
        return "CacheStreamFactory[]";
    }

    interface CachePreloadCallback {
        void onLoad(CacheStreamFactory streamCache);
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
                if (params.length > 3) stoppingAt = (String) params[3];
                InputStream inputStream = createInputStream(startingAt, stoppingAt);
                if (inputStream == null) {
                    return null;
                }
                try {
                    IOUtils.copy(inputStream, new NullOutputStream());
                    inputStream.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                return callback;
            }
        }

        @Override
        protected void onPostExecute(CachePreloadCallback callback) {
            super.onPostExecute(callback);
            Log.i("CacheStreamFactory", CacheStreamFactory.this+"->preload()->onPostExecute("+((callback==null)?"null":"not-null")+")");
            if(callback!=null) {
                callback.onLoad(CacheStreamFactory.this);
            }
        }
    }

    void preload(CachePreloadCallback callback) {
        Log.i("CacheStreamFactory", this+"->preload(...)");
        PreloadTask preloadTask = new PreloadTask();
        //preloadTask.callback = callback;
        preloadTask.execute(this,callback,null,null);
    }

    InputStream createInputStream() {
        return createInputStream(null,null);
    }

    InputStream createInputStream(String startingAt, String stoppingAt) {
        Log.i("CacheStreamFactory",this+"->createInputStream("+startingAt+","+stoppingAt+")");
        if (stoppingAt != null && stoppingAt.equals(name)) {
            Log.e("CacheStreamFactory", "stoppingAt hit, returning null");
            return null;
        }
        if (startingAt == null || startingAt.equals(name)) {
            InputStream cis = createCacheInputStream();
            if (cis != null) {
                Log.i("CacheStreamFactory",this+": cis!=null");
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
        Log.i("CacheStreamFactory", this+"->wrappedFallbackStream("+startingAt+", "+stoppingAt+")");
        if (fallback==null) {
            return null;
        }

        //final InputStream fallbackInputStream = new BufferedInputStream(fallback.createInputStream(startingAt, stoppingAt));
        //final OutputStream cacheOutputStream = new BufferedOutputStream(createCacheOutputStream());

        final InputStream fallbackInputStream = new BufferedInputStream(fallback.createInputStream(startingAt, stoppingAt));
        final OutputStream cacheOutputStream = new BufferedOutputStream(createCacheOutputStream());


        InputStream writeThroughStream = new InputStream() {

            @Override
            public int read() throws IOException {
                //Log.i("writeThroughStream","read() called");
                int b = -1;
                try {
                    b = fallbackInputStream.read();
                    if(b>=0) {
                       cacheOutputStream.write(b);
                       //cacheOutputStream.flush();
                    } else {
                        Log.e("writeThroughStream","fallbackInputStream.read() got "+b);
                    }
                }
                catch (IOException e) {
                    Log.e("writeThroughStream", "a wrapped stream got an exception");
                    e.printStackTrace();
                }
                return b;
            }

            @Override
            public long skip(long n) throws IOException {
                long skipped;
                Log.i("writeThroughStream","skip("+n+") called");
                for(skipped = 0;skipped < n; skipped++) {
                    try {
                        read();
                    }
                    catch (IOException e) {
                        Log.e("writeThroughStream", "exception during read");
                        e.printStackTrace();
                    }
                }
                return skipped;
            }

            @Override
            public void close() throws IOException {
                fallbackInputStream.close();
                cacheOutputStream.close();
            }

        };


        return writeThroughStream;

    }

    byte[] read() {
        return read(null,null);
    }

    // convenience method to mimick old ByteCache
    byte[] read(String startingAt, String stoppingAt) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            InputStream inputStream = this.createInputStream(startingAt, stoppingAt);
            long c = IOUtils.copy(inputStream, byteArrayOutputStream);
            inputStream.close();
            Log.i("CacheStreamFactory", this+": IOUtils.copy processes "+c+" bytes");
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.e("CacheStreamFactory", this+": IOException while reading stream to byte array");
        }
        return null;
    }
}
