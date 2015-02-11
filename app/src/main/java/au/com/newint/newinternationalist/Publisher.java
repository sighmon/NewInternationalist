package au.com.newint.newinternationalist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

/**
 * Created by New Internationalist on 9/01/15.
 */
public class Publisher {

    static ArrayList<Issue> issuesList;

    public interface DownloadCompleteListener {
        void onDownloadComplete(File fileDownloaded);
    }

    public interface ArticlesDownloadCompleteListener {
        void onArticlesDownloadComplete(JsonArray articles);
    }

    static ArrayList <DownloadCompleteListener> listeners = new ArrayList <DownloadCompleteListener> ();

    public static void setOnDownloadCompleteListener(DownloadCompleteListener listener) {
        // Store the listener object
        listeners.add(listener);
    }

    static ArrayList <ArticlesDownloadCompleteListener> articleListeners = new ArrayList <ArticlesDownloadCompleteListener> ();

    public static void setOnArticlesDownloadCompleteListener(ArticlesDownloadCompleteListener listener) {
        // Store the listener object
        articleListeners.add(listener);
    }

    public static int numberOfIssues() {

        // Count the number of instances of issue.json
        return getIssuesFromFilesystem().size();
    }

    public static ArrayList<Issue> getIssuesFromFilesystem() {

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

    public static Issue latestIssue() {
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

    public static File getCoverForIssue(Issue issue) {
        // Search filesystem for file. Download if need be.

        File coverFile = null;

        File dir = new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(issue.getID()));
        String[] pathComponents = issue.getCoverURL().getPath().split("/");
        String filename = pathComponents[pathComponents.length - 1];

        coverFile = new File(dir,filename);

        if (coverFile.exists()) {
            // Return cover from filesystem
            return coverFile;
        } else {
            // Download cover
            new DownloadMagazineCover().execute(issue);
            return null;
        }
    }

    public static class DownloadMagazineCover extends AsyncTask<Issue, Integer, File> {

        @Override
        protected File doInBackground(Issue... params) {

            // Download the cover

            File coverFile = null;

            Issue issue = (Issue) params[0];

            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) issue.getCoverURL().openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                assert urlConnection != null;
                InputStream urlConnectionInputStream = urlConnection.getInputStream();

                File dir = new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(issue.getID()));
                String[] pathComponents = issue.getCoverURL().getPath().split("/");
                String filename = pathComponents[pathComponents.length - 1];

                coverFile = new File(dir,filename);

                // Save to filesystem
                FileOutputStream fos = new FileOutputStream(coverFile);

                IOUtils.copy(urlConnectionInputStream, fos);

                fos.close();
            }
            catch(Exception e) {
                Log.e("http", e.toString());
            }
            finally {
                assert urlConnection != null;
                urlConnection.disconnect();
            }

            return coverFile;
        }

        @Override
        protected void onPostExecute(File coverFile) {
            super.onPostExecute(coverFile);

            // Send coverFile to listener
            for (DownloadCompleteListener listener : listeners) {
                Log.i("DownloadComplete", "Calling onDownloadComplete");
                // TODO: Handle multiple listeners
                listener.onDownloadComplete(coverFile);
            }
        }
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
                Iterator<JsonElement> i = rootArray.iterator();
                while(i.hasNext()) {
                    JsonObject jsonObject = i.next().getAsJsonObject();

                    int id = jsonObject.get("id").getAsInt();

                    File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(issue.getID()) + "/",Integer.toString(id));
                    dir.mkdirs();

                    File file = new File(dir,"article.json");

                    try {
                        Writer w = new FileWriter(file);

                        new Gson().toJson(jsonObject,w);

                        w.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

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
