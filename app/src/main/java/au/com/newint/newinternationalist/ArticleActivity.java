package au.com.newint.newinternationalist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class ArticleActivity extends ActionBarActivity {

    static Article article;
    static Issue issue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ArticleFragment())
                    .commit();
        }

        article = getIntent().getParcelableExtra("article");
        issue = getIntent().getParcelableExtra("issue");

        setTitle(issue.getTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_article, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ArticleFragment extends Fragment {

        public ArticleFragment() {
        }

        View rootView;

        @Override
        public void onResume() {
            super.onResume();
            if (rootView != null) {
//                Log.i("onResume", "****LOADING BODY****");
                WebView articleBody = (WebView) rootView.findViewById(R.id.article_body);
                articleBody.loadDataWithBaseURL(null, article.getBody(), "text/html", "utf-8", null);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            // Set a light theme
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.ArticleTheme);

            // Clone the inflater using the ContextThemeWrapper to apply the theme
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

            rootView = localInflater.inflate(R.layout.fragment_article, container, false);

            TextView articleTitle = (TextView) rootView.findViewById(R.id.article_title);
            TextView articleTeaser = (TextView) rootView.findViewById(R.id.article_teaser);
            TextView articleCategories = (TextView) rootView.findViewById(R.id.article_categories);
            final WebView articleBody = (WebView) rootView.findViewById(R.id.article_body);

            articleTitle.setText(article.getTitle());
            articleTeaser.setText(Html.fromHtml(article.getTeaser()));

            String categoriesTemporaryString = "";
            String separator = "";
            ArrayList<HashMap<String,Object>> categories = article.getCategories();
            for (HashMap<String,Object> category : categories) {
                categoriesTemporaryString += separator;
                categoriesTemporaryString += category.get("name");
                separator = "\n";
            }
            articleCategories.setText(categoriesTemporaryString);

            // Article body html (Doing this in onResume now)
//            articleBody.getSettings().setJavaScriptEnabled(true);
//            articleBody.loadDataWithBaseURL(null, article.getBody(), "text/html", "utf-8", null);

            // Register for ArticleBodyDownloadComplete listener
            Publisher.ArticleBodyDownloadCompleteListener listener = new Publisher.ArticleBodyDownloadCompleteListener() {

                @Override
                public void onArticleBodyDownloadComplete(ArrayList responseList) {
                    Log.i("ArticleBody", "Received listener, refreshing article body.");
                    // Check response, and respond with dialog or refresh body
                    HttpResponse response = (HttpResponse) responseList.get(0);
                    String bodyHTML = "";
                    int responseStatusCode;

                    if (response != null) {
                        responseStatusCode = response.getStatusLine().getStatusCode();

                        if (responseStatusCode >= 200 && responseStatusCode < 300) {
                            // We have the article Body
                            bodyHTML = (String) responseList.get(1);

                        } else if (responseStatusCode > 400 && responseStatusCode < 500) {
                            // Article request failed
                            Log.i("ArticleBody", "Failed with code: " + responseStatusCode);
                            // Alert and intent to login.
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(R.string.login_dialog_message_article_body).setTitle(R.string.login_dialog_title_article_body);
                            builder.setPositiveButton(R.string.login_dialog_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button
                                    Intent loginIntent = new Intent(rootView.getContext(), LoginActivity.class);
                                    startActivity(loginIntent);
                                }
                            });
                            builder.setNegativeButton(R.string.login_dialog_cancel_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                    getActivity().finish();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();

                        } else {
                            // Server error.
                            Log.i("ArticleBody", "Failed with code: " + responseStatusCode + " and response: " + response.getStatusLine());
                        }

                    } else {
                        // Error getting article body
                        Log.i("ArticleBody", "Failed! Response is null");
                    }

                    articleBody.loadDataWithBaseURL(null, bodyHTML, "text/html", "utf-8", null);
//                    Publisher.INSTANCE.articleBodyDownloadCompleteListener = null;
                }
            };
            Publisher.INSTANCE.setOnArticleBodyDownloadCompleteListener(listener);

            return rootView;
        }
    }
}
