package au.com.newint.newinternationalist;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
        // TODO: return latest issue.json
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
}
