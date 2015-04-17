package au.com.newint.newinternationalist;

import com.google.gson.JsonObject;

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

    JsonObject imageJson;

    public Image(JsonObject imageJson) {
        this.imageJson = imageJson;
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
}
