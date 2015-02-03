package au.com.newint.newinternationalist;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by New Internationalist on 9/01/15.
 */
public class Publisher {

    public interface DownloadCompleteListener {
        void onDownloadComplete(File fileDownloaded);
    }

    static ArrayList <DownloadCompleteListener> listeners = new ArrayList <DownloadCompleteListener> ();

    public static void setOnDownloadCompleteListener(DownloadCompleteListener listener) {
        // Store the listener object
        listeners.add(listener);
    }

    public static int numberOfIssues(Context context) {

        // Count the number of instances of issue.json
        return getAllIssueJsonFromFilesystem(context).size();
    }

    public static ArrayList getAllIssueJsonFromFilesystem(Context context) {
        File dir = context.getApplicationContext().getFilesDir();
        ArrayList issuesJsonArray = new ArrayList();
        addIssueJsonFilesToArrayFromDir(dir,issuesJsonArray);
        return issuesJsonArray;
    }

    public static void addIssueJsonFilesToArrayFromDir (File dir, ArrayList arrayList) {

        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    addIssueJsonFilesToArrayFromDir(file, arrayList);
                } else {
                    // do something here with the file
                    if (file.getName().equals("issue.json")) {
                        // Add to array
                        arrayList.add(file);
                    }
                }
            }
        }
    }

    public static JsonObject latestIssue(Context context) {
        // Return latest issue.json
        ArrayList issuesJsonArray = getAllIssueJsonFromFilesystem(context);

        JsonObject newestIssue = null;

        for (int i = 0; i < issuesJsonArray.size(); i++) {
            JsonObject thisIssue = parseIssueJson(issuesJsonArray.get(i));
            if (newestIssue != null) {
                // Compare release dates
                Date newestIssueDate = parseDateFromString(newestIssue.get("release").getAsString());
                Date thisIssueDate = parseDateFromString(thisIssue.get("release").getAsString());

                if (thisIssueDate.after(newestIssueDate)) {
                    newestIssue = thisIssue;
                }
            } else {
                newestIssue = thisIssue;
            }
        }
        if (newestIssue != null) {
            Log.i("LatestIssue", String.format("ID: %1$s, Title: %2$s", newestIssue.get("id"), newestIssue.get("title")));
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

    public static File getCoverForIssue(ArrayList coverParams) {
        // TODO: Search filesystem for file. Download if need be.

        File coverFile = null;

        URL coverURL = (URL) coverParams.get(0);
        String issueID = (String) coverParams.get(1);
        Context context = (Context) coverParams.get(2);

        File dir = new File(context.getFilesDir(), issueID);
        String[] pathComponents = coverURL.getPath().split("/");
        String filename = pathComponents[pathComponents.length - 1];

        coverFile = new File(dir,filename);

        if (coverFile.exists()) {
            // Return cover from filesystem
            return coverFile;
        } else {
            // Download cover
            new DownloadMagazineCover().execute(coverParams);
            return null;
        }
    }

    public static ArrayList buildCoverParams(String coverURLString, String issueID, Context context) {
        URL coverURL = null;
        try {
            coverURL = new URL(coverURLString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        ArrayList<Object> coverParams = new ArrayList<>();
        // Send URL object and Rails issueID to request Cover.
        coverParams.add(coverURL);
        coverParams.add(issueID);
        coverParams.add(context);
        return coverParams;
    }

    public static class DownloadMagazineCover extends AsyncTask<ArrayList, Integer, File> {

        @Override
        protected File doInBackground(ArrayList... params) {

            // Download the cover

            File coverFile = null;

            URL coverURL = (URL) params[0].get(0);
            String issueID = (String) params[0].get(1);
            Context context = (Context) params[0].get(2);

            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) coverURL.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                assert urlConnection != null;
                InputStream urlConnectionInputStream = urlConnection.getInputStream();

                File dir = new File(context.getFilesDir(), issueID);
                String[] pathComponents = coverURL.getPath().split("/");
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
}
