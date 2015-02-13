package au.com.newint.newinternationalist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.Date;

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

    public Issue(File jsonFile) {
        JsonElement root = null;
        try {
            root = new JsonParser().parse(new FileReader((File) jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("Issue(File) was passed a non-existent file");
        }

        issueJson = root.getAsJsonObject();

        articles = getArticles();

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

    public ArrayList<Article> getArticles() {
        if(articles == null) {
            File dir = new File(MainActivity.applicationContext.getFilesDir() + "/" + Integer.toString(getID()) + "/");
            articles = Publisher.buildArticlesFromDir(dir);
        }

        return articles;
    }

    public File getCover() {
        // Search filesystem for file. Download if need be.

        File coverFile = null;

        File dir = new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(getID()));
        String[] pathComponents = getCoverURL().getPath().split("/");
        String filename = pathComponents[pathComponents.length - 1];

        coverFile = new File(dir,filename);

        if (coverFile.exists()) {
            // Return cover from filesystem
            return coverFile;
        } else {
            // Download cover
            new DownloadMagazineCover().execute(this);
            return null;
        }
    }

    public File getCoverForSize(int width, int height) {

        // TODO: FIX THIS!!!!!!!!!!!!!

        File coverForSize = null;

        File coverDir =  new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(getID()));
        String[] pathComponents = getCoverURL().getPath().split("/");
        // TODO: Get the extension programatically!
        String coverForSizeFilename = pathComponents[pathComponents.length - 1] + "_" + width + "_" + height + ".jpg";
        coverForSize = new File(coverDir,coverForSizeFilename);

        if (coverForSize != null) {
            // Return coverForSize from filesystem
            return coverForSize;
        } else {
            File fullsizeCover = getCover();
            if (fullsizeCover != null) {
                // Scale cover for size requested
                Bitmap fullsizeCoverBitmap = BitmapFactory.decodeFile(fullsizeCover.getPath());
                Bitmap scaledCover = Bitmap.createScaledBitmap(fullsizeCoverBitmap, width, height, true);
                // Save to filesystem
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(coverForSize);
                    scaledCover.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream);
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return coverForSize;

            } else {
                // Download first, then scale
                new DownloadMagazineCover().execute(this);
                // TODO: Get callback to re-run coverForSize
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

    public static class DownloadMagazineCover extends AsyncTask<Issue, Integer, Issue> {

        @Override
        protected Issue doInBackground(Issue... params) {

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

            return issue;
        }

        @Override
        protected void onPostExecute(Issue issue) {
            super.onPostExecute(issue);

            // Send coverFile to listener
            // TODO: in future, hand in listener in .execute parameters
            for (Publisher.UpdateListener listener : Publisher.INSTANCE.listeners) {
                Log.i("DownloadComplete", "Calling onDownloadComplete");
                // TODO: Handle multiple listeners
                listener.onUpdate(issue);
            }
        }
    }
}
