package au.com.newint.newinternationalist;

/**
 * Created by pix on 29/01/15.
 */
public abstract class ByteCacheMethod {

    String name;

    public void CacheMethod(String name) {
        this.name = name;
    }

    public abstract ByteCacheHit read();

    public abstract void write(Object payload);

}
