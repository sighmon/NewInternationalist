package au.com.newint.newinternationalist;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import au.com.newint.newinternationalist.util.Purchase;

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
    // issues/<issueID>.json
    CacheStreamFactory articlesJSONCacheStreamFactory;

    SparseArray<ThumbnailCacheStreamFactory> thumbnailCacheStreamFactorySparseArray;

    public Issue(File jsonFile) throws StreamCorruptedException {
        this(Publisher.parseJsonFile(jsonFile).getAsJsonObject());
    }

    public Issue(int issueID) {
        this(Publisher.getIssueJsonForId(issueID));

    }

    public Issue(JsonObject issueJson) {
        this.issueJson = issueJson;
        coverCacheStreamFactory = FileCacheStreamFactory.createIfNecessary(getCoverLocationOnFilesystem(), new URLCacheStreamFactory(getCoverURL()));
        editorsImageCacheStreamFactory = FileCacheStreamFactory.createIfNecessary(getEditorsLetterLocationOnFilesystem(), new URLCacheStreamFactory(getEditorsPhotoURL()));
        thumbnailCacheStreamFactorySparseArray = new SparseArray<>();

        // Get SITE_URL
        String siteURLString = (String) Helpers.getSiteURL();

        URL articlesURL = null;
        try {
            articlesURL = new URL(siteURLString + "issues/" + this.getID() + ".json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        File cacheDir = MainActivity.applicationContext.getCacheDir();

        File cacheFile = new File(cacheDir, this.getID() + ".json");

        articlesJSONCacheStreamFactory = FileCacheStreamFactory.createIfNecessary(cacheFile, new URLCacheStreamFactory(articlesURL));

    }

    public ArrayList<Article> buildArticlesFromDir (File dir) {
        Helpers.debugLog("Issue","buildArticlesFromDir("+dir+")");
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
                        try {
                            articlesArray.add(new Article(file, this));
                        } catch (StreamCorruptedException e) {

                            e.printStackTrace();
                            // we skip this file, it will be reloaded next time
                        }
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
            String photoString = issueJson.get("editors_photo").getAsJsonObject().get("url").getAsString();
            if (BuildConfig.DEBUG && Helpers.getSiteURL().contains("3000")) {
                // For running from a local Rails dev site
                photoString = Helpers.getSiteURL() + photoString;
            }
            return new URL(photoString);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private URL getCoverURL() {
        try {
            String coverString = issueJson.get("cover").getAsJsonObject().get("url").getAsString();
            if (BuildConfig.DEBUG && Helpers.getSiteURL().contains("3000")) {
                // For running from a local Rails dev site
                coverString = Helpers.getSiteURL() + coverString;
            }
            return new URL(coverString);
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
        preloadArticles(null);
    }

    public void preloadArticles(final CacheStreamFactory.CachePreloadCallback callback) {

        articlesJSONCacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {
            @Override
            public void onLoad(byte[] payload) {
                if (callback != null) {
                    callback.onLoad(payload);
                }
            }

            @Override
            public void onLoadBackground(byte[] payload) {
                JsonArray rootArray = null;

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
                InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
                JsonElement root = new JsonParser().parse(inputStreamReader);

                if (root.isJsonNull()) {
                    //TODO: got null json, now what?
                    // doing nothing seems to work...
                    Log.e("Issue", "Preload articles returned a JsonNull root.");
                } else {
                    rootArray = root.getAsJsonObject().get("articles").getAsJsonArray();

                    // Save article.json for each article to the filesystem

                    if (rootArray != null) {
                        for (JsonElement aRootArray : rootArray) {
                            JsonObject jsonObject = aRootArray.getAsJsonObject();

                            int id = jsonObject.get("id").getAsInt();

                            ArticleJsonCacheStreamFactory articleJsonCacheStreamFactory = new ArticleJsonCacheStreamFactory(id, Issue.this);
                            OutputStream outputStream = articleJsonCacheStreamFactory.createCacheOutputStream();
                            //FIXME: this doesn't seem to create an actual output file...
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                            new Gson().toJson(jsonObject, outputStreamWriter);
                            try {
                                // paranoia...
                                outputStreamWriter.flush();
                                outputStreamWriter.close();
                                outputStream.flush();
                                outputStream.close();
                            } catch (IOException e) {
                                Log.e("Issue", "error closing articleJsonCacheStreamFactory output stream: " + e);
                            }

                        }
                    }

                    //TODO: articles should inform their issue when they are updated
                    // make issue reload articles from disk
                    Issue.this.articles = null;

                }

                if (callback != null) {
                    callback.onLoadBackground(payload);
                }
            }
        });
    }

    public ArrayList<Article> getArticles() {
        // articles is nulled by the DownloadArticlesJSONTask.onPostExecute in Publisher

        if (articles == null || articles.size() == 0) {
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

    public Article getArticleWithID(int articleID) {
        return new Article(articleID,this);
    }

//    public Article getArticleWithID(int articleID) {
//        Article articleMatched = null;
//        if (articles == null || articles.size() == 0) {
//            articles = getArticles();
//        }
//        if (articles != null && articles.size() > 0) {
//            for (Article article : articles) {
//                if (article.getID() == articleID) {
//                    articleMatched = article;
//                }
//            }
//        }
//        return articleMatched;
//    }

    public ThumbnailCacheStreamFactory getCoverCacheStreamFactoryForSize(int width) {
        //store in a hash, only make once for each width
        ThumbnailCacheStreamFactory tcsf = thumbnailCacheStreamFactorySparseArray.get(width);
        if (tcsf==null) {
            tcsf = new ThumbnailCacheStreamFactory(width, getCoverLocationOnFilesystem(), coverCacheStreamFactory);
        }/*
            // Register for DownloadComplete listener
            Publisher.ArticlesDownloadCompleteListener listener = new Publisher.ArticlesDownloadCompleteListener() {

                @Override
                public void onArticlesDownloadComplete(JsonArray articles) {
                    Helpers.debugLog("ArticlesReady", "Received listener, refreshing articles view.");
                    // Refresh adapter data
                    adapter.notifyDataSetChanged();
                    Publisher.articleListeners.clear();
                }
            };
            Publisher.INSTANCE.setOnArticlesDownloadCompleteListener(listener);

            // Register for editors photo complete listener.
            // Register for DownloadComplete listener
            Publisher.UpdateListener editorImageListener = new Publisher.UpdateListener() {
                @Override
                public void onUpdate(Object object) {

                    // Tell the adapter to update the footer view so it loads the editor image
                    adapter.notifyItemChanged(adapter.getItemCount() - 1);
                }
            };
            Publisher.INSTANCE.setOnDownloadCompleteListener(editorImageListener);

            */

        return tcsf;
    }

    public ThumbnailCacheStreamFactory getEditorsImageCacheStreamFactoryForSize(int width, int height) {

        return new ThumbnailCacheStreamFactory(width, height, getEditorsLetterLocationOnFilesystem(), editorsImageCacheStreamFactory);
    }

    public boolean deleteCache() {
        File dir = new File(MainActivity.applicationContext.getFilesDir().getPath() + "/" + this.getID());

        boolean success = false;

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                File childFile = new File(dir, child);
                if (childFile.isDirectory()) {
                    String[] childDirChildren = childFile.list();
                    for (String childDirChild : childDirChildren) {
                        // Delete everything!
                        success = new File(childFile, childDirChild).delete();
                    }
                } else {
                    if (!child.contains("json")) {
                        // Don't delete the .json
                        success = new File(dir, child).delete();
                    }
                }
            }
        }

        return success;
    }

    public URL getIssueJsonURL() {
        try {
            return new URL(Helpers.getSiteURL() + "issues/" + getID() + ".json");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void downloadZip(ArrayList<Purchase> purchases) {

        // Attempt to get zip URL from rails

        new DownloadZipTask().execute(purchases);
    }

    // Download body async task
    private class DownloadZipTask extends AsyncTask<Object, Integer, ArrayList> {

        @Override
        protected ArrayList doInBackground(Object... objects) {

            // Pass in purchases
            ArrayList<Purchase> purchases = (ArrayList<Purchase>) objects[0];

            // Get the zip url from rails
            DefaultHttpClient httpclient = new DefaultHttpClient();

            // Setup post request
            HttpContext ctx = new BasicHttpContext();
            ctx.setAttribute(ClientContext.COOKIE_STORE, Publisher.INSTANCE.cookieStore);
            HttpPost post = new HttpPost(getIssueJsonURL().toString());
            post.setHeader("Content-Type", "application/json");

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

            HttpResponse zipURLresponse = null;

            try {
                // Execute HTTP Post Request
                zipURLresponse = httpclient.execute(post, ctx);

            } catch (ClientProtocolException e) {
                Helpers.debugLog("TOC", "Zip URL download: ClientProtocolException: " + e);
            } catch (IOException e) {
                Helpers.debugLog("TOC", "Zip URL download: IOException: " + e);
            }

            int zipURLresponseStatusCode;
            boolean zipURLsuccess = false;
            ArrayList<Object> responseList = new ArrayList<>();

            if (zipURLresponse != null) {
                // Add it to the response list
                responseList.add(zipURLresponse);

                zipURLresponseStatusCode = zipURLresponse.getStatusLine().getStatusCode();

                if (zipURLresponseStatusCode >= 200 && zipURLresponseStatusCode < 300) {
                    // We have the ZipURL JSON
                    Helpers.debugLog("TOC", "Zip URL success: " + zipURLresponse.getStatusLine());
                    zipURLsuccess = true;

                } else if (zipURLresponseStatusCode > 400 && zipURLresponseStatusCode < 500) {
                    // Zip request failed
                    Helpers.debugLog("TOC", "Zip URL request failed with code: " + zipURLresponseStatusCode);

                } else {
                    // Server error.
                    Helpers.debugLog("TOC", "Zip URL request failed with code: " + zipURLresponseStatusCode + " and response: " + zipURLresponse.getStatusLine());
                }

            } else {
                // Error getting zipURL
                Helpers.debugLog("TOC", "Zip URL request failed! Response is null");
            }

            if (zipURLsuccess) {
                Helpers.debugLog("TOC", "Success! ZipURL found.");

                // Download the zip file

                String zipURLresponseJSONstring = "";
                JsonObject zipURLJson;
                String zipURL = "";
                HttpResponse zipFileResponse = null;
                try {
                    zipURLresponseJSONstring = EntityUtils.toString(zipURLresponse.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (zipURLresponseJSONstring.length() > 0) {
                    zipURLJson = new JsonParser().parse(zipURLresponseJSONstring).getAsJsonObject();
                    zipURL = zipURLJson.get("zipURL").getAsString();
                }

                if (zipURL != null && zipURL.length() > 0) {
                    // Setup post request
                    HttpContext zipContext = new BasicHttpContext();
                    HttpGet zipGet = new HttpGet(zipURL);

                    try {
                        // Execute HTTP Post Request
                        zipFileResponse = httpclient.execute(zipGet, zipContext);

                    } catch (ClientProtocolException e) {
                        Helpers.debugLog("TOC", "Zip file download: ClientProtocolException: " + e);
                    } catch (IOException e) {
                        Helpers.debugLog("TOC", "Zip file download: IOException: " + e);
                    }
                }

                int zipFileResponseStatusCode;
                boolean zipFileSuccess = false;

                if (zipFileResponse != null) {

                    // Add it to the response list
                    responseList.add(zipFileResponse);

                    zipFileResponseStatusCode = zipFileResponse.getStatusLine().getStatusCode();

                    if (zipFileResponseStatusCode >= 200 && zipFileResponseStatusCode < 300) {
                        // We have the ZipURL JSON
                        Helpers.debugLog("TOC", "Zip file success: " + zipFileResponse.getStatusLine());
                        zipFileSuccess = true;

                    } else if (zipFileResponseStatusCode > 400 && zipFileResponseStatusCode < 500) {
                        // Zip request failed
                        Helpers.debugLog("TOC", "Zip file request failed with code: " + zipFileResponseStatusCode);

                    } else {
                        // Server error.
                        Helpers.debugLog("TOC", "Zip file request failed with code: " + zipFileResponseStatusCode + " and response: " + zipFileResponse.getStatusLine());
                    }

                } else {
                    Helpers.debugLog("TOC", "Zip file download response is null, sorry.");
                }

                if (zipFileSuccess) {
                    // Unzip the zip and move it into place

                    Helpers.debugLog("TOC", "Zip file: " + zipFileResponse);

                    ZipInputStream zipInputStream = null;
                    try {
                        zipInputStream = new ZipInputStream(new BufferedInputStream(zipFileResponse.getEntity().getContent()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File saveDir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getID()));

                    if (zipInputStream != null) {
                        try {
                            ZipEntry entry;
                            while ((entry = zipInputStream.getNextEntry()) != null) {
                                Helpers.debugLog("TOC","Zip file entry: " + entry.getName() + ", " + entry.getSize());

                                // If it's a directory, make it.
                                String filename = entry.getName();
                                File file = new File(saveDir, filename);

                                if (file.isDirectory()) {
                                    file.mkdir();
                                } else {
                                    // Read the bytes for this entry
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    byte[] buffer = new byte[1024];
                                    int count;
                                    while ((count = zipInputStream.read(buffer)) != -1) {
                                        baos.write(buffer, 0, count);
                                    }

                                    byte[] bytes = baos.toByteArray();

                                    // Write the bytes to the filesystem
                                    FileOutputStream fos = new FileOutputStream(file);
                                    fos.write(bytes);
                                    fos.close();
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                zipInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            return responseList;
        }

        @Override
        protected void onPostExecute(ArrayList responseList) {
            super.onPostExecute(responseList);

            // Post listener
            Publisher.IssueZipDownloadCompleteListener listener = Publisher.INSTANCE.issueZipDownloadCompleteListener;
            if (listener != null) {
                listener.onIssueZipDownloadComplete(responseList);
            }
        }
    }


    // PARCELABLE delegate methods

    private Issue(Parcel in) {
        this(in.readInt());
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
