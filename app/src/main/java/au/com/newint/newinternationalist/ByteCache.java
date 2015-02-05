package au.com.newint.newinternationalist;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by pix on 29/01/15.
 */
public class ByteCache {

    ArrayList<ByteCacheMethod> methods;

    public void Cache() {
        methods = new ArrayList();
    }

    public void addMethod(ByteCacheMethod method) {
        methods.add(method);
    }

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
                // writing back
                for ( int j=0 ; j < i+1 ; j++) {
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

}