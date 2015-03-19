package au.com.newint.newinternationalist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

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

    HashMap<URL,Integer> imageRequestsHashMap;

    CacheStreamFactory coverCacheStreamFactory;

    public Issue(File jsonFile) {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("Issue(File) was passed a non-existent file");
        }

        issueJson = root.getAsJsonObject();

//        articles = getArticles();

        File coverFile = getCoverLocationOnFilesystem();

        coverCacheStreamFactory = new FileCacheStreamFactory(coverFile, new URLCacheStreamFactory(getCoverURL()));

        /*
        title = getTitle();
        id = getID();
        number = getNumber();
        release = getRelease();
        editors_name = getEditorsName();
        editors_letter_html = getEditorsLetterHtml();
        editors_photo = getEditorsPhotoURL();
        cover = getCoverURL();

        issueJson = null; // ugh...
        */
    }

    public Issue(int issueID) {
        issueJson = Publisher.getIssueJsonForId(issueID);
        articles = getArticles();
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
            return new URL(issueJson.get("editors_photo").getAsJsonObject().get("url").getAsString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public URL getCoverURL() {
        try {
            return new URL(issueJson.get("cover").getAsJsonObject().get("url").getAsString());
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

    public Uri getCoverUriOnFilesystem() {

        String issueDir =  MainActivity.applicationContext.getFilesDir() + Integer.toString(getID());
        String[] pathComponents = getCoverURL().getPath().split("/");
        String coverFilename = pathComponents[pathComponents.length - 1];
        Uri.Builder uri = new Uri.Builder();
        uri.appendPath(issueDir);
        uri.appendPath(coverFilename);

        return uri.build();
    }

    public ArrayList<Article> getArticles() {
        if (articles == null) {
            File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getID()) + "/");
            articles = Publisher.buildArticlesFromDir(dir);
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

    public InputStream getCoverInputStream() {
        return coverCacheStreamFactory.createInputStream();
    }

    public InputStream getCoverInputStreamFromFile() {
        return coverCacheStreamFactory.createInputStream(null, "file");
    }

    public File getCoverForSize(int width, int height) {
        return getImageForSize(getCoverURL(), width, height);
    }

    public File getEditorsImage() {
        return getImage(getEditorsPhotoURL());
    }

    public File getEditorsImageForSize(int width, int height) {
        return getImageForSize(getEditorsPhotoURL(), width, height);
    }

    public File getImage(URL imageURL) {
        // Search filesystem for file. Download if need be.

        File imageFile = null;

        File dir = new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(getID()));
        String[] pathComponents = imageURL.getPath().split("/");
        String filename = pathComponents[pathComponents.length - 1];

        imageFile = new File(dir,filename);

        if (imageFile.exists()) {
            // Return image from filesystem
            return imageFile;
        } else {
            if (imageRequestsHashMap == null) {
                imageRequestsHashMap = new HashMap<>();
            }
            Integer isRequesting = imageRequestsHashMap.get(imageURL);
            if (isRequesting != null && isRequesting == 1) {
                // Image is already being requested
                return null;
            } else {
                // Download image
                if (imageRequestsHashMap == null) {
                    imageRequestsHashMap = new HashMap<>();
                }
                imageRequestsHashMap.put(imageURL, 1);
                new DownloadImage().execute(this, imageURL);
                return null;
            }
        }
    }


    public File getImageForSize(URL imageURL, int width, int height) {

        File imageForSize = null;

        File issueDir =  new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(getID()));
        String[] pathComponents = imageURL.getPath().split("/");
        String fullsizeImageFilename = pathComponents[pathComponents.length -1];
        String fileExtension = null;
        if (fullsizeImageFilename.contains(".")) {
            fileExtension = fullsizeImageFilename.substring(fullsizeImageFilename.lastIndexOf("."));
        } else {
            fileExtension = "";
        }
        String imageForSizeFilename = fullsizeImageFilename + "_" + width + "_" + height + fileExtension;
        imageForSize = new File(issueDir,imageForSizeFilename);

        if (imageForSize != null && imageForSize.exists()) {
            // Return imageForSize from filesystem
            return imageForSize;
        } else {
            // Scale image for size requested
            File fullsizeImage = getImage(imageURL);
            if (fullsizeImage != null && fullsizeImage.exists()) {
                Bitmap fullsizeImageBitmap = BitmapFactory.decodeFile(fullsizeImage.getPath());
                if (fullsizeImageBitmap != null) {
                    // TODO: Work out why this creates jagged images. Is the image size wrong??
                    //                    Bitmap scaledCover = Bitmap.createScaledBitmap(fullsizeImageBitmap, width, height, true);

                    // Scale image with fixed width and aspect ratio, crop if need be
                    Bitmap scaledImage = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
                    float originalWidth = fullsizeImageBitmap.getWidth(), originalHeight = fullsizeImageBitmap.getHeight();
                    Canvas canvas = new Canvas(scaledImage);
                    float scale = width / originalWidth;
                    float xTranslation = 0.0f, yTranslation = (height - originalHeight * scale) / 2.0f;
                    Matrix transformation = new Matrix();
                    transformation.postTranslate(xTranslation, yTranslation);
                    transformation.preScale(scale, scale);
                    Paint paint = new Paint();
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(fullsizeImageBitmap, transformation, paint);

                    // Save to filesystem
                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = new FileOutputStream(imageForSize);
                        scaledImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return imageForSize;
                } else {
                    // The image file is corrups!
                    if (fullsizeImage.delete()) {
                        Log.i("Image", "This image " + imageURL.toString() + " was corrup, but deleted successfully.");
                    } else {
                        Log.i("Image", "ERROR: Couldn't delete this cover..." + fullsizeImage);
                    }
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    // PARCELABLE delegate methods

    private Issue(Parcel in) {
        issueJson = Publisher.getIssueJsonForId(in.readInt());
        articles = getArticles();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.getID());
    }

    public static final Parcelable.Creator<Issue> CREATOR
            = new Parcelable.Creator<Issue>() {
        public Issue createFromParcel(Parcel in) {
            return new Issue(in);
        }

        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };

    // async tasks -------------------------------------------------------

    public static class DownloadImage extends AsyncTask<Object, Integer, Issue> {

        @Override
        protected Issue doInBackground(Object... params) {

            // Download the image

            File imageFile = null;

            Issue issue = (Issue) params[0];
            URL imageURL = (URL) params[1];

            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) imageURL.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                assert urlConnection != null;
                InputStream urlConnectionInputStream = urlConnection.getInputStream();

                File dir = new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(issue.getID()));
                String[] pathComponents = imageURL.getPath().split("/");
                String filename = pathComponents[pathComponents.length - 1];

                imageFile = new File(dir,filename);

                // Save to filesystem
                FileOutputStream fos = new FileOutputStream(imageFile);

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

            // Remove imageRequest entry
            if (issue.imageRequestsHashMap != null && issue.imageRequestsHashMap.size() > 0) {
                issue.imageRequestsHashMap.remove(imageURL);
            }

            return issue;
        }

        @Override
        protected void onPostExecute(Issue issue) {
            super.onPostExecute(issue);

            // Send issue to listener
            // TODO: in future, hand in listener in .execute parameters
            for (Publisher.UpdateListener listener : Publisher.INSTANCE.listeners) {
                Log.i("DownloadComplete", "Calling onDownloadComplete");
                // TODO: Handle multiple listeners
                listener.onUpdate(issue);
            }
        }
    }
}
