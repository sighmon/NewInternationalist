package au.com.newint.newinternationalist;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

        ByteCache bodyCache = new ByteCache();

        File articleDir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getIssueID()) + "/", Integer.toString(getID()));
        File cacheFile = new File(articleDir,"body.html");

        bodyCache.addMethod(new MemoryByteCacheMethod());
        bodyCache.addMethod(new FileByteCacheMethod(cacheFile));
        bodyCache.addMethod(new URLByteCacheMethod(articleBodyURL));

        new DownloadBodyTask().execute(bodyCache);

        return "<html><body><p>Loading...</p></body></html>";
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
    private class DownloadBodyTask extends AsyncTask<ByteCache, Integer, String> {

        @Override
        protected String doInBackground(ByteCache... caches) {

            ByteCache bodyCache = caches[0];

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bodyCache.read());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
            StringBuilder total = new StringBuilder();
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    total.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (total.length() < 1) {
                // Need to POST request for body
                Log.i("ArticleBody", "TODO: Post request!");

                URL articleBodyURL = null;
                try {
                    articleBodyURL = new URL(Helpers.getSiteURL() + "issues/" + Integer.toString(getIssueID()) + "/articles/" + Integer.toString(getID()) + "/body");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                // TODO: Post to rails

                return "TODO: Post request";

            } else {
                return total.toString();
            }
        }

        @Override
        protected void onPostExecute(String bodyHTML) {
            super.onPostExecute(bodyHTML);

            // Post body to listener
            Publisher.INSTANCE.articleBodyDownloadCompleteListener.onArticleBodyDownloadComplete(bodyHTML);
        }
    }
}
