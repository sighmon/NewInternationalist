package au.com.newint.newinternationalist;

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;


public class ImageActivity extends ActionBarActivity {

    static String url;

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
        setTitle(url);
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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
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
            // TODO: Fix the vertical-centering css so it doesn't randomly appear too high
            String html = String.format("<html> <head> <style type='text/css'> body { padding: 0; margin: 0; } body img { } </style> </head> <body> <img src='%1$s' style='width: 100%%; position: relative; top: 50%%; transform: translateY(-50%%);' /> </body> </html>", url);

            // TODO: REMOVE Purely for remote debugging with chrome using chrome://inspect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }

            imageWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);

            return rootView;
        }
    }
}
