package au.com.newint.newinternationalist;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.apache.http.cookie.Cookie;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    static boolean newIssueAdded = false;

    static Context applicationContext;
    static Resources applicationResources;

    ByteCache issuesJSONCache;

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


        Publisher.INSTANCE.issuesJSONCacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {
            @Override
            public void onLoadBackground(byte[] payload) {

                //TODO: numerous direct filesystem access here which should be abstracted with CSFs

                JsonElement root = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(payload)));
                JsonArray magazines = root.getAsJsonArray();

                Issue latestIssueOnFile = null;

                if (magazines!=null) {
                    JsonObject latestIssueOnlineJson = magazines.get(0).getAsJsonObject();
                    //latestIssue = new Issue()

                    JsonObject newestOnlineIssue = magazines.get(0).getAsJsonObject();
                    int newestOnlineIssueRailsId = newestOnlineIssue.get("id").getAsInt();
                    int magazinesOnFilesystem = Publisher.INSTANCE.numberOfIssues();

                    Log.i("Filesystem", String.format("Number of issues on filesystem: %1$d", magazinesOnFilesystem));
                    Log.i("www", String.format("Number of issues on www: %1$d", magazines.size()));

                    if (magazines.size() > magazinesOnFilesystem) {
                        // There are more issues online. Now check if it's a new or backissue
                        Issue latestIssue = Publisher.INSTANCE.latestIssue();
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
                        latestIssue = Publisher.INSTANCE.latestIssue();


                        //if (latestIssue!=null) latestIssue.getCover();

                    }
                }

            }

            //TODO: does this have to be in the foreground?

            @Override
            public void onLoad(byte[] payload) {
                Issue latestIssueOnFile = Publisher.INSTANCE.latestIssue();

                latestIssueOnFile.coverCacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {

                    @Override
                    public void onLoad(byte[] payload) {

                        Log.i("coverCSF..onLoad", "Received listener, showing cover.");

                        // Show cover
                        final ImageButton home_cover = (ImageButton) MainActivity.this.findViewById(R.id.home_cover);
                        if (home_cover != null) {
                            Log.i("coverCSF..onLoad", "calling decodeStream");

                            final Bitmap coverBitmap = BitmapFactory.decodeByteArray(payload,0,payload.length);
                            Log.i("coverCSF..onLoad", "decodeStream returned");
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
                                    home_cover.setImageBitmap(coverBitmap);
                                    home_cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                    home_cover.startAnimation(fadeInAnimation);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });
                            home_cover.startAnimation(fadeOutAnimation);
                        }

                    }

                    @Override
                    public void onLoadBackground(byte[] payload) {}
                });
            }
        });

        // Search intent
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i("Search","Searching for: " + query);
        }
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

            // Set a listener for home_cover taps
            final ImageButton home_cover = (ImageButton) rootView.findViewById(R.id.home_cover);
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

            // Set a listener for Login taps
            Button login = (Button) rootView.findViewById(R.id.home_login);
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent loginIntent = new Intent(rootView.getContext(), LoginActivity.class);
                    startActivity(loginIntent);
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

}
