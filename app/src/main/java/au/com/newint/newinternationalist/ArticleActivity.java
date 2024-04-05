package au.com.newint.newinternationalist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import au.com.newint.newinternationalist.util.IabHelper;
import au.com.newint.newinternationalist.util.IabResult;
import au.com.newint.newinternationalist.util.Inventory;
import au.com.newint.newinternationalist.util.Purchase;


public class ArticleActivity extends AppCompatActivity {

    static Article article;
    static Issue issue;
    static IabHelper mHelper;
    static Inventory inventory = null;
    static ArrayList<Purchase> purchases = null;
    private GestureDetectorCompat mDetector;
    private ProgressDialog pDialog = null;
    private ShareActionProvider mshareActionProvider;

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

        mDetector = new GestureDetectorCompat(this, new ArticleGestureListener());
    }

    @Override
    public void onResume() {
        super.onResume();
        article = getIntent().getParcelableExtra("article");
        issue = getIntent().getParcelableExtra("issue");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private void showProgressDialog() {
        if (pDialog == null) {
            pDialog = new ProgressDialog(ArticleActivity.this);
            pDialog.setTitle(getResources().getString(R.string.article_guest_pass_loading_title));
            pDialog.setMessage(getResources().getString(R.string.article_guest_pass_loading_message));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
        }
        pDialog.show();
    }

    private void dismissProgressDialog() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        // Dispose of the in-app helper
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Subscribe", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (mHelper != null && !mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d("Subscribe", "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_article, menu);

        MenuItem item = menu.findItem(R.id.menu_item_share);
        mshareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mshareActionProvider != null) {
            String url = article.getWebURL().toString();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            String articleInformation = article.getTitle()
                    + " - New Internationalist magazine, "
                    + dateFormat.format(article.getPublication());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm reading "
                    + articleInformation
                    + ".\n\n"
                    + "Article link:\n"
                    + url
                    + "\n\nMagazine link:\n"
                    + issue.getWebURL()
                    + "\n\nSent from New Internationalist Android app:\n"
                    + Helpers.GOOGLE_PLAY_APP_URL
            );
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, articleInformation);

            // Send analytics event if user permits
            Helpers.sendGoogleAnalyticsEvent("Article", "Share", url);

            mshareActionProvider.setShareIntent(shareIntent);
        }

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

            // Check for a login or purchase to generate a guest pass...
            URLCacheStreamFactory urlCacheStreamFactory = article.getGuestPassURLCacheStreamFactory(purchases);
            if (urlCacheStreamFactory != null) {

                // Show loading indicator
                showProgressDialog();

                urlCacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {
                    @Override
                    public void onLoad(byte[] payload) {
                        // JSON parsing of guest pass
                        String guestPassURLString = null;
                        if (payload != null && payload.length > 0) {
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
                            InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
                            JsonElement root = new JsonParser().parse(inputStreamReader);

                            if (root.isJsonNull()) {
                                // Got null guest pass from server...
                                Log.e("GuestPass", "NULL guest pass json from rails");
                            } else {
                                // Build up the GuestPass URL
                                String guestPass = "?guest_pass=" + root.getAsJsonObject().get("key").getAsString();
                                guestPassURLString = Helpers.getSiteURL() + "issues/" + issue.getID() + "/articles/" + article.getID() + guestPass;
                            }
                        }

                        handleGuestPassURL(guestPassURLString);
                        dismissProgressDialog();
                    }

                    @Override
                    public void onLoadBackground(byte[] payload) {

                    }
                });
            } else {
                handleGuestPassURL(null);
            }
            return true;

        } else if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.increase_font_size) {
            // Increase font size
            Helpers.changeFontSize(0.25f, this);
            return true;
        } else if (id == R.id.default_font_size) {
            // Set default font size
            Helpers.changeFontSize(1.0f, this);
            return true;
        } else if (id == R.id.decrease_font_size) {
            // Decrease font size
            Helpers.changeFontSize(-0.25f, this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleGuestPassURL(String url) {
        if (url == null || url.equals("")) {
            url = article.getWebURL().toString();
        }
        // Send article share information here...
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String articleInformation = article.getTitle()
                + " - New Internationalist magazine, "
                + dateFormat.format(article.getPublication());
        shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm reading "
                        + articleInformation
                        + ".\n\n"
                        + "Article link:\n"
                        + url
                        + "\n\nMagazine link:\n"
                        + issue.getWebURL()
                        + "\n\nSent from New Internationalist Android app:\n"
                        + Helpers.GOOGLE_PLAY_APP_URL
        );
        shareIntent.setType("text/plain");
        // TODO: When time permits, save the image to externalStorage and then share.
//                shareIntent.putExtra(Intent.EXTRA_STREAM, issue.getCoverUriOnFilesystem());
//                shareIntent.setType("image/jpeg");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, articleInformation);
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share_toc)));

        // Send analytics event if user permits
        Helpers.sendGoogleAnalyticsEvent("Article", "Share", url);
    }

    class ArticleGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

//        @Override
//        public boolean onDown(MotionEvent event) {
//            Helpers.debugLog(DEBUG_TAG, "onDown: " + event.toString());
//            return true;
//        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

            // TODO: Write code to handle swipe left/right
            // http://developer.android.com/training/gestures/detector.html#detect

            float FLING_TRIGGER_POINT = 100;
            float FLING_VERTICAL_SLOP = 50;

            float deltaX = event1.getX() - event2.getX();
            float deltaY = event1.getY() - event2.getY();
            Helpers.debugLog(DEBUG_TAG, "onFling: " + deltaX + " , " + deltaY);

            if (deltaX > FLING_TRIGGER_POINT && Math.abs(deltaY) < FLING_VERTICAL_SLOP) {
                // Intent to next article
                Article nextArticle = article.getNextArticle();
                Helpers.debugLog(DEBUG_TAG, "Fling intent to next article: " + nextArticle);
                if (nextArticle != null) {
                    Intent articleIntent = new Intent(getApplicationContext(), ArticleActivity.class);
                    articleIntent.putExtra("issue", issue);
                    articleIntent.putExtra("article", nextArticle);
                    startActivity(articleIntent);
                } else {
                    // Alert at last article.
                    Helpers.debugLog(DEBUG_TAG, "At last article...");
                    lastArticleAlert();
                }
            } else if (deltaX < -FLING_TRIGGER_POINT && Math.abs(deltaY) < FLING_VERTICAL_SLOP) {
                // Intent to previous article
                Helpers.debugLog(DEBUG_TAG, "Fling finish this article...");
                finish();
            }

            return true;
        }

        public void lastArticleAlert() {
            AlertDialog.Builder builder = new AlertDialog.Builder(ArticleActivity.this);
            builder.setMessage(R.string.last_article_dialog_message).setTitle(R.string.last_article_dialog_title);
            builder.setPositiveButton(R.string.last_article_dialog_ok_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked to share their achievement
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    // Send issue share information here...
                    DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                    String magazineInformation = issue.getTitle()
                            + " - New Internationalist magazine, "
                            + dateFormat.format(issue.getRelease());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "I just finished reading "
                                    + magazineInformation
                                    + ".\n\n"
                                    + issue.getWebURL()
                                    + "\n\nSent from New Internationalist Android app:\n"
                                    + Helpers.GOOGLE_PLAY_APP_URL
                    );
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, magazineInformation);
                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share_toc)));

                    // Send analytics event if user permits
                    Helpers.sendGoogleAnalyticsEvent("Issue", "Share", issue.getWebURL().toString());
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.last_article_dialog_cancel_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked to go back to the contents page
                    Intent contentsPageIntent = new Intent(getApplicationContext(), TableOfContentsActivity.class);
                    contentsPageIntent.putExtra("issue", issue);
                    startActivity(contentsPageIntent);
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Article Fragment
     */
    public static class ArticleFragment extends Fragment {

        public ArticleFragment() {
        }

        View rootView;
        Publisher.ArticleBodyDownloadCompleteListener articleBodyDownloadCompleteListener;

        @Override
        public void onResume() {
            super.onResume();

            // Send Google Analytics if the user allows it
            Helpers.sendGoogleAnalytics(article.getTitle() + " (" + issue.getNumber() + ")");

            // Setup in-app billing
            mHelper = Helpers.setupIabHelper(getActivity().getApplicationContext());

            if (!Helpers.emulator) {
                // Only startSetup if not running in an emulator
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        if (!result.isSuccess()) {
                            // Oh noes, there was a problem.
                            Log.d("ArticleBody", "Problem setting up In-app Billing: " + result);
                        }
                        // Hooray, IAB is fully set up!
                        Helpers.debugLog("ArticleBody", "In-app billing setup result: " + result);

                        // Make a products list of the subscriptions and this issue
                        final ArrayList<String> skuList = new ArrayList<String>();
                        skuList.add(Helpers.TWELVE_MONTH_SUBSCRIPTION_ID);
                        skuList.add(Helpers.ONE_MONTH_SUBSCRIPTION_ID);
                        skuList.add(Helpers.singleIssuePurchaseID(issue.getNumber()));

                        try {
                            purchases = new ArrayList<>();
                            inventory = mHelper.queryInventory(true, skuList);
                            // Is there a subscription purchase in the inventory?
                            for (String sku : skuList) {
                                Purchase purchase = inventory.getPurchase(sku);
                                if (purchase != null) {
                                    purchases.add(purchase);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // Now attempt to get the expanded article body sending any in-app purchases
                        String articleBodyHTML = article.getExpandedBody(purchases);
                        ProgressBar articleBodyLoadingSpinner = (ProgressBar) rootView.findViewById(R.id.article_body_loading_spinner);
                        if (articleBodyHTML == null) {
                            articleBodyHTML = Helpers.wrapInHTML("");
                            articleBodyLoadingSpinner.setVisibility(View.VISIBLE);
                        } else {
                            articleBodyLoadingSpinner.setVisibility(View.GONE);
                        }
                        final WebView articleBody = (WebView) rootView.findViewById(R.id.article_body);
                        if (articleBody.getContentHeight() == 0) {
                            // Load the body if there isn't any content
                            articleBody.loadDataWithBaseURL("file:///android_asset/", articleBodyHTML, "text/html", "utf-8", null);
                        } else {
                            // If the content height is > 0, We're returning from another article so don't reload the body
                            Log.w("Article", "Intent from another article, so not re-loading body...");
                        }

                    }
                });
            }

            if (rootView != null) {
//                Helpers.debugLog("onResume", "****LOADING BODY****");

                // When the article webView body has finished loading, insert the images.
                final WebView articleBody = (WebView) rootView.findViewById(R.id.article_body);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (BuildConfig.DEBUG) {
                        // Allow debugging via chrome if app is in debug build
                        WebView.setWebContentsDebuggingEnabled(true);
                    }
                }

                articleBody.getSettings().setJavaScriptEnabled(true);
                articleBody.getSettings().setAllowFileAccess(true);
                articleBody.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished (WebView view, String url) {

                        // Insert the images from the cache onPageFinished loading
                        ArrayList<Image> images = article.getImages();
                        for (final Image image : images) {
                            // Get the images
                            Helpers.debugLog("ArticleBody", "Loading image: " + image.getID());
                            image.fullImageCacheStreamFactory.preload(null, null, new CacheStreamFactory.CachePreloadCallback() {
                                @Override
                                public void onLoad(byte[] payload) {
                                    if (payload != null) {
                                        try {
                                            Helpers.debugLog("ArticleBody", "Inserting image: " + image.getID() + ", " + image.getImageLocationOnFilesystem().toURI().toURL());
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
                                    } else {
                                        Log.e("ArticleBody", "ERROR: fullImageCacheStreamFactory payload is null.");
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
                        Helpers.debugLog("Article", "Link tapped: " + url);
                        if ( fileExtension.equals("jpeg") || fileExtension.equals("jpg") || fileExtension.equals("png") || fileExtension.equals("gif") ) {
                            // An image was tapped
                            Intent imageIntent = new Intent(MainActivity.applicationContext, ImageActivity.class);
//                          // Pass the image url through
                            imageIntent.putExtra("url", url);
                            imageIntent.putExtra("article", article);
                            startActivity(imageIntent);
                            return true;
                        } else if (url.matches("(.*)/issues/(\\d+)/articles/(\\d+)")) {
                            // It's a link to another article
                            String[] components = url.split("/");
                            final ArrayList<String> issueArticleIDs = new ArrayList<String>();
                            for (String component : components) {
                                if (component.matches("(\\d+)")) {
                                    // It's a digit, add it to our ID array
                                    issueArticleIDs.add(component);
                                }
                            }
                            if (issueArticleIDs.size() == 2) {
                                final Issue issueInUrl = new Issue(Integer.parseInt(issueArticleIDs.get(0)));
                                final ProgressDialog progress = new ProgressDialog(getActivity());
                                progress.setTitle(getResources().getString(R.string.article_link_loading_progress_title));
                                progress.setMessage(getResources().getString(R.string.article_link_loading_progress_message));
                                progress.show();
                                issueInUrl.preloadArticles(new CacheStreamFactory.CachePreloadCallback() {
                                    @Override
                                    public void onLoad(byte[] payload) {
                                        Article articleInUrl = issueInUrl.getArticleWithID(Integer.parseInt(issueArticleIDs.get(1)));
                                        if (issueInUrl != null && articleInUrl != null) {
                                            Intent articleIntent = new Intent(rootView.getContext(), ArticleActivity.class);
                                            articleIntent.putExtra("issue", issueInUrl);
                                            articleIntent.putExtra("article", articleInUrl);
                                            startActivity(articleIntent);
                                            progress.dismiss();
                                            Helpers.debugLog("Article", "Opening article: " + articleInUrl.getTitle() + " (" + articleInUrl.getID() + ")");
                                        } else {
                                            Log.e("Article", "Error: issue or article in URL is null, so can't open article.");
                                        }
                                    }

                                    @Override
                                    public void onLoadBackground(byte[] payload) {

                                    }
                                });
                            } else {
                                Log.e("Article", "Error parsing issue or article ID in link.");
                            }
                            return true;
                        } else if (url.matches("(.*)/issues/(\\d+)")) {
                            // It's a link to another issue
                            int issueID = Integer.parseInt(url.replaceAll("\\D+",""));
                            Issue issueInUrl = new Issue(issueID);
                            Intent tableOfContentsIntent = new Intent(rootView.getContext(), TableOfContentsActivity.class);
                            // Pass issue through as a Parcel
                            tableOfContentsIntent.putExtra("issue", issueInUrl);
                            startActivity(tableOfContentsIntent);
                            return true;
                        } else {
                            // An external link tapped
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
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
            final Context contextThemeWrapper = new ContextThemeWrapper(MainActivity.applicationContext, R.style.ArticleTheme);

            // Clone the inflater using the ContextThemeWrapper to apply the theme
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

            rootView = localInflater.inflate(R.layout.fragment_article, container, false);

            TextView articleTitle = (TextView) rootView.findViewById(R.id.article_title);
            TextView articleTeaser = (TextView) rootView.findViewById(R.id.article_teaser);
            final WebView articleBody = (WebView) rootView.findViewById(R.id.article_body);
            final ProgressBar articleBodyLoadingSpinner = (ProgressBar) rootView.findViewById(R.id.article_body_loading_spinner);

            articleTitle.setText(article.getTitle());
            String teaserString = article.getTeaser();
            if (teaserString != null && !teaserString.isEmpty()) {
                articleTeaser.setVisibility(View.VISIBLE);
                articleTeaser.setText(Html.fromHtml(teaserString));
            } else {
                articleTeaser.setVisibility(View.GONE);
            }

            ArrayList<Category> categories = article.getCategories();

            // Using a line breaking LinearLayout instead to compact the category buttons
//            final ExpandableHeightGridView gridview = (ExpandableHeightGridView) rootView.findViewById(R.id.article_categories_gridview);
//            gridview.setExpanded(true);
//            gridview.setAdapter(new CategoriesAdapter(MainActivity.applicationContext, categories));

            final LinearLayoutLineBreak categoriesLinearLayout = (LinearLayoutLineBreak) rootView.findViewById(R.id.article_categories_linear_layout);
            for (final Category category : categories) {
                Button categoryButton = new Button(rootView.getContext());
                categoryButton.setText(category.getDisplayNameSingleWord());
                categoryButton.setTextColor(Color.WHITE);
                categoryButton.setAllCaps(false);

                categoryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent categoryIntent = new Intent(MainActivity.applicationContext, CategoryActivity.class);
                        Helpers.debugLog("Categories", "Category tapped: " + category.getDisplayName());
                        categoryIntent.putExtra("categoryJson", category.categoryJson.toString());
                        startActivity(categoryIntent);
                    }
                });

                categoriesLinearLayout.addView(categoryButton);
            }

            // Get Images
            ArrayList<Image> images = article.getImages();
            Helpers.debugLog("Article", "Images: " + images.size());

            // Register for ArticleBodyDownloadComplete listener
            articleBodyDownloadCompleteListener = new Publisher.ArticleBodyDownloadCompleteListener() {

                @Override
                public void onArticleBodyDownloadComplete(ArrayList responseList) {
                    Helpers.debugLog("ArticleBody", "Received listener, refreshing article body.");

                    articleBodyLoadingSpinner.setVisibility(View.GONE);

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
                            Helpers.debugLog("ArticleBody", "Failed with code: " + responseStatusCode);
                            // Alert and intent to login.
                            Activity alertActivity = getActivity();
                            if (alertActivity != null) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(alertActivity);
                                builder.setMessage(R.string.login_dialog_message_article_body).setTitle(R.string.login_dialog_title_article_body);
                                builder.setPositiveButton(R.string.login_dialog_ok_button, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User clicked OK button
                                        Intent loginIntent = new Intent(rootView.getContext(), LoginActivity.class);
                                        startActivity(loginIntent);
                                    }
                                });
                                builder.setNeutralButton(R.string.login_dialog_purchase_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User clicked Purchase button
                                        Intent subscribeIntent = new Intent(rootView.getContext(), SubscribeActivity.class);
                                        startActivity(subscribeIntent);
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
                            }

                        } else {
                            // Server error.
                            Helpers.debugLog("ArticleBody", "Failed with code: " + responseStatusCode + " and response: " + response.getStatusLine());
                            bodyHTML = response.getStatusLine().toString();
                        }

                    } else {
                        // Error getting article body
                        Helpers.debugLog("ArticleBody", "Failed! Response is null");
                        Activity alertActivity = getActivity();
                        if (alertActivity != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(alertActivity);
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
                    }

                    articleBody.loadDataWithBaseURL("file:///android_asset/", bodyHTML, "text/html", "utf-8", null);
//                    Publisher.INSTANCE.articleBodyDownloadCompleteListener = null;
                }
            };
            Publisher.INSTANCE.setOnArticleBodyDownloadCompleteListener(articleBodyDownloadCompleteListener);

            return rootView;
        }

        public class CategoriesAdapter extends BaseAdapter {
            private Context mContext;
            private ArrayList<Category> mCategories;

            public CategoriesAdapter(Context c, ArrayList<Category> categories) {
                mContext = c;
                mCategories = categories;
            }

            public int getCount() {
                return mCategories.size();
            }

            public Object getItem(int position) {
                return null;
            }

            public long getItemId(int position) {
                return 0;
            }

            // create a new ImageView for each item referenced by the Adapter
            public View getView(final int position, View convertView, ViewGroup parent) {
                Button categoryButton;
                if (convertView == null) {
                    // if it's not recycled, initialize some attributes
                    categoryButton = new Button(mContext);
//                    categoryButton.setLayoutParams(new GridView.LayoutParams(85, 85));
                    categoryButton.setPadding(0, 0, 0, 0);
                    categoryButton.setTransformationMethod(null);
                    categoryButton.getBackground().setColorFilter(getResources().getColor(R.color.button_material_light), PorterDuff.Mode.MULTIPLY);
//                    categoryButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    categoryButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 50));

                    categoryButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent categoryIntent = new Intent(MainActivity.applicationContext, CategoryActivity.class);
                            Category categoryTapped = (Category) mCategories.get(position);
                            Helpers.debugLog("Categories", "Category tapped: " + categoryTapped.getDisplayName());
                            categoryIntent.putExtra("categoryJson", categoryTapped.categoryJson.toString());
                            startActivity(categoryIntent);
                        }
                    });

                } else {
                    categoryButton = (Button) convertView;
                }

                categoryButton.setText(mCategories.get(position).getDisplayName());
                return categoryButton;
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            Publisher.INSTANCE.removeArticleBodyDownloadCompleteListener(articleBodyDownloadCompleteListener);
        }
    }
}
