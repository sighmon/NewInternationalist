package au.com.newint.newinternationalist;

/**
 * Created by pix on 29/01/15.
 */
public abstract class CacheMethod<PayloadType> {

    String name;

    public void CacheMethod(String name) {
        this.name = name;
    }

    public abstract CacheHit<PayloadType> read();

    public abstract void write(Object payload);

}
