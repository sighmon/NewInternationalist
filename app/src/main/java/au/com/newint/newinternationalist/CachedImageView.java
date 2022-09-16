package au.com.newint.newinternationalist;

import android.content.Context;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by New Internationalist on 30/04/15.
 */
public class CachedImageView extends AppCompatImageView {
    public CacheStreamFactory cacheStreamFactory;

    public CachedImageView(Context context) {
        super(context);
    }

    public void setCacheStreamFactory(CacheStreamFactory cacheStreamFactory) {
        // find last cacheStreamFactory and kill it's process
        if(this.cacheStreamFactory!=null) {

            this.cacheStreamFactory.cancel(false);
        }
        this.cacheStreamFactory = cacheStreamFactory;
    }

    public boolean hasCacheStreamFactory(CacheStreamFactory cacheStreamFactory) {
        return this.cacheStreamFactory==cacheStreamFactory;
    }

}