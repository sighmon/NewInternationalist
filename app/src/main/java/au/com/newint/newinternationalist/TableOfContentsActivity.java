package au.com.newint.newinternationalist;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.JsonArray;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class TableOfContentsActivity extends ActionBarActivity {

    ByteCache articlesJSONCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_of_contents);
        Fragment tableOfContentsFragment = new TableOfContentsFragment();

        Issue issue = getIntent().getParcelableExtra("issue");

        // Send issue to fragment
        Bundle bundle = new Bundle();
        bundle.putParcelable("issue", issue);
        tableOfContentsFragment.setArguments(bundle);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, tableOfContentsFragment)
                    .commit();
        }

        // Magazine title from Parcel issue
        setTitle(issue.getTitle());

        // Get SITE_URL
        String siteURLString = (String) MainActivity.getVariableFromConfig(this, "SITE_URL");

        // Get articles.json (actually issueID.json) and save/update our cache
        URL articlesURL = null;
        try {
            articlesURL = new URL(siteURLString + "issues/" + issue.getID() + ".json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        articlesJSONCache = new ByteCache();

        File cacheDir = getApplicationContext().getCacheDir();
        File cacheFile = new File(cacheDir, issue.getID() + ".json");

        articlesJSONCache.addMethod(new MemoryByteCacheMethod());
        articlesJSONCache.addMethod(new FileByteCacheMethod(cacheFile));
        articlesJSONCache.addMethod(new URLByteCacheMethod(articlesURL));

        new Publisher.DownloadArticlesJSONTask().execute(articlesJSONCache, issue);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_table_of_contents, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case android.R.id.home:
                // Handles a back/up button press and returns to previous Activity
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class TableOfContentsFragment extends Fragment {

        public TableOfContentsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_table_of_contents, container, false);

            final RecyclerView recList = (RecyclerView) rootView.findViewById(R.id.card_list);
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(rootView.getContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            // Get issue from bundle
            Bundle bundle = this.getArguments();
            final Issue issueFromActivity;
            if (bundle != null) {
                issueFromActivity = bundle.getParcelable("issue");
            } else {
                issueFromActivity = new Issue(Publisher.latestIssue().getID());
            }

            final TableOfContentsAdapter adapter = new TableOfContentsAdapter(issueFromActivity);
            recList.setAdapter(adapter);

            // Register for DownloadComplete listener
            Publisher.ArticlesDownloadCompleteListener listener = new Publisher.ArticlesDownloadCompleteListener() {

                @Override
                public void onArticlesDownloadComplete(JsonArray articles) {
                    Log.i("ArticlesReady", "Received listener, refreshing articles view.");
                    // TODO: Work out how to refresh adapter
                    adapter.notifyDataSetChanged();
                    Publisher.articleListeners.clear();
                }
            };
            Publisher.setOnArticlesDownloadCompleteListener(listener);

            return rootView;
        }

        // Adapter for CardView
        public class TableOfContentsAdapter extends RecyclerView.Adapter<TableOfContentsAdapter.TableOfContentsViewHolder> {

            public Issue issue;

            public TableOfContentsAdapter(Issue issue) {
                this.issue = issue;
            }

            @Override
            public TableOfContentsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.
                        from(parent.getContext()).
                        inflate(R.layout.fragment_table_of_contents_card_view, parent, false);
//                  View itemView = parent.getRootView().findViewById(R.id.card_view);

                return new TableOfContentsViewHolder(itemView);
            }

            @Override
            public int getItemCount() {
                return issue.getArticles().size();
            }

            @Override
            public void onBindViewHolder(TableOfContentsViewHolder holder, int position) {
                Article article = issue.getArticles().get(position);
                holder.articleTitleTextView.setText(article.getTitle());
                holder.articleTeaserTextView.setText(article.getTeaser());
            }


            public class TableOfContentsViewHolder extends RecyclerView.ViewHolder {

                public TextView articleTitleTextView;
                public TextView articleTeaserTextView;

                public TableOfContentsViewHolder(View itemView) {
                    super(itemView);
                    articleTitleTextView = (TextView) itemView.findViewById(R.id.article_title);
                    articleTeaserTextView = (TextView) itemView.findViewById(R.id.article_teaser);
                }
            }
        }
    }
}
