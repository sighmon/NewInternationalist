package au.com.newint.newinternationalist;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamCorruptedException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by New Internationalist on 9/01/15.
 */
public enum Publisher {
    INSTANCE;

    final CacheStreamFactory issuesJSONCacheStreamFactory;

    ArrayList<Issue> issuesList;

    ArrayList <UpdateListener> listeners = new ArrayList <UpdateListener> ();

    ArrayList <LoginListener> loginListeners = new ArrayList<>();

    ArrayList <SubscriptionListener> subscriptionListeners = new ArrayList<>();

    boolean loggedIn;
    boolean hasValidSubscription;

    BasicCookieStore cookieStore;

    Publisher() {
        setupCookieStore();

        // Get SITE_URL from config variables
        String siteURLString = Helpers.getSiteURL();
        Helpers.debugLog("SITE_URL", siteURLString);

        // Get issues.json and save/update our cache
        URL issuesURL = null;
        try {
            issuesURL = new URL(siteURLString + "issues.json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        File cacheDir = MainActivity.applicationContext.getCacheDir();
        File cacheFile = new File(cacheDir,"issues.json");

        issuesJSONCacheStreamFactory = FileCacheStreamFactory.createIfNecessary(cacheFile, new URLCacheStreamFactory(issuesURL));

    }

    public void setupCookieStore() {
        if (cookieStore == null) {
            cookieStore = new BasicCookieStore();
        }
    }

    public void deleteCookieStore() {
        cookieStore = new BasicCookieStore();
    }

    public interface UpdateListener {
        void onUpdate(Object object);
    }

    public interface LoginListener {
        void onUpdate(Object object);
    }

    public interface SubscriptionListener {
        void onUpdate(Object object);
    }

    //TODO: shouldn't this be in Issue?
//    public interface ArticlesDownloadCompleteListener {
//        void onArticlesDownloadComplete(JsonArray articles);
//    }

    public interface ArticleBodyDownloadCompleteListener {
        void onArticleBodyDownloadComplete(ArrayList responseList);
    }

    public interface IssueZipDownloadCompleteListener {
        void onIssueZipDownloadComplete(ArrayList responseList);
    }

    public void setOnDownloadCompleteListener(UpdateListener listener) {
        // Store the listener object
        listeners.add(listener);
    }

    public void removeDownloadCompleteListener(UpdateListener listener) {
        // Remove the listener object
        listeners.remove(listener);
    }

    public void removeArticleBodyDownloadCompleteListener(ArticleBodyDownloadCompleteListener listener) {
        listeners.remove(listener);
    }

    public void removeIssueZipDownloadCompleteListener(IssueZipDownloadCompleteListener zipListener) {
        listeners.remove(zipListener);
    }

    public void setLoggedInListener(LoginListener listener) {
        // Store the listener object
        loginListeners.add(listener);
    }

    public void setSubscriptionListener(SubscriptionListener listener) {
        // Store the listener object
        subscriptionListeners.add(listener);
    }

    // Done using preloadArticles now.. no listeners needed
//    static ArrayList <ArticlesDownloadCompleteListener> articleListeners = new ArrayList <ArticlesDownloadCompleteListener> ();
//
//    public void setOnArticlesDownloadCompleteListener(ArticlesDownloadCompleteListener listener) {
//        // Store the listener object
//        articleListeners.add(listener);
//    }

    ArticleBodyDownloadCompleteListener articleBodyDownloadCompleteListener;
    IssueZipDownloadCompleteListener issueZipDownloadCompleteListener;

    public void setOnArticleBodyDownloadCompleteListener(ArticleBodyDownloadCompleteListener listener) {
        // Store the listener object
        articleBodyDownloadCompleteListener = listener;
    }

    public void setOnIssueZipDownloadCompleteListener(IssueZipDownloadCompleteListener listener) {
        // Store the listener object
        issueZipDownloadCompleteListener = listener;
    }

    public int numberOfIssues() {

        // Count the number of instances of issue.json
        return getIssuesFromFilesystem().size();
    }

    public ArrayList<Issue> getIssuesFromFilesystem() {

        if (issuesList == null) {
            File dir = MainActivity.applicationContext.getFilesDir();
            issuesList = buildIssuesFromDir(dir);
            Collections.sort(issuesList, new Comparator<Issue>() {
                @Override
                public int compare(Issue lhs, Issue rhs) {
                    return rhs.getRelease().compareTo(lhs.getRelease());
                }
            });
        }

        return issuesList;

    }

    public static ArrayList<Issue> buildIssuesFromDir (File dir) {
        ArrayList<Issue> issuesArray = new ArrayList<Issue>();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    issuesArray.addAll(buildIssuesFromDir(file));
                } else {
                    // do something here with the file
                    if (file.getName().equals("issue.json")) {
                        // Add to array
                        try {
                            issuesArray.add(new Issue(file));
                        } catch (StreamCorruptedException e) {
                            e.printStackTrace();
                            // don't add it
                        }
                    }
                }
            }
        }
        return issuesArray;
    }

    public Issue getIssueForId(int id) {
        return new Issue(id);
    }

    public static JsonObject getIssueJsonForId(int id) {
        // Return issue.json for id handed in
        File issueJson;

        File dir = new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(id));

        issueJson = new File(dir,"issue.json");

        if (issueJson.exists()) {
            // Return parsed issue.json as JsonObject

            try {
                return parseJsonFile(issueJson);
            } catch (StreamCorruptedException e) {
                e.printStackTrace();
                return null;
            }

        } else {
            // We don't have the issue.json, something went wrong with the initial download. HELP!
            // TODO: Download issues.json and re-save to filesystem
            return null;
        }
    }

    public Issue latestIssue() {
        // assuming this array is sorted
        ArrayList<Issue> issuesArray = getIssuesFromFilesystem();

        Issue newestIssue = issuesArray.isEmpty()?null:issuesArray.get(0);;

        if (newestIssue != null) {
            Helpers.debugLog("LatestIssue", String.format("ID: %1$s, Title: %2$s", newestIssue.getID(), newestIssue.getTitle()));
        }

        return newestIssue;
    }

    public static JsonObject parseJsonFile(File jsonFile) throws StreamCorruptedException {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) jsonFile));
        } catch (FileNotFoundException|JsonSyntaxException e) {
            e.printStackTrace();
        }

        if (root == null || root.isJsonNull()) {
            jsonFile.delete();
            throw new StreamCorruptedException(jsonFile.toString() + " was badly formed json");
        }

        return root.getAsJsonObject();

    }

    public static Date parseDateFromString(String inputString) {
        Date releaseDate = null;
        DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            releaseDate = inputFormat.parse(inputString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return releaseDate;
    }

    // DEBUG FUNCTIONS

    public boolean deleteDirectory(Issue issue) {
        File dir = new File(MainActivity.applicationContext.getFilesDir().getPath() + "/" + issue.getID());

        boolean success = false;

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                success = new File(dir, aChildren).delete();
            }
        }

        return success;
    }
}
