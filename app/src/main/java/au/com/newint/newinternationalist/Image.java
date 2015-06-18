package au.com.newint.newinternationalist;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by New Internationalist on 17/04/15.
 */
public class Image {

    // id
    // article_id
    // caption
    // credit
    // data
    //  url
    //  thumb
    //   url
    // media_id
    // hidden
    // position

    Article parentArticle;
    JsonObject imageJson;
    int issueID;
    CacheStreamFactory fullImageCacheStreamFactory;

    public Image(JsonObject imageJson, int issueID, Article parentArticle) {
        this.parentArticle = parentArticle;
        this.imageJson = imageJson;
        this.issueID = issueID;
        fullImageCacheStreamFactory = new FileCacheStreamFactory(getImageLocationOnFilesystem(), new URLCacheStreamFactory(getFullsizeImageURL()));
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Image) {
            Image image = (Image) object;
            return this.getID() == image.getID();
        }
        return false;
    }

    public int getID() {
        return imageJson.get("id").getAsInt();
    }

    public int getArticleID() {
        return imageJson.get("article_id").getAsInt();
    }

    public int getPosition() {
        JsonElement positionObject = imageJson.get("position");
        if (positionObject != null) {
            return positionObject.getAsInt();
        } else {
            return 1;
        }
    }

    public String getCaption() {
        JsonElement element = imageJson.get("caption");
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }

    public String getCredit() {
        JsonElement element = imageJson.get("credit");
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }

    private URL getFullsizeImageURL() {
        try {
            return new URL(imageJson.get("data").getAsJsonObject().get("url").getAsString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public File getImageLocationOnFilesystem() {
        Article article = this.parentArticle;
        File articleDir =  new File(MainActivity.applicationContext.getFilesDir(), Integer.toString(article.getIssueID()) + "/" + article.getID() + "/");
        String[] pathComponents = getFullsizeImageURL().getPath().split("/");
        String imageFilename = pathComponents[pathComponents.length - 1];

        return new File(articleDir, imageFilename);
    }

    public ThumbnailCacheStreamFactory getImageCacheStreamFactoryForSize(int width) {

        return new ThumbnailCacheStreamFactory(width, getImageLocationOnFilesystem(), fullImageCacheStreamFactory);
    }
}
