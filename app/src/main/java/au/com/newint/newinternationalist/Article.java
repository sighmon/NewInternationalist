package au.com.newint.newinternationalist;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by New Internationalist on 4/02/15.
 */
public class Article {

    public static int id;
    public static String title;
    public static String teaser;
    public static Date publication;
    public static boolean keynote;
    public static String featured_image_caption;
    public static HashMap featured_image;
    public static Array categories;
    public static Array images;

    public Article() {
        title = "Test article";
    }
}
