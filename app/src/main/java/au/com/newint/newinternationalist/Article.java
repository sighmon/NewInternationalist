package au.com.newint.newinternationalist;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by New Internationalist on 4/02/15.
 */
public class Article {

    int id;
    String title;
    String teaser;
    Date publication;
    boolean keynote;
    String featured_image_caption;
    HashMap featured_image;
    Array categories;
    Array images;

    public Article() {
        title = "Test article";
        teaser = "Article teaser text goes here.";
    }
}
