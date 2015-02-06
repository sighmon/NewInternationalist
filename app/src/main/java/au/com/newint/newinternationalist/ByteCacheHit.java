package au.com.newint.newinternationalist;

import android.util.Log;

import java.util.Date;

/**
 * Created by pix on 29/01/15.
 */
public class ByteCacheHit {
    public byte[] payload;
    public Date timestamp;

    public ByteCacheHit(byte[] payload) {
        this(payload, new Date());
    }

    public ByteCacheHit(byte[] payload, Date timestamp) {
        Log.i("ByteCacheHit", "created with payload length: " + payload.length);
        this.payload = payload;
        this.timestamp = timestamp;
    }
}
