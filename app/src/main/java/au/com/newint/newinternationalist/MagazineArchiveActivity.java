package au.com.newint.newinternationalist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;


public class MagazineArchiveActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magazine_archive);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MagazineArchiveFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_magazine_archive, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MagazineArchiveFragment extends Fragment {

        public MagazineArchiveFragment() {
        }

        ArrayList<Issue> magazines = null;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_magazine_archive, container, false);

            // Get Magazines
            magazines = Publisher.INSTANCE.getIssuesFromFilesystem();

            // Setup the GridView
            GridView gridview = (GridView) rootView.findViewById(R.id.magazineArchiveGridView);
            gridview.setAdapter(new ImageAdapter(rootView.getContext()));

            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    // On tap, move to magazine table of contents
                    Intent tableOfContentsIntent = new Intent(rootView.getContext(), TableOfContentsActivity.class);
                    // Pass issue through as a Parcel
                    tableOfContentsIntent.putExtra("issue", magazines.get(position));
                    startActivity(tableOfContentsIntent);
                }
            });

            return rootView;
        }

        public class ImageAdapter extends BaseAdapter {
            private Context mContext;

            public ImageAdapter(Context c) {
                mContext = c;
            }

            public int getCount() {

                return Publisher.INSTANCE.numberOfIssues();
            }

            public Object getItem(int position) {
                return null;
            }

            public long getItemId(int position) {
                return 0;
            }

            // convert the cover size in px to dp

            int coverWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 100, getResources().getDisplayMetrics());

            int coverHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 141, getResources().getDisplayMetrics());

            // create a new ImageView for each item referenced by the Adapter
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView;
                if (convertView == null) {  // if it's not recycled, initialize some attributes
                    imageView = new ImageView(mContext);
                    imageView.setLayoutParams(new GridView.LayoutParams(coverWidth, coverHeight));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setPadding(0, 0, 0, 0);
                } else {
                    imageView = (ImageView) convertView;
                }

                // Get/set the cover for this view.
                if (magazines != null) {
                    Issue issue = magazines.get(position);
                    // TODO: Get cover thumb from cache
                    File coverFile = Publisher.INSTANCE.latestIssue().getCover();
                    if (coverFile != null && coverFile.exists()) {
                        Bitmap coverBitmap = BitmapFactory.decodeFile(coverFile.getPath());
                        imageView.setImageBitmap(coverBitmap);
//                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }
                }

                return imageView;
            }
        }
    }
}
