package au.com.newint.newinternationalist;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

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

    public static ArrayList<Article> buildArticlesFromDir (File dir) {
        ArrayList<Article> articlesArray = new ArrayList<Article>();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    articlesArray.addAll(buildArticlesFromDir(file));
                } else {
                    // do something here with the file
                    if (file.getName().equals("article.json")) {
                        // Add to array
                        articlesArray.add(new Article(file));
                    }
                }
            }
        }
        return articlesArray;
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

    public void preloadArticles() {



        // Get SITE_URL
        String siteURLString = (String) Helpers.getSiteURL();

        // Get articles.json (actually issueID.json) and save/update our cache
        URL articlesURL = null;
        try {
            articlesURL = new URL(siteURLString + "issues/" + this.getID() + ".json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        ByteCache articlesJSONCache = new ByteCache();

        File cacheDir = MainActivity.applicationContext.getCacheDir();
        File cacheFile = new File(cacheDir, this.getID() + ".json");

        // new way..
        CacheStreamFactory articlesJSONCacheStreamFactory = new FileCacheStreamFactory(cacheFile, new URLCacheStreamFactory(articlesURL));

        articlesJSONCacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {
            @Override
            public void onLoad(byte[] payload) {
            }

            @Override
            public void onLoadBackground(byte[] payload) {
                JsonArray rootArray = null;

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
                InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
                JsonElement root = new JsonParser().parse(inputStreamReader);
                rootArray = root.getAsJsonObject().get("articles").getAsJsonArray();

                // Save article.json for each article to the filesystem

                if (rootArray != null) {
                    for (JsonElement aRootArray : rootArray) {
                        JsonObject jsonObject = aRootArray.getAsJsonObject();

                        int id = jsonObject.get("id").getAsInt();

                        File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(Issue.this.getID()) + "/", Integer.toString(id));

                        dir.mkdirs();

                        File file = new File(dir, "article.json");

                        try {
                            Writer w = new FileWriter(file);

                            new Gson().toJson(jsonObject, w);

                            w.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //TODO: articles should inform their issue when they are updated
                // make issue reload articles from disk
                Issue.this.articles = null;

            }
        });



    }

    public ArrayList<Article> getArticles() {
        // articles is nulled by the DownloadArticlesJSONTask.onPostExecute in Publisher

        if (articles == null) {
            // assumes that all articles have been downloaded..
            File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getID()) + "/");
            articles = buildArticlesFromDir(dir);
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
