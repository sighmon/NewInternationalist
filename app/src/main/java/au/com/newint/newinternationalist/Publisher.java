package au.com.newint.newinternationalist;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;

/**
 * Created by New Internationalist on 9/01/15.
 */
public class Publisher {

    public static int numberOfIssues() {
        int issues = 0;
        // TODO: Count the number of instances of issue.json
        return issues;
    }

    public static JsonObject latestIssue() {
        // TODO: return an issue model, for now just an int.
//        File file = new File(getApplicationContext().getFilesDir(), filename);
//        Gson gson = new Gson();
        JsonObject issue = new JsonObject();
        return issue;
    }
}
