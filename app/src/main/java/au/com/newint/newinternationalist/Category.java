package au.com.newint.newinternationalist;

import android.text.TextUtils;

import com.google.gson.JsonObject;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by New Internationalist on 2/04/15.
 */
public class Category {

//    int id;
//    String name;
//    int colour;

    JsonObject categoryJson;

    ArrayList<Article> articles;

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

    public String getDisplayName() {

        // Remove the slashes
        String fullName = getName();
        String[] nameComponents = fullName.split("/");
        ArrayList<String> wordArray = new ArrayList<String>();
        for (String component : nameComponents) {
            if (!component.equals("") && !component.equalsIgnoreCase(getSectionName())) {
                // Remove the - between words
                String[] subnameComponents = component.split("-");
                ArrayList<String> subWordArray = new ArrayList<String>();
                for (String subComponent : subnameComponents) {
                    if (!subComponent.equals("")) {
                        subWordArray.add(Helpers.capitalize(subComponent));
                    }
                }
                wordArray.add(TextUtils.join(" ", subWordArray));
            }
        }
        if (wordArray.size() == 0) {
            return getSectionName();
        } else if (wordArray.size() == 1) {
            return wordArray.get(0);
        } else {
            return TextUtils.join(" / ", wordArray);
        }
    }

    public String getDisplayNameSingleWord() {
        String[] nameComponents = getName().split("/");
        return Helpers.capitalize(nameComponents[nameComponents.length - 1]);
    }

    public int getColour() {
        return categoryJson.get("colour").getAsInt();
    }

    public int getID() {
        return categoryJson.get("id").getAsInt();
    }

    public String getSectionName() {

        return Helpers.capitalize(getName().split("/")[1]);
    }

    public ArrayList<Article> getArticles() {
        if (articles==null) {
            ArrayList<Issue> issuesList = Publisher.INSTANCE.getIssuesFromFilesystem();
            articles = new ArrayList<Article>();
            for (Issue issue : issuesList) {
                ArrayList<Article> articlesList = issue.getArticles();
                for (Article article : articlesList) {
                    ArrayList<Category> articleCategories = article.getCategories();
                    for (Category category : articleCategories) {
                        if (category.equals(this)) {
                            articles.add(article);
                        }
                    }
                }
            }
        }
        return articles;
    }
}
