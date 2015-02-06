package au.com.newint.newinternationalist;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by pix on 29/01/15.
 */
public class ByteCache extends AsyncTask<Object,Integer,byte[]> {

    ArrayList<ByteCacheMethod> methods;

    public ByteCache() {
        methods = new ArrayList();
    }

    public void addMethod(ByteCacheMethod method) {
        methods.add(method);
    }


    public byte[] read() {
        return read(null,null,new Date(Long.MAX_VALUE));
    }

    // TODO: the returned byte array should really be immutable
    public byte[] read(String startingAt, String stoppingAt, Date expiryDate) {


        if(methods.size()<1) {
            throw new AssertionError("ByteCache object has no methods.");
        }

        int startIndex = findIndexForName(startingAt);
        if(startIndex<0) startIndex=0;

        int stopIndex = findIndexForName(stoppingAt);
        if(stopIndex<0) stopIndex=methods.size();

        for ( int i=0; i<methods.size() ; i++ ) {
            ByteCacheHit hit = methods.get(i).read();
            if (hit!=null && ((i==stopIndex-1) || (hit.timestamp.before(expiryDate)))) {
                // writing back (all previous methods, in reverse)
                // eg, first method is memory [i=0]
                //     second method is disk [i=1]
                //     third method is net [i=2]
                // if disk is considered a cache hit,
                //     the value in memory cache is refreshed with a write
                // upshot of this is that the last method's write() should never be called
                for ( int j=i-1 ; j >= 0 ; j--) {
                    methods.get(j).write(hit.payload);
                }
                return hit.payload;
            } else {
                Log.i("ByteCache", "cache miss on " + methods.get(i).name);
            }
        }

        return null;
    }

    private int findIndexForName(String name) {
        for (int i=0;i<this.methods.size();i++) {
            if(this.methods.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    // background read methods ----------------------------------------------

    public void readWithCallback(CacheListener listener) {
        execute(listener);
    }

    public void readWithCallback(CacheListener listener, String startingAt) {
        execute(listener, startingAt);
    }

    //region AsyncTask
    //------------------------------------------------------------------------

    CacheListener cacheListener;

    @Override
    protected byte[] doInBackground(Object... params) {
        cacheListener = (CacheListener)params[0];

        if(params.length>1) {
            String startingAt = (String) params[1];
            if (startingAt!=null) {
                return read(startingAt, null, new Date(Long.MAX_VALUE));
            }
        }
        return read();
    }

    @Override
    protected void onPostExecute(byte[] bytes) {
        super.onPostExecute(bytes);
        cacheListener.onReadComplete(bytes);
    }

    public interface CacheListener {
        public void onReadComplete(byte[] payload);
    }

    //endregion

}