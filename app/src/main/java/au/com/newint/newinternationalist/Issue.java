package au.com.newint.newinternationalist;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
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
public class Issue implements Parcelable {

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

        articles = getArticles();

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

    public Issue(int issueID) {
        issueJson = Publisher.getIssueJsonForId(issueID);
        articles = getArticles();
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

    public ArrayList<Article> getArticles() {
        if(articles == null) {
            File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getID()) + "/");
            articles = Publisher.buildArticlesFromDir(dir);
        }

        return articles;
    }

    // PARCELABLE delegate methods

    private Issue(Parcel in) {
        issueJson = Publisher.getIssueJsonForId(in.readInt());
        articles = getArticles();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.getID());
    }

    public static final Parcelable.Creator<Issue> CREATOR
            = new Parcelable.Creator<Issue>() {
        public Issue createFromParcel(Parcel in) {
            return new Issue(in);
        }

        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };
}
