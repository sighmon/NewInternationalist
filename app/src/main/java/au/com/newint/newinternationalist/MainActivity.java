package au.com.newint.newinternationalist;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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
import org.apache.http.cookie.Cookie;
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

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.SaveCallback;

public class MainActivity extends ActionBarActivity {

    static boolean newIssueAdded = false;

    static Context applicationContext;
    static Resources applicationResources;

    IabHelper mHelper;
    static Issue latestIssueOnFileBeforeUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }

        applicationContext = getApplicationContext();
        applicationResources = getResources();

        // Set default preferences, the false on the end means it's only set once
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);



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
                    Log.i("InApp", "In-app billing setup result: " + result);

                    // Ask Google Play for a products list on a background thread
                    ArrayList<String> additionalSkuList = new ArrayList<String>();
                    additionalSkuList.add("12monthauto");
                    additionalSkuList.add("1monthauto");
                    IabHelper.QueryInventoryFinishedListener mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
                        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                            if (result.isFailure()) {
                                // handle error
                                Log.i("InApp", "Query failed: " + result);
                                return;
                            }

                            // Check subscription inventory
                            Log.i("InApp", "Inventory: " + inventory);
                            SkuDetails yearlyAutomaticSubscription = inventory.getSkuDetails("12monthauto");
                            SkuDetails monthlyAutomaticSubscription = inventory.getSkuDetails("1monthauto");
                            Log.i("InApp", "12monthauto: " + yearlyAutomaticSubscription.getTitle() + yearlyAutomaticSubscription.getPrice());
                            Log.i("InApp", "1monthauto: " + monthlyAutomaticSubscription.getTitle() + monthlyAutomaticSubscription.getPrice());

                            // Check if the user has purchased the inventory
                            if (inventory.hasPurchase("12monthauto")) {
                                // TODO: See how multiple purchases work.. and renewals
                                Purchase purchase = inventory.getPurchase("12monthauto");
                                Log.i("InApp", "Purchase: " + purchase.toString());

                                Date purchaseDate = new Date(purchase.getPurchaseTime());

                                if (Helpers.isSubscriptionValid(purchaseDate, 12)) {
                                    // User has a valid subscription
                                    Publisher.INSTANCE.hasValidSubscription = true;
                                    for (Publisher.SubscriptionListener listener : Publisher.INSTANCE.subscriptionListeners) {
                                        Log.i("InApp", "Sending listener subscription valid.");
                                        // Pass in login success boolean
                                        listener.onUpdate(purchase);
                                    }
                                    Log.i("InApp", "Subscription expiry date: " + Helpers.subscriptionExpiryDate(purchaseDate, 12));
                                }
                            } else if (inventory.hasPurchase("1monthauto")) {
                                // TODO: See how multiple purchases work.. and renewals
                                Purchase purchase = inventory.getPurchase("1monthauto");
                                Log.i("InApp", "Purchase: " + purchase.toString());

                                Date purchaseDate = new Date(purchase.getPurchaseTime());

                                if (Helpers.isSubscriptionValid(purchaseDate, 1)) {
                                    // User has a valid subscription
                                    Publisher.INSTANCE.hasValidSubscription = true;
                                    for (Publisher.SubscriptionListener listener : Publisher.INSTANCE.subscriptionListeners) {
                                        Log.i("InApp", "Sending listener subscription valid.");
                                        // Pass in purchase if needed
                                        listener.onUpdate(purchase);
                                    }
                                    Log.i("InApp", "Subscription expiry date: " + Helpers.subscriptionExpiryDate(purchaseDate, 1));
                                }
                            }
                        }
                    };
                    mHelper.queryInventoryAsync(true, additionalSkuList, mQueryFinishedListener);
                }
            });
        }

        // Setup push notifications
        Parse.initialize(this, Helpers.getVariableFromConfig("PARSE_APP_ID"), Helpers.getVariableFromConfig("PARSE_CLIENT_KEY"));
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "Successfully subscribed to the broadcast channel.");
                } else {
                    Log.e("com.parse.push", "Failed to subscribe for push", e);
                }
            }
        });

        // Download the latest issues.json list of magazines
        Publisher.INSTANCE.issuesJSONCacheStreamFactory.preload("net", null, new CacheStreamFactory.CachePreloadCallback() {
            @Override
            public void onLoadBackground(byte[] payload) {

                //TODO: numerous direct filesystem access here which should be abstracted with CSFs

                JsonArray magazines = null;

                if (payload.length > 0) {

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

                    Log.i("Filesystem", String.format("Number of issues on filesystem: %1$d", magazinesOnFilesystem));
                    Log.i("www", String.format("Number of issues on www: %1$d", magazines.size()));

                    if (magazines.size() > magazinesOnFilesystem) {
                        // There are more issues online. Now check if it's a new or backissue

                        Issue latestIssue = Publisher.INSTANCE.latestIssue(); // hits filesystem but we are still in background
                        if (latestIssue != null) {
                            int newestFilesystemIssueRailsId = latestIssue.getID();

                            if (newestOnlineIssueRailsId != newestFilesystemIssueRailsId) {
                                // It's a new issue
                                Log.i("NewIssue", String.format("New issue available! Id: %1$d", newestOnlineIssueRailsId));
                                newIssueAdded = true;
                            }
                        }

                        for (JsonElement magazine : magazines) {
                            JsonObject jsonObject = magazine.getAsJsonObject();

                            int id = jsonObject.get("id").getAsInt();

                            File dir = new File(getApplicationContext().getFilesDir(), Integer.toString(id));
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
                    Log.i("ArticleBody", "Failed! Response is null");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.no_internet_dialog_message_article_body).setTitle(R.string.no_internet_dialog_title_article_body);

                    builder.setNegativeButton(R.string.no_internet_dialog_ok_button, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            //finish();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {

                    if (latestIssueOnFileBeforeUpdate != null && latestIssueOnFileBeforeUpdate != latestIssueOnFile) {

                        latestIssueOnFile.coverCacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {

                            @Override
                            public void onLoad(byte[] payload) {

                                Log.i("Cover", "New issue cover available, showing cover.");

                                // Show cover
                                final ImageView home_cover = (ImageView) MainActivity.this.findViewById(R.id.home_cover);
                                if (home_cover != null) {
                                    Log.i("coverCSF..onLoad", "calling decodeStream");
                                    final Bitmap coverBitmap = Helpers.bitmapDecode(payload);
                                    Log.i("coverCSF..onLoad", "decodeStream returned");
                                    animateUpdateImageViewWithBitmap(home_cover, coverBitmap);
                                }

                            }

                            @Override
                            public void onLoadBackground(byte[] payload) {
                            }
                        });
                    } else {
                        Log.i("LatestIssue", "No new issue available.");
                    }
                }
            }
        });

        // Search intent
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i("Search","Searching for: " + query);
        }

        // Attempt to login if credentials have been stored.
        String username = Helpers.getFromPrefs(Helpers.LOGIN_USERNAME_KEY, "");

        if (username != null && !username.equals("")) {
            // Try logging in!
            new SilentUserLoginTask(Helpers.getFromPrefs(Helpers.LOGIN_USERNAME_KEY, ""), Helpers.getPassword("")).execute();
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
        android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) menu.findItem(R.id.action_search).getActionView();
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
                // Log.i("Menu", "Settings pressed.");
                // Settings intent
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about:
                // Log.i("Menu", "About pressed.");
                // About intent
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.action_search:
                Log.i("Search", "Search tapped on Home view.");
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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // Check if we're logged in
            if (Publisher.INSTANCE.loggedIn) {
                // Set login text to Logged in
                Button loginButton = (Button) rootView.findViewById(R.id.home_login);
                loginButton.setText("Logged in");
            }

            // Add listener for login successful!
            loginListener = new Publisher.LoginListener() {

                @Override
                public void onUpdate(Object object) {
                    Button loginButton = (Button) rootView.findViewById(R.id.home_login);
                    loginButton.setText("Logged in");
                }
            };
            Publisher.INSTANCE.setLoggedInListener(loginListener);

            // Check if there's a valid subscription
            if (Publisher.INSTANCE.hasValidSubscription) {
                // Set subscription text to Thanks for Subscribing
                Button subscribeButton = (Button) rootView.findViewById(R.id.home_subscribe);
                subscribeButton.setText("Thanks for subscribing");
            }

            // Add listener for subscriptions
            subscriptionListener = new Publisher.SubscriptionListener() {

                @Override
                public void onUpdate(Object object) {
                    Button subscribeButton = (Button) rootView.findViewById(R.id.home_subscribe);
                    subscribeButton.setText("Thanks for subscribing");
                }
            };
            Publisher.INSTANCE.setSubscriptionListener(subscriptionListener);

            // Set a listener for home_cover taps
            final ImageView home_cover = (ImageView) rootView.findViewById(R.id.home_cover);
            home_cover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Cover tapped
                    Log.i("Cover", "Cover was tapped!");
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
            latestIssueOnFileBeforeUpdate.coverCacheStreamFactory.preload(null, "net", new CacheStreamFactory.CachePreloadCallback() {

                @Override
                public void onLoad(byte[] payload) {

                    Log.i("coverCSF..onLoad", "Received listener, showing cover.");

                    // Show cover
                    final ImageView home_cover = (ImageView) rootView.findViewById(R.id.home_cover);
                    if (home_cover != null) {
                        Log.i("coverCSF..onLoad", "calling decodeStream");
                        final Bitmap coverBitmap = Helpers.bitmapDecode(payload);
                        Log.i("coverCSF..onLoad", "decodeStream returned");
                        animateUpdateImageViewWithBitmap(home_cover, coverBitmap);
                    }

                }

                @Override
                public void onLoadBackground(byte[] payload) {
                }
            });

            return rootView;
        }

        @Override
        public void onPause() {
            super.onPause();
            Publisher.INSTANCE.removeDownloadCompleteListener(listener);
        }
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

            // List current cookies
            List<Cookie> cookies = Publisher.INSTANCE.cookieStore.getCookies();
            if( !cookies.isEmpty() ){
                for (Cookie cookie : cookies){
                    String cookieString = cookie.getName() + " : " + cookie.getValue();
                    Log.i("Home login", "Old cookie: " + cookieString);
                }
            }

            // Delete cookies.
            Publisher.INSTANCE.deleteCookieStore();

            // Try to connect
            HttpContext ctx = new BasicHttpContext();
            ctx.setAttribute(ClientContext.COOKIE_STORE, Publisher.INSTANCE.cookieStore);
            HttpPost post = new HttpPost(Helpers.getSiteURL() + "users/sign_in.json?username=" + mEmail);
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
                Log.i("Home login", "ClientProtocolException: " + e);
            } catch (IOException e) {
                Log.i("Home login", "IOException: " + e);
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

                } else if (responseStatusCode > 400 && responseStatusCode < 500) {
                    // Login was incorrect.
                    Log.i("Home login", "Failed with code: " + responseStatusCode);

                } else {
                    // Server error.
                    Log.i("Home login", "Failed with code: " + responseStatusCode + " and response: " + response.getStatusLine());
                }

            } else {
                // Error logging in
                Log.i("Home login", "Failed! Response is null");
            }

            return success;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {

                // Let listener know
                for (Publisher.LoginListener listener : Publisher.INSTANCE.loginListeners) {
                    Log.i("Home login", "Sending listener login success: True");
                    // Pass in login success boolean
                    listener.onUpdate(success);
                }

            } else {
                // Silently fail
            }
        }
    }
}
