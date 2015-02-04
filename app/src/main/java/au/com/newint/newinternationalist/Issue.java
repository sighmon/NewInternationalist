package au.com.newint.newinternationalist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by New Internationalist on 4/02/15.
 */
public class Issue {

    public static String title;
    public static int id;
    public static int number;
    public static Date release;
    public static String editors_name;
    public static String editors_letter_html;
    public static String editors_photo;
    public static HashMap cover;
    public static List articles;

    public Issue() {
        title = "Test title";
        id = 1;
        number = 1;
        release = new Date();
        editors_name = "Some Name";
        editors_letter_html = "A long string of editors letter text.";
        editors_photo = "http://www.somesite.com/somephoto.jpg";
        cover = new HashMap();
        articles = new ArrayList<Article>();
        // TOFIX: For now lets just add 5 articles
        for (int i = 1; i <= 5; i++) {
            Article article = new Article();
            article.title = article.title + i;
            articles.add(article);
        }
    }
}
