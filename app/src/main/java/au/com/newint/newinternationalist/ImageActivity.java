package au.com.newint.newinternationalist;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;


public class ImageActivity extends AppCompatActivity {

    static String url;
    static Article article;
    static Issue issue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ImageActivityFragment())
                    .commit();
        }

        url = getIntent().getStringExtra("url");
        article = getIntent().getParcelableExtra("article");
        issue = getIntent().getParcelableExtra("issue");
        if (article != null) {
            setTitle(article.getTitle());
        } else if (issue != null) {
            setTitle(issue.getTitle());
        } else {
            setTitle(R.string.image_zoom_default_title);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Send Google Analytics if the user allows it
        String analyticsString = getResources().getString(R.string.title_activity_image);
        if (url != null) {
            String[] urlComponents = url.split("/");
            analyticsString = urlComponents[urlComponents.length - 1];
        }
        if (issue != null) {
            analyticsString += " (" + issue.getNumber() + ")";
        } else if (article != null) {
            analyticsString += " (" + article.parentIssue.getNumber() + ")";
        }
        Helpers.sendGoogleAnalytics(analyticsString);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image, menu);
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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Image fragment
     */
    public static class ImageActivityFragment extends Fragment {

        public ImageActivityFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_image, container, false);

            WebView imageWebView = (WebView) rootView.findViewById(R.id.image_web_view);
            imageWebView.getSettings().setBuiltInZoomControls(true);
            imageWebView.setBackgroundColor(getResources().getColor(R.color.background_material_dark));
            // Oh CSS you difficult beast. Using divs displaying as tables to centre the image
            String html = String.format("<html> <head> <style type='text/css'> body { padding: 0; margin: 0; } body img { width: 100%%; } </style> </head> <body> <div style='display: table; position: absolute; height: 100%%; width: 100%%;'> <div style=' display: table-cell; vertical-align: middle;'> <img src='%1$s' /> </div> </div> </body> </html>", url);

            imageWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);

            return rootView;
        }
    }
}
