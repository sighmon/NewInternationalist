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
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.newint.newinternationalist.util.Purchase;

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
    Issue parentIssue;


    public Article(int id, Issue parentIssue) {
        this(getArticleJsonForId(id, parentIssue), parentIssue);
    }

    public Article(JsonObject jsonObject, Issue parentIssue) {
        this.articleJson = jsonObject;
        this.parentIssue = parentIssue;
    }


    public Article(File jsonFile, Issue parentIssue) throws StreamCorruptedException {
        this(Publisher.parseJsonFile(jsonFile).getAsJsonObject(),parentIssue);
    }

    public static JsonObject getArticleJsonForId(int id, Issue parentIssue) {
        ArticleJsonCacheStreamFactory articleJsonCacheStreamFactory = new ArticleJsonCacheStreamFactory(id,parentIssue);

        return (new JsonParser()).parse(new InputStreamReader(articleJsonCacheStreamFactory.createCacheInputStream())).getAsJsonObject();
    }

    public int getID() {
        // TODO: Fix this crash... seems to only happen with the Fracking issue..
        return articleJson.get("id").getAsInt();
    }

    public int getIssueID() {
        return parentIssue.getID();
    }

    public String getTitle() {
        return articleJson.get("title").getAsString();
    }

    public String getTeaser() {
        return articleJson.get("teaser").getAsString();
    }

    public boolean isBodyOnFilesystem() {
        return bodyCacheFile().exists();
    }

    private File bodyCacheFile() {
        File articleDir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getIssueID()) + "/", Integer.toString(getID()));
        return new File(articleDir,"body.html");
    }

    private String getBody(ArrayList<Purchase> purchases) {

        // POST request to rails.

        URL articleBodyURL = null;
        try {
            articleBodyURL = new URL(Helpers.getSiteURL() + "issues/" + Integer.toString(getIssueID()) + "/articles/" + Integer.toString(getID()) + "/body_android");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        File cacheFile = bodyCacheFile();

        String bodyHTML = null;

        if (cacheFile.exists()) {
            // Already have the body, so return it's contents as a string
//            Log.i("ArticleBody", "Filesystem hit! Returning from file.");
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
                bodyHTML = Helpers.wrapInHTML(stringBuilder.toString());
            }
            catch (FileNotFoundException e) {
                Log.e("ArticleBody", "File not found: " + e.toString());
                bodyHTML = Helpers.wrapInHTML("ERROR: File not found.");
            } catch (IOException e) {
                Log.e("ArticleBody", "Cannot read file: " + e.toString());
                bodyHTML = Helpers.wrapInHTML("ERROR: Cannot read file.");
                cacheFile.delete();
            }
        } else {
            // Download the body
            new DownloadBodyTask().execute(articleBodyURL, purchases);
        }
        return bodyHTML;
    }

    public String getExpandedBody(ArrayList<Purchase> purchases) {
        // Expand [File:xxx] image tags
        String articleBody = getBody(purchases);

        if (articleBody != null) {
            articleBody = expandImageTagsInBody(articleBody);
        }

        return articleBody;
    }

    private String expandImageTagsInBody(String body) {

        ArrayList<Image> images = getImages();
        Pattern regex = Pattern.compile("\\[File:(\\d+)(?:\\|([^\\]]*))?]");
        Matcher regexMatcher = regex.matcher(body);
        String imageID = null;
        while (regexMatcher.find()) {
            MatchResult matchResult = regexMatcher.toMatchResult();
            String replacement = null;
            Log.i("ExpandedBody", "Group: " + regexMatcher.group());
            Log.i("ExpandedBody", "Group count: " + regexMatcher.groupCount());
            Log.i("ExpandedBody", "Group 1: " + regexMatcher.group(1));
            imageID = regexMatcher.group(1);
            String[] options = new String[0];
            if (regexMatcher.group(2) != null) {
                options = regexMatcher.group(2).split("\\|");
            }
            String cssClass = "article-image";
            String imageWidth = "300";
            for (String option : options) {
                if (option.equalsIgnoreCase("full")) {
                    cssClass = "all-article-images article-image-cartoon article-image-full";
                    imageWidth = "945";
                } else if (option.equalsIgnoreCase("cartoon")) {
                    cssClass = "all-article-images article-image-cartoon";
                    imageWidth = "600";
                } else if (option.equalsIgnoreCase("centre")) {
                    cssClass = "all-article-images article-image-cartoon article-image-centre";
                    imageWidth = "300";
                } else if (option.equalsIgnoreCase("small")) {
                    cssClass = "article-image article-image-small";
                    imageWidth = "150";
                }
            }

            // Check for no shadow and left options
            if (Arrays.asList(options).contains("ns")) {
                cssClass += " no-shadow";
            } else if (Arrays.asList(options).contains("left")) {
                cssClass += " article-image-float-none";
            }

            String credit_div = null;
            String caption_div = null;
            String imageCredit = null;
            String imageCaption = null;
            String imageSource = "file:///android_res/drawable/loading_image.png";
            String imageZoomURI = "#";

            Image image = null;
            for (Image anImage : images) {
                if (anImage.getID() == Integer.valueOf(imageID)) {
                    image = anImage;
                }
            }
            if (image != null) {
                imageCredit = image.getCredit();
                imageCaption = image.getCaption();
            }

            if (imageCredit != null) {
                credit_div = String.format("<div class='new-image-credit'>%1$s</div>", imageCredit);
            }

            if (imageCaption != null) {
                caption_div = String.format("<div class='new-image-caption'>%1$s</div>", imageCaption);
            }

            replacement = String.format("<div class='%1$s'><a href='%2$s'><img id='image%3$s' width='%4$s' src='%5$s'/></a>%6$s%7$s</div>", cssClass, imageZoomURI, imageID, imageWidth, imageSource, caption_div, credit_div);

            body = body.substring(0, matchResult.start()) + replacement + body.substring(matchResult.end());
            regexMatcher.reset(body);
        }

        if (imageID == null && images != null && images.size() > 0) {
            // There are images that haven't been embedded in the body
            String imageStringOfMissingImages = "";
            for (Image missedImage : images) {
                // Build up new string of images
                imageStringOfMissingImages += String.format("[File:%1$s]", Integer.toString(missedImage.getID()));
            }
            // Add image [File: xx|full] tag to body and recursively try again
            if (!imageStringOfMissingImages.equals("")) {
                // Searching for article-body tag.. could do this better???
                String emptyArticleBodyString = "<div class=\"article-body\">\\s*<p(.*)>\\s*</p>\\s*</div>";
                Pattern emptyBodyRegex = Pattern.compile(emptyArticleBodyString);
                Matcher emptyBodyRegexMatcher = emptyBodyRegex.matcher(body);
                boolean matchFound = false;
                while (emptyBodyRegexMatcher.find()) {
                    MatchResult emptyMatchResult = emptyBodyRegexMatcher.toMatchResult();
                    body = body.substring(0, emptyMatchResult.start()) + imageStringOfMissingImages + body.substring(emptyMatchResult.end());
                    emptyBodyRegexMatcher.reset(body);
                    matchFound = true;
                }
                // Am I doing the recursion right here?
                // TODO: Seems to be returning twice.. but works.
                if (matchFound) {
                    return expandImageTagsInBody(body);
                }
            }
        }

        return body;
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

    public ArrayList<Category> getCategories() {

        JsonArray rootArray = articleJson.get("categories").getAsJsonArray();
        ArrayList<Category> categories = new ArrayList<>();

        if (rootArray != null) {
            for (JsonElement aRootArray : rootArray) {
                JsonObject jsonObject = aRootArray.getAsJsonObject();
                if (jsonObject != null) {
                    Category category = new Category(jsonObject);
                    categories.add(category);
                }
            }
        }

        return categories;
    }

    public ArrayList<Image> getImages() {

        JsonArray rootArray = articleJson.get("images").getAsJsonArray();
        ArrayList<Image> images = new ArrayList<>();

        if (rootArray != null) {
            for (JsonElement aRootArray : rootArray) {
                JsonObject jsonObject = aRootArray.getAsJsonObject();
                if (jsonObject != null) {
                    Image image = new Image(jsonObject, parentIssue.getID(), this);
                    images.add(image);
                }
            }
        }

        // Sort the image array by position
        Collections.sort(images, new Comparator<Image>() {
            @Override
            public int compare(Image lhs, Image rhs) {
                return Double.compare(lhs.getPosition(), rhs.getPosition());
            }
        });

        return images;
    }

    public URL getWebURL() {
        try {
            return new URL(Helpers.getSiteURL() + "issues/" + getIssueID() + "/articles/" + getID());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    // PARCELABLE delegate methods


    private Article(Parcel in) {
        //int articleID = in.createIntArray()[0];
        //int issueID = in.createIntArray()[1];
        
        // strange behaviour occurs if we call createIntArray() twice,
        // so we have yet-another-constructor that takes the int array
        this(in.createIntArray());
    }

    // ... and then call the Article(articleID, parentIssue) constructor
    private Article(int[] intArray) {
        this(intArray[0], new Issue(intArray[1]));
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
    private class DownloadBodyTask extends AsyncTask<Object, Integer, ArrayList> {

        @Override
        protected ArrayList doInBackground(Object... objects) {

            URL articleBodyURL = (URL) objects[0];
            // Pass in purchases
            ArrayList<Purchase> purchases = (ArrayList<Purchase>) objects[1];
            String bodyHTML = "";

            // Try logging into Rails for authentication.
            DefaultHttpClient httpclient = new DefaultHttpClient();

            // Setup post request
            HttpContext ctx = new BasicHttpContext();
            ctx.setAttribute(ClientContext.COOKIE_STORE, Publisher.INSTANCE.cookieStore);
            HttpPost post = new HttpPost(articleBodyURL.toString());
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // Add in-app purchase JSON data
            if (purchases != null && purchases.size() > 0) {
                JsonArray purchasesJsonArray = new JsonArray();
                for (Purchase purchase : purchases) {
                    // Send each purchase JSON data to rails to validate with Google Play
                    JsonParser parser = new JsonParser();
                    JsonObject purchaseJsonObject = (JsonObject)parser.parse(purchase.getOriginalJson());
                    purchasesJsonArray.add(purchaseJsonObject);

                }
                StringEntity stringEntity = null;
                try {
                    stringEntity = new StringEntity(purchasesJsonArray.toString());
                    stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                post.setEntity(stringEntity);
            }

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

                } else {
                    // Server error.
                    Log.i("ArticleBody", "Failed with code: " + responseStatusCode + " and response: " + response.getStatusLine());
                }

            } else {
                // Error getting article body
                Log.i("ArticleBody", "Failed! Response is null");
            }

            if (success) {
                try {
                    // Save to filesystem

                    bodyHTML = Helpers.wrapInHTML(EntityUtils.toString(response.getEntity(), "UTF-8"));

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
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ArrayList<Object> responseList = new ArrayList<>();
            responseList.add(response);
            // Get expanded bodyHTML here too..
            if (success) {
                responseList.add(getExpandedBody(purchases));
            }

            return responseList;
        }

        @Override
        protected void onPostExecute(ArrayList responseList) {
            super.onPostExecute(responseList);

            // Post body to listener
            Publisher.ArticleBodyDownloadCompleteListener listener = Publisher.INSTANCE.articleBodyDownloadCompleteListener;
            if (listener != null) {
                listener.onArticleBodyDownloadComplete(responseList);
            }
        }
    }
}
