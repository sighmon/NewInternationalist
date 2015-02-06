package au.com.newint.newinternationalist;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.Date;

/**
 * Created by pix on 6/02/15.
 */
public class MemoryByteCacheMethod extends ByteCacheMethod {
    byte[] payload;


    public MemoryByteCacheMethod(String name) {
        super(name);
    }

    public MemoryByteCacheMethod() {
        this("memory");
    }


    @Override
    public ByteCacheHit read() {
        Log.i("MemoryByteCacheMethod", "creating ByteCacheHit");
        if (payload!=null) {
            return new ByteCacheHit(payload);
        }
        return null;
    }

    @Override
    public void write(byte[] payload) {
       this.payload = payload;
    }
}
