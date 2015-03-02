package au.com.newint.newinternationalist;

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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.cookie.Cookie;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

        // Get SITE_URL from config variables
        String siteURLString = Helpers.getSiteURL();
        Log.i("SITE_URL", siteURLString);

        // Get issues.json and save/update our cache
        URL issuesURL = null;
        try {
            issuesURL = new URL(siteURLString + "issues.json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        issuesJSONCache = new ByteCache();

        File cacheDir = getApplicationContext().getCacheDir();
        File cacheFile = new File(cacheDir,"issues.json");

        issuesJSONCache.addMethod(new MemoryByteCacheMethod());
        issuesJSONCache.addMethod(new FileByteCacheMethod(cacheFile));
        issuesJSONCache.addMethod(new URLByteCacheMethod(issuesURL));

        new DownloadIssuesJSONTask().execute(issuesJSONCache);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

        Issue latestIssueOnFile;
        Publisher.UpdateListener listener;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // Add listener for login successful!
            listener = new Publisher.UpdateListener() {

                @Override
                public void onUpdate(Object object) {
                    // Check if we've got a cookie
                    List<Cookie> cookies = Publisher.INSTANCE.cookieStore.getCookies();
                    if (!cookies.isEmpty()) {
                        // Set login text to Logged in
                        Button loginButton = (Button) rootView.findViewById(R.id.home_login);
                        loginButton.setText("Logged in");
                    }
                }
            };
            Publisher.INSTANCE.setLoggedInListener(listener);

            // Register for DownloadComplete listener
            listener = new Publisher.UpdateListener() {
                @Override
                public void onUpdate(Object object) {

                    Issue issue = (Issue) object;

                    Log.i("DownloadComplete", "Received listener, showing cover.");

                    // Show cover
                    ImageButton home_cover = (ImageButton) rootView.findViewById(R.id.home_cover);
                    if (home_cover != null) {
                        Bitmap coverBitmap = BitmapFactory.decodeFile(issue.getCover().getPath());
                        home_cover.setImageBitmap(coverBitmap);
                        home_cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }

                    latestIssueOnFile = issue;
                }
            };
            Publisher.INSTANCE.setOnDownloadCompleteListener(listener);

            // Display latest cover if available on filesystem
            latestIssueOnFile = Publisher.INSTANCE.latestIssue();
            if (latestIssueOnFile != null) {
                File coverFile = latestIssueOnFile.getCover();

                if (coverFile != null && coverFile.exists()) {
                    // Show cover
                    ImageButton home_cover = (ImageButton) rootView.findViewById(R.id.home_cover);
                    if (home_cover != null) {
                        Bitmap coverBitmap = BitmapFactory.decodeFile(coverFile.getPath());
                        home_cover.setImageBitmap(coverBitmap);
                        home_cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }
                }
            }

            // Set a listener for home_cover taps
            final ImageButton home_cover = (ImageButton) rootView.findViewById(R.id.home_cover);
            home_cover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Cover tapped
                    Log.i("Cover", "Cover was tapped!");
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

    private class DownloadIssuesJSONTask extends AsyncTask<ByteCache, Integer, JsonArray> {

        @Override
        protected JsonArray doInBackground(ByteCache... caches) {

            JsonArray rootArray = null;

            ByteCache issuesJSONCache = caches[0];

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(issuesJSONCache.read("net"));
            BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);
            InputStreamReader inputStreamReader = new InputStreamReader(bufferedInputStream);
            JsonElement root = new JsonParser().parse(inputStreamReader);
            rootArray = root.getAsJsonArray();

            //JsonObject firstMagazine = root.getAsJsonArray().get(0).getAsJsonObject();
            //Log.i("firstMagazine", firstMagazine.toString());

            //Log.i("toJson", new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(firstMagazine));

            JsonArray magazines = rootArray;

            if (magazines != null) {
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
                    if (latestIssue!=null) latestIssue.getCover();

                }
            }

            return rootArray;
        }

        @Override
        protected void onPostExecute(JsonArray magazines) {
            super.onPostExecute(magazines);

            // ...

        }
    }
}
