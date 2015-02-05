package au.com.newint.newinternationalist;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by New Internationalist on 4/02/15.
 */
public class Issue {

    String title;
    int id;
    int number;
    Date release;
    String editors_name;
    String editors_letter_html;
    String editors_photo;
    HashMap cover;
    List articles;

    public Issue() {
        title = "Magazine title";
        id = 1;
        number = 1;
        release = new Date();
        editors_name = "Editor's Name";
        editors_letter_html = "A long string of editors letter text.";
        editors_photo = "http://www.somesite.com/somephoto.jpg";
        cover = new HashMap();
        articles = new ArrayList<Article>();
        // TODO: Hook this up to Publisher
        for (int i = 1; i <= 25; i++) {
            final Article article = new Article();
            article.title = article.title + i;
            article.teaser = article.teaser + " And some more text for teaser " + i;
            article.id = i;
            articles.add(article);
        }
    }
}
