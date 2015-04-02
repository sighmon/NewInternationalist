package au.com.newint.newinternationalist;

import com.google.gson.JsonObject;

/**
 * Created by New Internationalist on 2/04/15.
 */
public class Category {

//    int id;
//    String name;
//    int colour;

    JsonObject categoryJson;

    public Category(JsonObject categoryJson) {
        this.categoryJson = categoryJson;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Category) {
            Category category = (Category) object;
            return this.getID() == category.getID();
        }
        return false;
    }

    public String getName() {
        return categoryJson.get("name").getAsString();
    }

    public int getColour() {
        return categoryJson.get("colour").getAsInt();
    }

    public int getID() {
        return categoryJson.get("id").getAsInt();
    }

    public String getSectionName() {
        return getName().split("/")[1];
    }

}
