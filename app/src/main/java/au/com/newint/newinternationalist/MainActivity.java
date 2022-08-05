package au.com.newint.newinternationalist;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.com.newint.newinternationalist.util.IabHelper;
import au.com.newint.newinternationalist.util.IabResult;
import au.com.newint.newinternationalist.util.Inventory;
import au.com.newint.newinternationalist.util.Purchase;
import au.com.newint.newinternationalist.util.SkuDetails;

//import au.com.newint.newinternationalist.util.RegistrationIntentService;


public class MainActivity extends AppCompatActivity {

    static boolean newIssueAdded = false;

    static Context applicationContext;
    static Resources applicationResources;

    IabHelper mHelper;
    static Issue latestIssueOnFileBeforeUpdate;

    static ProgressBar loadingSpinner;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    @Override
    protected void onResume() {
        super.onResume();

        // Send Google Analytics if the user allows it
        Helpers.sendGoogleAnalytics(getResources().getString(R.string.home_title));

        // Register the deep link referrer if the user allows it
        if (this.getIntent().getData() != null) {
            Helpers.registerGoogleConversionsReferrer(this.getIntent());
        }

        // Register the push notification receiver
//        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Crash reporting example
//        Helpers.crashLog("Newint crash log test.");
//        Helpers.crash("Newint non-fatal test crash!");

        if (getIntent().getBooleanExtra("EXIT", false)) {
            System.exit(0);
        }

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }

        // Set default preferences, the false on the end means it's only set once
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Handle deep link
        onNewIntent(getIntent());

        // Setup in-app billing
        mHelper = Helpers.setupIabHelper(this);

        if (!Helpers.emulator) {
            // Only startSetup if not running in an emulator
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem.
                        Log.d("InApp", "Problem setting up In-app Billing: " + result);
                    }
                    // Hooray, IAB is fully set up!
                    Helpers.debugLog("InApp", "In-app billing setup result: " + result);

                    // Ask Google Play for a products list on a background thread
                    ArrayList<String> additionalSkuList = new ArrayList<String>();
                    additionalSkuList.add("12monthauto");
                    additionalSkuList.add("1monthauto");
                    IabHelper.QueryInventoryFinishedListener mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
                        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                            if (result.isFailure()) {
                                // handle error
                                Helpers.debugLog("InApp", "Query failed: " + result);
                                return;
                            }

                            // Check subscription inventory
                            Helpers.debugLog("InApp", "Inventory: " + inventory);
                            SkuDetails yearlyAutomaticSubscription = inventory.getSkuDetails("12monthauto");
                            SkuDetails monthlyAutomaticSubscription = inventory.getSkuDetails("1monthauto");
                            Helpers.debugLog("InApp", "12monthauto: " + yearlyAutomaticSubscription.getTitle() + yearlyAutomaticSubscription.getPrice());
                            Helpers.debugLog("InApp", "1monthauto: " + monthlyAutomaticSubscription.getTitle() + monthlyAutomaticSubscription.getPrice());

                            // Check if the user has purchased the inventory
                            if (inventory.hasPurchase("12monthauto")) {
                                // TODO: See how multiple purchases work.. and renewals
                                Purchase purchase = inventory.getPurchase("12monthauto");
                                Helpers.debugLog("InApp", "Purchase: " + purchase.toString());

                                Date purchaseDate = new Date(purchase.getPurchaseTime());

                                if (Helpers.isSubscriptionValid(purchaseDate, 12)) {
                                    // User has a valid subscription
                                    Publisher.INSTANCE.hasValidSubscription = true;
                                    for (Publisher.SubscriptionListener listener : Publisher.INSTANCE.subscriptionListeners) {
                                        Helpers.debugLog("InApp", "Sending listener subscription valid.");
                                        // Pass in login success boolean
                                        listener.onUpdate(purchase);
                                    }
                                    Helpers.debugLog("InApp", "Subscription expiry date: " + Helpers.subscriptionExpiryDate(purchaseDate, 12));
                                }
                            } else if (inventory.hasPurchase("1monthauto")) {
                                // TODO: See how multiple purchases work.. and renewals
                                Purchase purchase = inventory.getPurchase("1monthauto");
                                Helpers.debugLog("InApp", "Purchase: " + purchase.toString());

                                Date purchaseDate = new Date(purchase.getPurchaseTime());

                                if (Helpers.isSubscriptionValid(purchaseDate, 1)) {
                                    // User has a valid subscription
                                    Publisher.INSTANCE.hasValidSubscription = true;
                                    for (Publisher.SubscriptionListener listener : Publisher.INSTANCE.subscriptionListeners) {
                                        Helpers.debugLog("InApp", "Sending listener subscription valid.");
                                        // Pass in purchase if needed
                                        listener.onUpdate(purchase);
                                    }
                                    Helpers.debugLog("InApp", "Subscription expiry date: " + Helpers.subscriptionExpiryDate(purchaseDate, 1));
                                }
                            }
                        }
                    };
                    try {
                        mHelper.queryInventoryAsync(true, additionalSkuList, mQueryFinishedListener);
                    } catch (Exception e) {
                        // Handle no in-app purchase available
                    }
                }
            });
        }

        // Search intent
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Helpers.debugLog("Search","Searching for: " + query);
        }

        // Attempt to login if credentials have been stored.
        String username = Helpers.getFromPrefs(Helpers.LOGIN_USERNAME_KEY, "");

        if (username != null && !username.equals("")) {
            // Try logging in!
            new SilentUserLoginTask(username, Helpers.getPassword("")).execute();
        }
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        String data = intent.getDataString();
        Bundle extras = intent.getExtras();
        if (data == null && extras != null) {
            // Push notification - handle extras
            String railsID = (String) extras.get("railsID");
            String issueID = (String) extras.get("issueID");
            String articleID = (String) extras.get("articleID");
            if (railsID != null) {
                data = "/issues/" + railsID;
            } else if (issueID != null && articleID != null) {
                data = "/issues/" + issueID + "/articles/" + articleID;
            }
        }
        if (data != null && (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_MAIN.equals(action))) {
            // Parse magazine and or article ID and start intent
            if (data.matches("(.*)/issues/(\\d+)/articles/(\\d+)") || data.matches("(.*)/issues/(\\d+)/articles/(\\d+)/")) {
                // It's an article deep link
                String[] components = data.split("/");
                final ArrayList<String> issueArticleIDs = new ArrayList<String>();
                for (String component : components) {
                    if (component.matches("(\\d+)")) {
                        // It's a digit, add it to our ID array
                        issueArticleIDs.add(component);
                    }
                }
                if (issueArticleIDs.size() == 2) {
                    final Issue issueInUrl = new Issue(Integer.parseInt(issueArticleIDs.get(0)));
                    issueInUrl.preloadArticles(new CacheStreamFactory.CachePreloadCallback() {
                        @Override
                        public void onLoad(byte[] payload) {
                            Article articleInUrl = issueInUrl.getArticleWithID(Integer.parseInt(issueArticleIDs.get(1)));
                            if (issueInUrl != null && articleInUrl != null) {
                                Intent articleIntent = new Intent(applicationContext, ArticleActivity.class);
                                articleIntent.putExtra("issue", issueInUrl);
                                articleIntent.putExtra("article", articleInUrl);
                                startActivity(articleIntent);
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
            } else if (data.matches("(.*)/issues/(\\d+)") || data.matches("(.*)/issues/(\\d+)/")) {
                // It's a deep link to an issue
                int issueID = Integer.parseInt(data.replaceAll("\\D+", ""));
                try {
                    Issue issueInUrl = new Issue(issueID);
                    Intent tableOfContentsIntent = new Intent(applicationContext, TableOfContentsActivity.class);
                    // Pass issue through as a Parcel
                    tableOfContentsIntent.putExtra("issue", issueInUrl);
                    startActivity(tableOfContentsIntent);
                } catch (Exception e) {
                    Log.e("NewIntent", "ERROR: error finding issue: " + issueID);
                }
            } else if (data.matches("(.*)/issues")) {
                // It's a deep link to the magazine archive
                Intent magazineArchiveIntent = new Intent(applicationContext, MagazineArchiveActivity.class);
                startActivity(magazineArchiveIntent);
            } else if (data.matches("(.*)/categories(.*)")) {
                // It's a deep link to the categories page
                Intent categoriesIntent = new Intent(applicationContext, CategoriesActivity.class);
                startActivity(categoriesIntent);
            } else {
                // Ignore it
                Log.e("NewIntent", "ERROR: badly formed data URL: " + data);
            }
        }
    }

    private static void animateUpdateImageViewWithBitmap(final ImageView imageView, final Bitmap bitmap) {

        if (imageView != null && bitmap != null) {
            Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
            final Animation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
            fadeOutAnimation.setDuration(300);
            fadeInAnimation.setDuration(300);
            fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    imageView.setImageBitmap(bitmap);
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imageView.startAnimation(fadeInAnimation);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            imageView.startAnimation(fadeOutAnimation);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Dispose of the in-app helper
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                // Helpers.debugLog("Menu", "Settings pressed.");
                // Settings intent
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about:
                // Helpers.debugLog("Menu", "About pressed.");
                // About intent
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.action_search:
                Helpers.debugLog("Search", "Search tapped on Home view.");
//                onSearchRequested();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MainFragment extends Fragment {

        public MainFragment() {
        }

        Publisher.UpdateListener listener;
        Publisher.LoginListener loginListener;
        Publisher.SubscriptionListener subscriptionListener;
        static View rootView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // Setup loading spinner
            loadingSpinner = (ProgressBar) rootView.findViewById(R.id.home_cover_loading_spinner);
            loadingSpinner.setVisibility(View.VISIBLE);

            // Download the latest issues.json list of magazines
            Publisher.INSTANCE.issuesJSONCacheStreamFactory.preload("net", null, new CacheStreamFactory.CachePreloadCallback() {
                @Override
                public void onLoadBackground(byte[] payload) {

                    JsonArray magazines = null;

                    if (payload != null && payload.length > 0) {

                        JsonElement root = new JsonParser().parse(new String(payload));
                        //TODO: throws an exception (which one?) if the payload is empty instead of returning null
                        // IllegalStateException

                        magazines = root.getAsJsonArray();
                    }

                    Issue latestIssueOnFile = null;

                    if (magazines != null) {
                        JsonObject latestIssueOnlineJson = magazines.get(0).getAsJsonObject();
                        //latestIssue = new Issue()

                        JsonObject newestOnlineIssue = magazines.get(0).getAsJsonObject();
                        int newestOnlineIssueRailsId = newestOnlineIssue.get("id").getAsInt();
                        int magazinesOnFilesystem = Publisher.INSTANCE.numberOfIssues();

                        Helpers.debugLog("Filesystem", String.format("Number of issues on filesystem: %1$d", magazinesOnFilesystem));
                        Helpers.debugLog("www", String.format("Number of issues on www: %1$d", magazines.size()));

                        if (magazines.size() > magazinesOnFilesystem) {
                            // There are more issues online. Now check if it's a new or backissue

                            Issue latestIssue = Publisher.INSTANCE.latestIssue(); // hits filesystem but we are still in background
                            if (latestIssue != null) {
                                int newestFilesystemIssueRailsId = latestIssue.getID();

                                if (newestOnlineIssueRailsId != newestFilesystemIssueRailsId) {
                                    // It's a new issue
                                    Helpers.debugLog("NewIssue", String.format("New issue available! Id: %1$d", newestOnlineIssueRailsId));
                                    newIssueAdded = true;
                                }
                            }

                            for (JsonElement magazine : magazines) {
                                JsonObject jsonObject = magazine.getAsJsonObject();

                                int id = jsonObject.get("id").getAsInt();

                                File dir = new File(Helpers.getStorageDirectory(), Integer.toString(id));
                                dir.mkdirs();

                                File file = new File(dir, "issue.json");

                                try {
                                    Writer w = new FileWriter(file);

                                    new Gson().toJson(jsonObject, w);

                                    w.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            //TODO: this should be done in main activity in response to a listener
                            Publisher.INSTANCE.issuesList = null;

                            latestIssue = Publisher.INSTANCE.latestIssue(); //hits filesystem but we are in the background still


                            //if (latestIssue!=null) latestIssue.getCover();

                        }
                    }

                }

                @Override
                public void onLoad(byte[] payload) {

                    Issue latestIssueOnFile = Publisher.INSTANCE.latestIssue();

                    //if latestIssueOnFile==null we probably have no internet, and this is the first run
                    //TODO: DRY this up, maybe make a helper?

                    if (latestIssueOnFile == null) {
                        Helpers.debugLog("ArticleBody", "ERROR! No latestIssue() on fileSystem.");
                        Activity alertActivity = getActivity();
                        if (alertActivity != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(alertActivity);
                            builder.setMessage(R.string.no_internet_dialog_message_article_body).setTitle(R.string.no_internet_dialog_title_article_body);

                            builder.setNegativeButton(R.string.no_internet_dialog_ok_button, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                    //finish();
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        loadingSpinner.setVisibility(View.GONE);
                    } else {

                        if (latestIssueOnFileBeforeUpdate == null || (latestIssueOnFileBeforeUpdate != null && latestIssueOnFileBeforeUpdate.getID() != latestIssueOnFile.getID())) {

                            latestIssueOnFile.coverCacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {

                                @Override
                                public void onLoad(byte[] payload) {

                                    // Stop loading spinner
                                    loadingSpinner.setVisibility(View.GONE);

                                    Helpers.debugLog("Cover", "New issue cover available, showing cover.");

                                    // Show cover
                                    final ImageView home_cover = (ImageView) rootView.findViewById(R.id.home_cover);
                                    if (home_cover != null) {
                                        Helpers.debugLog("coverCSF..onLoad", "calling decodeStream");
                                        final Bitmap coverBitmap = Helpers.bitmapDecode(payload);
                                        Helpers.debugLog("coverCSF..onLoad", "decodeStream returned");
                                        animateUpdateImageViewWithBitmap(home_cover, coverBitmap);
                                    }

                                }

                                @Override
                                public void onLoadBackground(byte[] payload) {
                                }
                            });
                        } else {
                            Helpers.debugLog("LatestIssue", "No new issue available.");
                        }
                    }
                }
            });

            // Check if we're logged in
            if (Publisher.INSTANCE.loggedIn) {
                // Set login text to Logged in
                setLoginTextToLoggedIn();
            }

            // Add listener for login successful!
            loginListener = new Publisher.LoginListener() {

                @Override
                public void onUpdate(Object object) {
                    Activity mainActivity = getActivity();
                    if (mainActivity != null) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // Set login text.
                                setLoginTextToLoggedIn();

                            }
                        });
                    }
                }
            };
            Publisher.INSTANCE.setLoggedInListener(loginListener);

            // Check if there's a valid subscription
            if (Publisher.INSTANCE.hasValidSubscription) {
                // Set subscription text to Thanks for Subscribing
                setSubscribeTextToSubscribed();
            }

            // Add listener for subscriptions
            subscriptionListener = new Publisher.SubscriptionListener() {

                @Override
                public void onUpdate(Object object) {
                    Activity mainActivity = getActivity();
                    if (mainActivity != null) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // Set subscribe text.
                                setSubscribeTextToSubscribed();
                                // Set logged-in text so not to confuse in-app purchase buyers.
                                setLoginTextToLoggedIn();
                            }
                        });
                    }
                }
            };
            Publisher.INSTANCE.setSubscriptionListener(subscriptionListener);

            // Set a listener for home_cover taps
            final ImageView home_cover = (ImageView) rootView.findViewById(R.id.home_cover);
            home_cover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Cover tapped
                    Helpers.debugLog("Cover", "Cover was tapped!");
                    Issue latestIssueOnFile = Publisher.INSTANCE.latestIssue();
                    if(latestIssueOnFile!=null) {
                        Intent tableOfContentsIntent = new Intent(rootView.getContext(), TableOfContentsActivity.class);
                        // Pass issue through as a Parcel
                        tableOfContentsIntent.putExtra("issue", latestIssueOnFile);
                        startActivity(tableOfContentsIntent);
                    }
                }
            });

            home_cover.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Set 50% black overlay
                            home_cover.setColorFilter(Color.argb(125, 0, 0, 0));
                            return false;
                        case MotionEvent.ACTION_UP:
                            // Remove overlay
                            home_cover.setColorFilter(null);
                            return false;
                        default:
                            // Do nothing
                            return false;
                    }
                }
            });

            // Set a listener for Magazine Archive taps
            Button magazineArchive = (Button) rootView.findViewById(R.id.home_magazine_archive);
            magazineArchive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent magazineArchiveIntent = new Intent(rootView.getContext(), MagazineArchiveActivity.class);
                    startActivity(magazineArchiveIntent);
                }
            });

            // Set a listener for Categories taps
            Button categoriesButton = (Button) rootView.findViewById(R.id.home_categories);
            categoriesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent categoriesIntent = new Intent(rootView.getContext(), CategoriesActivity.class);
                    startActivity(categoriesIntent);
                }
            });

            // Set a listener for Subscribe taps
            Button subscribeButton = (Button) rootView.findViewById(R.id.home_subscribe);
            subscribeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent subscribeIntent = new Intent(rootView.getContext(), SubscribeActivity.class);
                    startActivity(subscribeIntent);
                }
            });

            // Set a listener for Login taps
            Button login = (Button) rootView.findViewById(R.id.home_login);
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent loginIntent = new Intent(rootView.getContext(), LoginActivity.class);
                    startActivity(loginIntent);
                }
            });

            // If cover is already on filesystem, show it before updating issues.json
            latestIssueOnFileBeforeUpdate = Publisher.INSTANCE.latestIssue();
            if (latestIssueOnFileBeforeUpdate != null) {
                latestIssueOnFileBeforeUpdate.coverCacheStreamFactory.preload(null, "net", new CacheStreamFactory.CachePreloadCallback() {

                    @Override
                    public void onLoad(byte[] payload) {

                        Helpers.debugLog("coverCSF..onLoad", "Received listener, showing cover.");

                        // Stop loading spinner
                        loadingSpinner.setVisibility(View.GONE);

                        // Show cover
                        final ImageView home_cover = (ImageView) rootView.findViewById(R.id.home_cover);
                        if (home_cover != null) {
                            Helpers.debugLog("coverCSF..onLoad", "calling decodeStream");
                            final Bitmap coverBitmap = Helpers.bitmapDecode(payload);
                            Helpers.debugLog("coverCSF..onLoad", "decodeStream returned");
                            animateUpdateImageViewWithBitmap(home_cover, coverBitmap);
                        }

                    }

                    @Override
                    public void onLoadBackground(byte[] payload) {
                    }
                });
            }

            return rootView;
        }

        @Override
        public void onPause() {
            super.onPause();
            Publisher.INSTANCE.removeDownloadCompleteListener(listener);
        }
    }

    // Push notifications
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Helpers.debugLog(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public static void setLoginTextToLoggedIn() {
        Button loginButton = (Button) MainFragment.rootView.findViewById(R.id.home_login);
        loginButton.setText(R.string.home_logged_in);
    }

    public static void setSubscribeTextToSubscribed() {
        Button subscribeButton = (Button) MainFragment.rootView.findViewById(R.id.home_subscribe);
        subscribeButton.setText(R.string.home_subscribed);
    }

    // User login to Rails in the background, but silently fail
    public class SilentUserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        SilentUserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            boolean success = false;

            // Try logging into Rails for authentication.
            DefaultHttpClient httpclient = new DefaultHttpClient();

            // TODO: Try http://developer.android.com/reference/java/net/HttpURLConnection.html
            // TODO: Use URLCacheStreamFactory here.

            // Try to connect
            HttpContext ctx = new BasicHttpContext();
            ctx.setAttribute(ClientContext.COOKIE_STORE, Publisher.INSTANCE.cookieStore);
            HttpPost post = new HttpPost(Helpers.getSiteURL() + "users/sign_in.json");
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse response = null;

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("user[login]", mEmail));
                nameValuePairs.add(new BasicNameValuePair("user[password]", mPassword));
                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Save username to SharedPreferences
                Helpers.saveToPrefs(Helpers.LOGIN_USERNAME_KEY,mEmail);

                // Execute HTTP Post Request
                response = httpclient.execute(post, ctx);

            } catch (ClientProtocolException e) {
                Helpers.debugLog("Home login", "ClientProtocolException: " + e);
            } catch (IOException e) {
                Helpers.debugLog("Home login", "IOException: " + e);
            }

            int responseStatusCode;
            Publisher.INSTANCE.loggedIn = false;
            if (response != null) {
                responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode > 200 && responseStatusCode < 300) {
                    // Login was successful, we should have a cookie
                    success = true;
                    Publisher.INSTANCE.loggedIn = true;

                    Helpers.savePassword(mPassword);

                    // Let listener know
                    for (Publisher.LoginListener listener : Publisher.INSTANCE.loginListeners) {
                        Helpers.debugLog("Login", "Sending listener login success: True");
                        // Pass in login success boolean
                        listener.onUpdate(success);
                    }

                } else if (responseStatusCode > 400 && responseStatusCode < 500) {
                    // Login was incorrect.
                    Helpers.debugLog("Home login", "Failed with code: " + responseStatusCode);

                } else {
                    // Server error.
                    Helpers.debugLog("Home login", "Failed with code: " + responseStatusCode + " and response: " + response.getStatusLine());
                }

            } else {
                // Error logging in
                Helpers.debugLog("Home login", "Failed! Response is null");
            }

            return success;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {

                // Let listener know
                for (Publisher.LoginListener listener : Publisher.INSTANCE.loginListeners) {
                    Helpers.debugLog("Home login", "Sending listener login success: True");
                    // Pass in login success boolean
                    listener.onUpdate(success);
                }

            } else {
                // Silently fail
            }
        }
    }
}
