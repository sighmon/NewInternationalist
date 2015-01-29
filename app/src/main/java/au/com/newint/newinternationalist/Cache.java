package au.com.newint.newinternationalist;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by pix on 29/01/15.
 */
public class Cache<PayloadType> {

    ArrayList<CacheMethod<PayloadType>> methods;

    public void Cache() {
        methods = new ArrayList();
    }

    public void addMethod(CacheMethod<PayloadType> method) {
        methods.add(method);
    }

    public PayloadType read(String startingAt, String stoppingAt, Date expiryDate) {


        if(methods.size()<1) {
            throw new AssertionError("Cache object has no methods.");
        }

        int startIndex = findIndexForName(startingAt);
        if(startIndex<0) startIndex=0;

        int stopIndex = findIndexForName(stoppingAt);
        if(stopIndex<0) stopIndex=methods.size();

        for ( int i=0; i<methods.size() ; i++ ) {
            CacheHit<PayloadType> hit = methods.get(i).read();
            if (hit!=null && ((i==stopIndex-1) || (hit.timestamp.before(expiryDate)))) {
                // writing back
                for ( int j=0 ; j < i+1 ; j++) {
                    methods.get(j).write(hit.payload);
                }
                return hit.payload;
            } else {
                Log.i("Cache", "cache miss on " + methods.get(i).name);
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

/*
    class Cache:
    def __init__(self,methods=[]):
        self.methods = methods
    def addMethod(self,method):
        self.methods.append(method)
    def read(self,startingAt=None,stoppingAt=None,maxAge=datetime.timedelta.max):
        if(len(self.methods)==0):
            raise NotImplementedError('Cache has no methods')
        startIndex = next((i for (i,v) in enumerate(self.methods) if v.name==startingAt),0)
        stopIndex = next((i for (i,v) in enumerate(self.methods) if v.name==stoppingAt),len(self.methods))
        for index,method in enumerate(self.methods[startIndex:stopIndex]):
            hit = method.read()
            # ignore maxAge if this is the last method
            if hit and ((index==stopIndex-1) or (datetime.datetime.today()-hit.timestamp)<maxAge):
                print("cache hit on "+str(method)+" at "+str(hit.timestamp))
                for method in self.methods[:index+1]:
                    #print("writing back value to method ",method.name)
                    method.write(hit)
                return hit.data
            else:
                print("cache miss on "+str(method)+" ("+str(bool(hit))+")")
*/

}