package au.com.newint.newinternationalist;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        // Set default preferences, the false on the end means it's only set once
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Log which SITE_URL we are using for debugging
        String siteURLString = (String) getVariableFromConfig(this, "SITE_URL");
        Log.i("SITE_URL", siteURLString);
        URL siteURL = null;
        try {
            siteURL = new URL(siteURLString + "issues.json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Get issues.json and save/update our cache
        new DownloadIssuesJSONTask().execute(siteURL);
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
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // Set a listener for home_cover taps
            final ImageButton home_cover = (ImageButton) rootView.findViewById(R.id.home_cover);
            home_cover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Handle cover clicked
                    Log.i("Cover", "Cover was clicked!");

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

            return rootView;
        }
    }

    private static String getVariableFromConfig(Context context, String string) {
        Resources resources = context.getResources();
        AssetManager assetManager = resources.getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(string);
        } catch (IOException e) {
            Log.e("Properties","Failed to open config property file");
            return null;
        }
    }

    private class DownloadIssuesJSONTask extends AsyncTask<URL, Integer, List> {

        @Override
        protected List doInBackground(URL... urls) {

            List magazineIssues = new ArrayList();

            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) urls[0].openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                assert urlConnection != null;
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                magazineIssues = readJsonStream(inputStream);
                Log.i("Issues.json", String.format("JSON has %1$d magazines.", magazineIssues.size()));
            }
            catch(Exception e) {
                Log.e("http", e.toString());
            }
            finally {
                assert urlConnection != null;
                urlConnection.disconnect();
            }

            return magazineIssues;
        }

        @Override
        protected void onPostExecute(List list) {
            super.onPostExecute(list);

            // TODO: Save to cache

            // TODO: Update home cover if there's a new issue
//            new DownloadMagazineCover().execute(coverURL, issueID);
        }
    }

    public List readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readMagazineArray(reader);
        }
        finally {
            reader.close();
        }
    }

    public List readMagazineArray(JsonReader reader) throws IOException {
        List magazines = new ArrayList();

        reader.beginArray();
        while (reader.hasNext()) {
            magazines.add(readMagazine(reader));
        }
        reader.endArray();
        return magazines;
    }

    public List readMagazine(JsonReader reader) throws IOException {

        List magazine = new ArrayList();

        // NOTE: List order is the same as the JSON feed.
        // PIX: can List order be trusted in Android?

        String coverURL = null;
        String editorsLetterHTML = null;
        String editorsName = null;
        String editorsPhoto = null;
        int id = 0;
        int number = 0;
        Date releaseDate = null;
        String title = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = reader.nextName();
            if (fieldName.equals("id")) {
                id = reader.nextInt();
                magazine.add(id);
            } else if (fieldName.equals("number")) {
                number = reader.nextInt();
                magazine.add(number);
            } else if (fieldName.equals("release")) {
                String releaseString = reader.nextString();
                DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    releaseDate = inputFormat.parse(releaseString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                magazine.add(releaseDate);
            } else if (fieldName.equals("title")) {
                title = reader.nextString();
                magazine.add(title);
            } else if (fieldName.equals("cover")) {
                // Get URL from next node.
                reader.beginObject();
                while (reader.hasNext()) {
                    String coverFieldName = reader.nextName();
                    if (coverFieldName.equals("url")) {
                        coverURL = reader.nextString();
                        magazine.add(coverURL);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } else if (fieldName.equals("editors_name")) {
                editorsName = reader.nextString();
                magazine.add(editorsName);
            } else if (fieldName.equals("editors_photo")) {
                // Get URL from next node.
                reader.beginObject();
                while (reader.hasNext()) {
                    String editorsFieldName = reader.nextName();
                    if (editorsFieldName.equals("url")) {
                        editorsPhoto = reader.nextString();
                        magazine.add(editorsPhoto);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } else if (fieldName.equals("editors_letter_html")) {
                editorsLetterHTML = reader.nextString();
                magazine.add(editorsLetterHTML);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return magazine;
    }

    private class DownloadMagazineCover extends AsyncTask<URL, Integer, String> {

        @Override
        protected String doInBackground(URL... params) {

            // TODO: Finish downloading the cover

            return null;
        }
    }
}
