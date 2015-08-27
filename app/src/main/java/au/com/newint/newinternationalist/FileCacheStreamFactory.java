package au.com.newint.newinternationalist;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Created by pix on 27/02/15.
 */
public class FileCacheStreamFactory extends CacheStreamFactory{

    static HashMap<String, FileCacheStreamFactory> instances;

    static {
        instances = new HashMap<>();
    }

    private final File cacheFile;

    FileCacheStreamFactory(File cacheFile, CacheStreamFactory fallback) {
        this(cacheFile,fallback,null);
    }

    FileCacheStreamFactory(File cacheFile, CacheStreamFactory fallback, String name) {
        super(fallback, name==null?"file":name);

        this.cacheFile = cacheFile;
    }

    static FileCacheStreamFactory createIfNecessary(File cacheFile, CacheStreamFactory fallback) {
        return FileCacheStreamFactory.createIfNecessary(cacheFile, fallback, null);
    }

    // only create one FileCSF for each file url
    static FileCacheStreamFactory createIfNecessary(File cacheFile, CacheStreamFactory fallback, String name) {

        String key = null;
        try {
            key = cacheFile.getCanonicalPath();
        } catch (IOException e) {
            Log.e("FileCacheStreamFactory", "IOError in cacheFile.getCanonicalPath()");
            return null;
            //e.printStackTrace();
        }
        FileCacheStreamFactory fileCacheStreamFactory = instances.get(key);
        if (fileCacheStreamFactory==null) {
            fileCacheStreamFactory = new FileCacheStreamFactory(cacheFile, fallback, name);
            Helpers.debugLog("FileCacheStreamFactory", "creating new cache ("+fileCacheStreamFactory.hashCode()+") for: "+key);

            instances.put(key,fileCacheStreamFactory);
        }

        return fileCacheStreamFactory;
    }

    @Override
    public String toString() {
        return "FileCacheStreamFactory["+cacheFile.getName()+(cacheFile.exists()?":"+cacheFile.length():":missing")+"]";
    }

    @Override
    protected InputStream createCacheInputStream() {

        //TODO: set a flag on completion, now that FileCSF's should be shared... but what to do when it's incomplete? block?

        if (cacheFile.exists() && cacheFile.length()>0) {
            Helpers.debugLog("FileCacheStreamFactory("+cacheFile.getName()+":"+this.hashCode()+")", "createInputStream() cache hit "+cacheFile.length()+" bytes");
            try {
                return new FileInputStream(cacheFile);
            } catch (FileNotFoundException e) {
                Helpers.debugLog("FileCacheStreamFactory", "no cache file yet");
            }
        }
        Helpers.debugLog("FileCacheStreamFactory("+cacheFile.getName()+")", "createInputStream() cache miss");
        return null;
    }

    class PartialFileOutputStream extends FileOutputStream {

        final File partialFile;
        final File completedFile;

        // workaround to store references to partial/completed file
        private PartialFileOutputStream(File completedFile, File partialFile) throws FileNotFoundException {
            super(partialFile);
            this.partialFile = partialFile;
            this.completedFile = completedFile;
        }

        public PartialFileOutputStream(File file) throws FileNotFoundException {
            this(file, new File(file.getPath() + ".part"));
        }

        @Override
        public void close() throws IOException {
            super.close(); // close the partial file output stream
            if(partialFile.exists()) {
                if (completedFile.exists()) {
                    Helpers.debugLog("PartialFileOS", "deleting " + completedFile + " before renaming " + partialFile);
                    completedFile.delete();
                }
                if (!partialFile.renameTo(completedFile)) {
                    throw (new IOException("Error renaming '" + partialFile + "' (" + (completedFile.exists() ? "exists" : "missing") + ") to '" + completedFile + "' (" + (completedFile.exists() ? "exists" : "missing") + ")"));
                }
            } else {
                Helpers.debugLog("PartialFileOS","close() called but '"+partialFile+"' does not exist.");
            }
        }
    }

    @Override
    protected OutputStream createCacheOutputStream() {
        Helpers.debugLog("FileCacheStreamFactory("+cacheFile.getName()+")", "createCacheOutputStream()");

        try {
            return new PartialFileOutputStream(cacheFile);
        } catch (FileNotFoundException e) {
            Log.e("FileCacheStreamFactory", "error creating cache file");
        }
        return null;
    }

    @Override
    protected void invalidateCache() {
        try {
            Helpers.debugLog("FileCacheStreamFactory:"+this.hashCode(), "deleting cache file: "+cacheFile.getCanonicalPath());
        } catch (IOException e) {
            Helpers.debugLog("FileCacheStreamFactory:"+this.hashCode(), "deleting cache file: ...can't get path");
        }
        cacheFile.delete();
    }



}
