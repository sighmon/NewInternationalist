package au.com.newint.newinternationalist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

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
    CacheStreamFactory coverCacheStreamFactory;
    CacheStreamFactory editorsImageCacheStreamFactory;

    public Issue(File jsonFile) {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("Issue(File) was passed a non-existent file");
        }

        issueJson = root.getAsJsonObject();
        coverCacheStreamFactory = new FileCacheStreamFactory(getCoverLocationOnFilesystem(), new URLCacheStreamFactory(getCoverURL()));
        editorsImageCacheStreamFactory = new FileCacheStreamFactory(getEditorsLetterLocationOnFilesystem(), new URLCacheStreamFactory(getEditorsPhotoURL()));

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
        //TODO: dry up Issue(File), Issue(int) and Issue(Parcel)
        issueJson = Publisher.getIssueJsonForId(issueID);
        articles = getArticles();
        coverCacheStreamFactory = new FileCacheStreamFactory(getCoverLocationOnFilesystem(), new URLCacheStreamFactory(getCoverURL()));
        editorsImageCacheStreamFactory = new FileCacheStreamFactory(getEditorsLetterLocationOnFilesystem(), new URLCacheStreamFactory(getEditorsPhotoURL()));
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

    private URL getCoverURL() {
        try {
            return new URL(issueJson.get("cover").getAsJsonObject().get("url").getAsString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public URL getWebURL() {
        try {
            return new URL(Helpers.getSiteURL() + "issues/" + getID());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public File getCoverLocationOnFilesystem() {

        File issueDir =  new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(getID()));
        String[] pathComponents = getCoverURL().getPath().split("/");
        String coverFilename = pathComponents[pathComponents.length - 1];

        return new File(issueDir, coverFilename);
    }

    public File getEditorsLetterLocationOnFilesystem() {

        File issueDir =  new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(getID()));
        String[] pathComponents = getEditorsPhotoURL().getPath().split("/");
        String editorsPhotoFilename = pathComponents[pathComponents.length - 1];

        return new File(issueDir, editorsPhotoFilename);
    }

    public Uri getCoverUriOnFilesystem() {

        String issueDir =  MainActivity.applicationContext.getFilesDir() + Integer.toString(getID());
        String[] pathComponents = getCoverURL().getPath().split("/");
        String coverFilename = pathComponents[pathComponents.length - 1];
        Uri.Builder uri = new Uri.Builder();
        uri.appendPath(issueDir);
        uri.appendPath(coverFilename);

        return uri.build();
    }

    public ArrayList<Article> getArticles() {
        if (articles == null) {
            File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getID()) + "/");
            articles = Publisher.buildArticlesFromDir(dir);
            // TODO: Sort into sections by category
            Collections.sort(articles, new Comparator<Article>() {
                @Override
                public int compare(Article lhs, Article rhs) {
                    return lhs.getPublication().compareTo(rhs.getPublication());
                }
            });
        }

        return articles;
    }

    public ThumbnailCacheStreamFactory getCoverCacheStreamFactoryForSize(int width) {

        return new ThumbnailCacheStreamFactory(width, getCoverLocationOnFilesystem(), coverCacheStreamFactory);
    }

    public ThumbnailCacheStreamFactory getEditorsImageCacheStreamFactoryForSize(int width, int height) {

        return new ThumbnailCacheStreamFactory(width, height, getEditorsLetterLocationOnFilesystem(), editorsImageCacheStreamFactory);
    }

    // PARCELABLE delegate methods

    private Issue(Parcel in) {
        issueJson = Publisher.getIssueJsonForId(in.readInt());
        articles = getArticles();
        coverCacheStreamFactory = new FileCacheStreamFactory(getCoverLocationOnFilesystem(), new URLCacheStreamFactory(getCoverURL()));
        editorsImageCacheStreamFactory = new FileCacheStreamFactory(getEditorsLetterLocationOnFilesystem(), new URLCacheStreamFactory(getEditorsPhotoURL()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.getID());
    }

    public static final Parcelable.Creator<Issue> CREATOR = new Parcelable.Creator<Issue>() {
        public Issue createFromParcel(Parcel in) {
            return new Issue(in);
        }

        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };

}
