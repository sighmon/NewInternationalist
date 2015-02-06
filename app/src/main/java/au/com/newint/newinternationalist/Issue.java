package au.com.newint.newinternationalist;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by New Internationalist on 4/02/15.
 */
public class Issue {

    /*
    String title;
    int id;
    int number;
    Date release;
    String editors_name;
    String editors_letter_html;
    URL editors_photo;
    URL cover;
    */
    ArrayList<Article> articles;

    JsonObject issueJson;

    public Issue(File jsonFile) {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("Issue(File) was passed a non-existent file");
        }

        issueJson = root.getAsJsonObject();

        articles = new ArrayList<Article>();
        for(int i=0;i<25;i++) {
            articles.add(new Article());
        }

        /*
        title = getTitle();
        id = getID();
        number = getNumber();
        release = getRelease();
        editors_name = getEditorsName();
        editors_letter_html = getEditorsLetterHtml();
        editors_photo = getEditorsPhotoURL();
        cover = getCoverURL();

        issueJson = null; // ugh...
        */
    }



    public String getTitle() {
        return issueJson.get("title").getAsString();
    }

    public int getID() {
        return issueJson.get("id").getAsInt();
    }

    public int getNumber() {
        return issueJson.get("number").getAsInt();
    }

    public Date getRelease() {
        return Publisher.parseDateFromString(issueJson.get("release").getAsString());
    }

    public String getEditorsName() {
        return issueJson.get("editors_name").getAsString();
    }

    public String getEditorsLetterHtml() {
        return issueJson.get("editors_letter_html").getAsString();
    }

    public URL getEditorsPhotoURL() {
        try {
            return new URL(issueJson.get("editors_photo").getAsJsonObject().get("url").getAsString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public URL getCoverURL() {
        try {
            return new URL(issueJson.get("cover").getAsJsonObject().get("url").getAsString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

}
