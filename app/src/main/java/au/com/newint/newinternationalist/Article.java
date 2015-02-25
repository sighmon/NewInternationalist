package au.com.newint.newinternationalist;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by New Internationalist on 4/02/15.
 */
public class Article {

/*    int id;
    String title;
    String teaser;
    Date publication;
    boolean keynote;
    String featured_image_caption;
    ArrayList featured_image;
    ArrayList categories;
    ArrayList images; */

    JsonObject articleJson;
    // TODO: create Images class
//    ArrayList<Images> articles;
    // TODO: create Categories class
//    ArrayList<Category> categories;

    public Article(File jsonFile) {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("Issue(File) was passed a non-existent file");
        }

        articleJson = root.getAsJsonObject();
    }

    public int getId() {
        return articleJson.get("id").getAsInt();
    }

    public String getTitle() {
        return articleJson.get("title").getAsString();
    }

    public String getTeaser() {
        return articleJson.get("teaser").getAsString();
    }

    public Date getPublication() {
        return Publisher.parseDateFromString(articleJson.get("publication").getAsString());
    }

    public boolean getKeynote() {
        boolean keynote = false;
        try {
            keynote = articleJson.get("keynote").getAsBoolean();
        } catch (Exception e) {
            // Keynote is empty, so getAsBoolean barfs.
            // TODO: more graceful fail?
//            Log.i("GetKeynote", e.toString());
        }
        return keynote;
    }

    public String getFeaturedImageCaption() {
        return articleJson.get("featured_image_caption").getAsString();
    }
}
