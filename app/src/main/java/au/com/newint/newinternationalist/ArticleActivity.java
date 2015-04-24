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
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


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
        } else if (id == R.id.menu_item_share) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            // Send article share information here...
            // TODO: Check for a login to generate a guest pass...
            DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            String articleInformation = article.getTitle()
                    + " - New Internationalist magazine "
                    + dateFormat.format(article.getPublication());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm reading "
                    + articleInformation
                    + ".\n\n"
                    + "Article link:\n"
                    + article.getWebURL()
                    + "\n\nMagazine link:\n"
                    + issue.getWebURL()
            );
            shareIntent.setType("text/plain");
            // TODO: When time permits, save the image to externalStorage and then share.
//                shareIntent.putExtra(Intent.EXTRA_STREAM, issue.getCoverUriOnFilesystem());
//                shareIntent.setType("image/jpeg");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "New Internationalist magazine, " + dateFormat.format(issue.getRelease()));
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share_toc)));
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
                final WebView articleBody = (WebView) rootView.findViewById(R.id.article_body);
                String articleBodyHTML = article.getExpandedBody();
                if (articleBodyHTML == null) {
                    articleBodyHTML = Helpers.wrapInHTML("<p>Loading...</p>");
                }
                articleBody.getSettings().setJavaScriptEnabled(true);
                articleBody.loadDataWithBaseURL("file:///android_asset/", articleBodyHTML, "text/html", "utf-8", null);
                articleBody.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished (WebView view, String url) {
                        // Insert the images from the cache onPageFinished loading
                        ArrayList<Image> images = article.getImages();
                        for (final Image image : images) {
                            // Get the images
                            Log.i("ArticleBody", "Loading image: " + image.getID());
                            image.fullImageCacheStreamFactory.preload(null, null, new CacheStreamFactory.CachePreloadCallback() {
                                @Override
                                public void onLoad(byte[] payload) {
                                    Log.i("ArticleBody", "Inserting image: " + image.getID());
                                    try {
                                        String javascript = String.format("javascript:"
                                                + "var insertBody = function () {"
                                                + "  var id = 'image%1$s';"
                                                + "  var img = document.getElementById(id);"
                                                + "  img.src = '%2$s';"
                                                + "  img.parentElement.href = '%3$s';"
                                                + "};"
                                                + "insertBody();", image.getID(), image.getImageLocationOnFilesystem().toURI().toURL(), image.getImageLocationOnFilesystem().toURI().toURL());
                                        articleBody.loadUrl(javascript);
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onLoadBackground(byte[] payload) {

                                }
                            });
                        }
                    }

                    // Handle tapping images to expand, and other web links
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView  view, String  url) {
                        String[] pathComponents = url.split("\\.");
                        String fileExtension = pathComponents[pathComponents.length - 1];
                        Log.i("Article", "Image tapped: " + fileExtension);
                        if ( fileExtension.equals("jpeg") || fileExtension.equals("jpg") || fileExtension.equals("png") || fileExtension.equals("gif") ){
                            // An image was tapped
                            Intent imageIntent = new Intent(MainActivity.applicationContext, ImageActivity.class);
//                          // Pass the image url through
                            imageIntent.putExtra("url", url);
                            startActivity(imageIntent);
                            return true;
                        } else {
                            // TODO: Something else tapped
                            return true;
                        }
                    }
                });
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
            String teaserString = article.getTeaser();
            if (teaserString != null && !teaserString.isEmpty()) {
                articleTeaser.setVisibility(View.VISIBLE);
                articleTeaser.setText(Html.fromHtml(teaserString));
            } else {
                articleTeaser.setVisibility(View.GONE);
            }

            String categoriesTemporaryString = "";
            String separator = "";
            ArrayList<Category> categories = article.getCategories();
            for (Category category : categories) {
                categoriesTemporaryString += separator;
                categoriesTemporaryString += category.getName();
                separator = "\n";
            }
            articleCategories.setText(categoriesTemporaryString);

            // Get Images
            ArrayList<Image> images = article.getImages();
            Log.i("Article", "Images: " + images.size());

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
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.no_internet_dialog_message_article_body).setTitle(R.string.no_internet_dialog_title_article_body);
                        builder.setNegativeButton(R.string.no_internet_dialog_ok_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                                getActivity().finish();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }

                    articleBody.loadDataWithBaseURL("file:///android_asset/", bodyHTML, "text/html", "utf-8", null);
//                    Publisher.INSTANCE.articleBodyDownloadCompleteListener = null;
                }
            };
            Publisher.INSTANCE.setOnArticleBodyDownloadCompleteListener(listener);

            return rootView;
        }
    }
}
