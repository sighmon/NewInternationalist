package au.com.newint.newinternationalist;

import android.os.AsyncTask;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by New Internationalist on 9/01/15.
 */
public enum Publisher {
    INSTANCE;

    ArrayList<Issue> issuesList;

    ArrayList <UpdateListener> listeners = new ArrayList <UpdateListener> ();

    public interface UpdateListener {
        void onUpdate(Object object);
    }

    //TODO: shouldn't this be in Issue?
    public interface ArticlesDownloadCompleteListener {
        void onArticlesDownloadComplete(JsonArray articles);
    }


    public void setOnDownloadCompleteListener(UpdateListener listener) {
        // Store the listener object
        listeners.add(listener);
    }

    public void removeDownloadCompleteListener(UpdateListener listener) {
        // Remove the listener object
        listeners.remove(listener);
    }

    static ArrayList <ArticlesDownloadCompleteListener> articleListeners = new ArrayList <ArticlesDownloadCompleteListener> ();

    public void setOnArticlesDownloadCompleteListener(ArticlesDownloadCompleteListener listener) {
        // Store the listener object
        articleListeners.add(listener);
    }

    public int numberOfIssues() {

        // Count the number of instances of issue.json
        return getIssuesFromFilesystem().size();
    }

    public ArrayList<Issue> getIssuesFromFilesystem() {

        if(issuesList==null) {
            File dir = MainActivity.applicationContext.getFilesDir();
            issuesList = buildIssuesFromDir(dir);
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
                        issuesArray.add(new Issue(file));
                    }
                }
            }
        }
        return issuesArray;
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

    public static JsonObject getIssueJsonForId(int id) {
        // Return issue.json for id handed in
        File issueJson;

        File dir = new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(id));

        issueJson = new File(dir,"issue.json");

        if (issueJson.exists()) {
            // Return parsed issue.json as JsonObject
            return parseIssueJson(issueJson);
        } else {
            // We don't have the issue.json, something went wrong with the initial download. HELP!
            // TODO: Download issues.json and re-save to filesystem
            return null;
        }
    }

    public Issue latestIssue() {
        // Return latest issue.json
        ArrayList<Issue> issuesJsonArray = getIssuesFromFilesystem();

        Issue newestIssue = null;

        for (int i = 0; i < issuesJsonArray.size(); i++) {
            Issue thisIssue = issuesJsonArray.get(i);
            if (newestIssue != null) {

                if (thisIssue.getRelease().after(newestIssue.getRelease())) {
                    newestIssue = thisIssue;
                }
            } else {
                newestIssue = thisIssue;
            }
        }
        if (newestIssue != null) {
            Log.i("LatestIssue", String.format("ID: %1$s, Title: %2$s", newestIssue.getID(), newestIssue.getTitle()));
        }
        return newestIssue;
    }

    public static JsonObject parseIssueJson(Object issueJson) {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) issueJson));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if (root != null) {
            return root.getAsJsonObject();
        } else {
            return null;
        }
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





    // ARTICLES download task for Issue issue
    public static class DownloadArticlesJSONTask extends AsyncTask<Object, Integer, JsonArray> {

        @Override
        protected JsonArray doInBackground(Object... objects) {

            JsonArray rootArray = null;

            ByteCache articlesJSONCache = (ByteCache) objects[0];
            Issue issue = (Issue) objects[1];

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(articlesJSONCache.read());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);
            InputStreamReader inputStreamReader = new InputStreamReader(bufferedInputStream);
            JsonElement root = new JsonParser().parse(inputStreamReader);
            rootArray = root.getAsJsonObject().get("articles").getAsJsonArray();

            // Save article.json for each article to the filesystem

            if (rootArray != null) {
                for (JsonElement aRootArray : rootArray) {
                    JsonObject jsonObject = aRootArray.getAsJsonObject();

                    int id = jsonObject.get("id").getAsInt();

                    File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(issue.getID()) + "/", Integer.toString(id));

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
            issue.articles = null;

            return rootArray;
        }

        @Override
        protected void onPostExecute(JsonArray articles) {
            super.onPostExecute(articles);

            // Send articles to listener
            for (ArticlesDownloadCompleteListener listener : articleListeners) {
                Log.i("ArticlesReady", "Calling onArticlesDownloadComplete");
                // TODO: Handle multiple listeners
                listener.onArticlesDownloadComplete(articles);
            }
        }
    }
}
