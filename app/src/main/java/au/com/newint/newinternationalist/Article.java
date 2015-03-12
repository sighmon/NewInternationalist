package au.com.newint.newinternationalist;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by New Internationalist on 4/02/15.
 */
public class Article implements Parcelable {

/*    int id;
    String title;
    String teaser;
    Date publication;
    boolean keynote;
    String featured_image_caption;
    ArrayList featured_image;
    ArrayList categories;
    ArrayList images; */

    JsonObject articleJson;
    int issueID;

    // TODO: create Images class
//    ArrayList<Images> articles;
    // TODO: create Categories class
//    ArrayList<Category> categories;

    public Article(File jsonFile) {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("Issue(File) was passed a non-existent file");
        }

        // TODO: Work out why this is crashing sometimes when root is JsonNull here.
        // Maybe that the article.json half gets downloaded and never saved?
        // So file exists but data was never saved?
        articleJson = root.getAsJsonObject();
        issueID = Integer.parseInt(jsonFile.getPath().split("/")[5]);
    }

    public int getID() {
        return articleJson.get("id").getAsInt();
    }

    public int getIssueID() {
        return issueID;
    }

    public String getTitle() {
        return articleJson.get("title").getAsString();
    }

    public String getTeaser() {
        return articleJson.get("teaser").getAsString();
    }

    public String getBody() {

        // POST request to rails.

        URL articleBodyURL = null;
        try {
            articleBodyURL = new URL(Helpers.getSiteURL() + "issues/" + Integer.toString(getIssueID()) + "/articles/" + Integer.toString(getID()) + "/body");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        File articleDir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getIssueID()) + "/", Integer.toString(getID()));
        File cacheFile = new File(articleDir,"body.html");

        String bodyHTML = wrapInHTML("<p>Loading...</p>");

        if (cacheFile.exists()) {
            // Already have the body, so return it's contents as a string
            Log.i("ArticleBody", "Filesystem hit! Returning from file.");
            try {
                FileInputStream inputStream = new FileInputStream(cacheFile);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                bodyHTML = wrapInHTML(stringBuilder.toString());
            }
            catch (FileNotFoundException e) {
                Log.e("ArticleBody", "File not found: " + e.toString());
                bodyHTML = wrapInHTML("ERROR: File not found.");
            } catch (IOException e) {
                Log.e("ArticleBody", "Cannot read file: " + e.toString());
                bodyHTML = wrapInHTML("ERROR: Cannot read file.");
            }
        } else {
            // Download the body
            new DownloadBodyTask().execute(articleBodyURL);
        }
        return bodyHTML;
    }

    public Date getPublication() {
        return Publisher.parseDateFromString(articleJson.get("publication").getAsString());
    }

    public boolean getKeynote() {
        boolean keynote = false;
        try {
            keynote = articleJson.get("keynote").getAsBoolean();
        } catch (Exception e) {
            // Keynote is empty, so getAsBoolean barfs.
            // TODO: more graceful fail?
//            Log.i("GetKeynote", e.toString());
        }
        return keynote;
    }

    public String getFeaturedImageCaption() {
        return articleJson.get("featured_image_caption").getAsString();
    }

    public ArrayList<HashMap<String,Object>> getCategories() {

        JsonArray rootArray = articleJson.get("categories").getAsJsonArray();
        ArrayList<HashMap<String,Object>> categories = new ArrayList<>();

        if (rootArray != null) {
            for (JsonElement aRootArray : rootArray) {
                JsonObject jsonObject = aRootArray.getAsJsonObject();
                if (jsonObject != null) {
                    HashMap<String, Object> category = new HashMap<>();
                    category.put("id", jsonObject.get("id").getAsInt());
                    category.put("name", jsonObject.get("name").getAsString());
                    if (jsonObject.get("colour") != null) {
                        category.put("colour", jsonObject.get("colour").getAsInt());
                    }
                    categories.add(category);
                }
            }
        }

        return categories;
    }

    // PARCELABLE delegate methods

    private Article(Parcel in) {
        int[] intArray = in.createIntArray();
        articleJson = Publisher.getArticleJsonForId(intArray[0], intArray[1]);
        issueID = intArray[1];
    }

    public static final Parcelable.Creator<Article> CREATOR
            = new Parcelable.Creator<Article>() {
        public Article createFromParcel(Parcel in) {
            return new Article(in);
        }

        public Article[] newArray(int size) {
            return new Article[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int[] intArray = {this.getID(), this.getIssueID()};
        dest.writeIntArray(intArray);
    }

    // Download body async task
    private class DownloadBodyTask extends AsyncTask<Object, Integer, String> {

        @Override
        protected String doInBackground(Object... objects) {

            URL articleBodyURL = (URL) objects[0];
            String bodyHTML = "";

            // Try logging into Rails for authentication.
            DefaultHttpClient httpclient = new DefaultHttpClient();

            // Try to connect
            HttpContext ctx = new BasicHttpContext();
            ctx.setAttribute(ClientContext.COOKIE_STORE, Publisher.INSTANCE.cookieStore);
            HttpPost post = new HttpPost(articleBodyURL.toString());
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse response = null;

            try {
                // Execute HTTP Post Request
                response = httpclient.execute(post, ctx);

            } catch (ClientProtocolException e) {
                Log.i("ArticleBody", "ClientProtocolException: " + e);
            } catch (IOException e) {
                Log.i("ArticleBody", "IOException: " + e);
            }

            int responseStatusCode;
            boolean success = false;

            if (response != null) {
                responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode >= 200 && responseStatusCode < 300) {
                    // We have the article Body
                    success = true;

                } else if (responseStatusCode > 400 && responseStatusCode < 500) {
                    // Article request failed
                    Log.i("ArticleBody", "Failed with code: " + responseStatusCode);
                    bodyHTML = wrapInHTML("Sorry, doesn't look like you're logged in or perhaps you don't have a current subscription.");
                    // TODO: alert and intent to login.

                } else {
                    // Server error.
                    Log.i("ArticleBody", "Failed with code: " + responseStatusCode + " and response: " + response.getStatusLine());
                    bodyHTML = wrapInHTML("Uh oh, sorry! Our server seems to be down, try again in a few minutes.");
                }

            } else {
                // Error getting article body
                Log.i("ArticleBody", "Failed! Response is null");
                bodyHTML = wrapInHTML("Uh oh, sorry! No response from the server.. try again soon.");
            }

            if (success) {
                try {
                    // Save to filesystem

                    bodyHTML = wrapInHTML(EntityUtils.toString(response.getEntity(), "UTF-8"));

                    File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getIssueID()) + "/", Integer.toString(getID()));

                    dir.mkdirs();

                    File file = new File(dir, "body.html");

                    try {
                        Writer w = new FileWriter(file);
                        w.write(bodyHTML);
                        w.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("ArticleBody", "Error writing body to filesystem.");
                        bodyHTML = wrapInHTML("Error writing body to filesystem.");
                    }

                    // Return the body html string
                    return bodyHTML;

                } catch (IOException e) {
                    e.printStackTrace();
                    bodyHTML = wrapInHTML("Error parsing response!");
                    return bodyHTML;
                }

            } else {
                return bodyHTML;
            }
        }

        @Override
        protected void onPostExecute(String bodyHTML) {
            super.onPostExecute(bodyHTML);

            // Post body to listener
            Publisher.INSTANCE.articleBodyDownloadCompleteListener.onArticleBodyDownloadComplete(bodyHTML);
        }
    }

    private String wrapInHTML(String htmlToWrap) {
        // TODO: Load CSS from file and throw it in the HTML returned
        return "<html><body style='margin: 0; padding: 0;'>" + htmlToWrap + "</body></html>";
    }
}
