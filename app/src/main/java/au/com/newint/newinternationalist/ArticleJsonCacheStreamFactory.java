package au.com.newint.newinternationalist;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by pix on 5/06/15.
 */
public class ArticleJsonCacheStreamFactory extends CacheStreamFactory {

    Issue source;
    int id;

    public ArticleJsonCacheStreamFactory(int id, Issue source) {
        super(null,"article");

        this.id = id;
        this.source = source;


    }

    @Override
    protected InputStream createCacheInputStream() {
        try {
            return new FileInputStream(getCacheFile());
        } catch (FileNotFoundException e) {
            Log.e("ArticleJsonCSF", String.format("file not found creating input stream: %s", e));
            return null;
        }
    }

    @Override
    protected OutputStream createCacheOutputStream() {

        try {
            return new FileOutputStream(getCacheFile());
        } catch (FileNotFoundException e) {
            Log.e("ArticleJsonCSF", String.format("error creating output stream: %s", e));
            return null;
        }
    }

    private File getCacheFile() {
        File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(this.source.getID()) + "/", Integer.toString(this.id));

        dir.mkdirs();

        return new File(dir, "article.json");
    }

    @Override
    protected void invalidateCache() {
        getCacheFile().delete();
        source.articlesJSONCacheStreamFactory.invalidate();
        source.articles = null;
    }
}
