package au.com.newint.newinternationalist;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by pix on 27/02/15.
 */
public class FileCacheStreamFactory extends CacheStreamFactory{

    private final File cacheFile;

    FileCacheStreamFactory(File cacheFile, CacheStreamFactory fallback) {
        this(cacheFile,fallback,null);
    }

    FileCacheStreamFactory(File cacheFile, CacheStreamFactory fallback, String name) {
        super(fallback, name==null?"file":name);

        this.cacheFile = cacheFile;
    }

    @Override
    public String toString() {
        return "FileCacheStreamFactory["+cacheFile.getName()+(cacheFile.exists()?":"+cacheFile.length():":missing")+"]";
    }

    @Override
    protected InputStream createCacheInputStream() {
        if (cacheFile.exists() && cacheFile.length()>0) {
            Log.i("FileCacheStreamFactory("+cacheFile.getName()+")", "createInputStream() cache hit "+cacheFile.length()+" bytes");
            try {
                return new FileInputStream(cacheFile);
            } catch (FileNotFoundException e) {
                Log.i("FileCacheStreamFactory", "no cache file yet");
            }
        }
        Log.i("FileCacheStreamFactory("+cacheFile.getName()+")", "createInputStream() cache miss");
        return null;
    }


    @Override
    protected OutputStream createCacheOutputStream() {
        Log.i("FileCacheStreamFactory("+cacheFile.getName()+")", "createCacheOutputStream()");

        try {
            return new FileOutputStream(cacheFile);
        } catch (FileNotFoundException e) {
            Log.e("FileCacheStreamFactory", "error creating cache file");
        }
        return null;
    }

    @Override
    protected void invalidateCache() {
        cacheFile.delete();
    }



}
