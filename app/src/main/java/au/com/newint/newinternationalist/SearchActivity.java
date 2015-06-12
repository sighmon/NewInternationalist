package au.com.newint.newinternationalist;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SearchActivity extends ActionBarActivity {

    static String searchQuery;
    static ArrayList<Issue> issuesArray;
    static ArrayList<ArrayList<Article>> filteredIssueArticlesArray;
    static ProgressDialog loadingProgressDialog;
    static Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SearchFragment())
                    .commit();
        }

        intent = getIntent();
        searchQuery = intent.getStringExtra(SearchManager.QUERY);

        // Set the activity title
        setTitle("Results for: " + searchQuery);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);

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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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
    public static class SearchFragment extends Fragment {

        public SearchFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_search, container, false);

            // Recycler view search results as cards and headers
            final RecyclerView recList = (RecyclerView) rootView.findViewById(R.id.search_results_card_list);
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(rootView.getContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            final SearchAdapter adapter = new SearchAdapter();
            recList.setAdapter(adapter);

            return rootView;
        }

        // Adapter for CardView
        public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

            public ArrayList<Object> listElements;
            public Issue issue;
            private static final int TYPE_HEADER = 0;
            private static final int TYPE_ARTICLE = 1;

            public SearchAdapter() {

                listElements = new ArrayList<>();

                // Set a loading indicator going
                loadingProgressDialog = new ProgressDialog(getActivity());
                loadingProgressDialog.setTitle(getResources().getString(R.string.search_loading_progress_title));
                loadingProgressDialog.setMessage(getResources().getString(R.string.search_loading_progress_message));
                loadingProgressDialog.show();

                // Perform the serach in an AsyncTask

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {

                        // Build up the issuesArray
                        issuesArray = Publisher.INSTANCE.getIssuesFromFilesystem();

                        // Get the intent, verify the action and get the query
                        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

                            // Perform the search
                            filterArticlesForSearchQuery(searchQuery);
                        }

                        for (ArrayList<Article> articleList : filteredIssueArticlesArray) {
                            if (articleList.size() <= 0) {
                                continue;
                            }
                            Issue issue = new Issue(articleList.get(0).getIssueID());
                            listElements.add(issue);
                            for (Article article : articleList) {
                                listElements.add(article);
                            }
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        // After loading finishes dismiss the loading dialog
                        loadingProgressDialog.dismiss();
                        notifyDataSetChanged();
                    }
                }.execute();
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = null;
                if (viewType == 0) {
                    // Header
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_search_header, parent, false);
                    return new SearchHeaderViewHolder(itemView);

                } else if (viewType == 1) {
                    // Article
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_search_card_view, parent, false);
                    return new SearchArticleViewHolder(itemView);

                } else {
                    // Uh oh... didn't match view type.
                    return null;
                }
            }

            @Override
            public int getItemCount() {
                return listElements.size();
            }

            @Override
            public int getItemViewType(int position) {
                if (isPositionHeader(position)) {
                    return TYPE_HEADER;
                }
                return TYPE_ARTICLE;
            }

            private boolean isPositionHeader(int position) {
                return listElements.get(position) instanceof Issue;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                if (holder instanceof SearchHeaderViewHolder) {
                    // Header
                    issue = (Issue) listElements.get(position);
                    DateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
                    String issueHeaderString = Integer.toString(issue.getNumber()) + " - " + issue.getTitle() + "\n\n" + dateFormat.format(issue.getRelease());
                    ((SearchHeaderViewHolder) holder).searchResultsHeader.setText(issueHeaderString);

                    // Set cover image
                    final ImageView coverImageView = ((SearchHeaderViewHolder) holder).issueCoverImageView;

                    // Set the loading cover for recycled views
//                    Bitmap defaultCoverBitmap = BitmapFactory.decodeResource(MainActivity.applicationContext.getResources(), R.drawable.home_cover);
//                    coverImageView.setImageBitmap(defaultCoverBitmap);

                    issue.getCoverCacheStreamFactoryForSize((int) getResources().getDimension(R.dimen.search_results_cover_width)).preload(new CacheStreamFactory.CachePreloadCallback() {
                        @Override
                        public void onLoad(byte[] payload) {
                            Bitmap coverBitmap = BitmapFactory.decodeByteArray(payload,0,payload.length);
                            coverImageView.setImageBitmap(coverBitmap);

                        }

                        @Override
                        public void onLoadBackground(byte[] payload) {

                        }
                    });

                } else if (holder instanceof SearchArticleViewHolder) {
                    // Article
                    Article article = (Article) listElements.get(position);
                    SearchArticleViewHolder searchArticleViewHolder = ((SearchArticleViewHolder) holder);
                    searchArticleViewHolder.articleTitleTextView.setText(article.getTitle());
                    String articleTeaser = article.getTeaser();
                    if (articleTeaser != null && !articleTeaser.isEmpty()) {
                        searchArticleViewHolder.articleTeaserTextView.setVisibility(View.VISIBLE);
                        searchArticleViewHolder.articleTeaserTextView.setText(Html.fromHtml(articleTeaser));
                    } else {
                        // Remove teaser view.
                        searchArticleViewHolder.articleTeaserTextView.setVisibility(View.GONE);
                    }

                    ArrayList<Image> images = article.getImages();
                    final ImageView articleImageView = searchArticleViewHolder.articleImageView;
                    if (images.size() > 0) {
                        images.get(0).getImageCacheStreamFactoryForSize(MainActivity.applicationContext.getResources().getDisplayMetrics().widthPixels).preload(new CacheStreamFactory.CachePreloadCallback() {
                            @Override
                            public void onLoad(byte[] payload) {
                                if (payload != null && payload.length > 0) {
                                    Bitmap coverBitmap = BitmapFactory.decodeByteArray(payload, 0, payload.length);
                                    articleImageView.setImageBitmap(coverBitmap);

                                }
                            }

                            @Override
                            public void onLoadBackground(byte[] payload) {

                            }
                        });
                    } else {
                        articleImageView.setVisibility(View.GONE);
                    }
                }
            }


            public class SearchArticleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView articleTitleTextView;
                public TextView articleTeaserTextView;
                public ImageView articleImageView;

                public SearchArticleViewHolder(View itemView) {
                    super(itemView);
                    articleTitleTextView = (TextView) itemView.findViewById(R.id.search_article_title);
                    articleTeaserTextView = (TextView) itemView.findViewById(R.id.search_article_teaser);
                    articleImageView = (ImageView) itemView.findViewById(R.id.search_article_image);
                    itemView.setOnClickListener(this);
                }

                @Override
                public void onClick(View v) {
//                    Toast.makeText(MainActivity.applicationContext, "View clicked at position: " + getPosition(), Toast.LENGTH_SHORT).show();
                    Intent articleIntent = new Intent(MainActivity.applicationContext, ArticleActivity.class);
                    // Pass issue through as a Parcel
                    if (listElements.get(getPosition()) instanceof Article) {
                        Article articleTapped = (Article) listElements.get(getPosition());
                        Issue articleTappedIssue = new Issue(articleTapped.getIssueID());
                        articleIntent.putExtra("article", articleTapped);
                        articleIntent.putExtra("issue", articleTappedIssue);
                    }
                    startActivity(articleIntent);
                }
            }

            public class SearchHeaderViewHolder extends RecyclerView.ViewHolder {

                public ImageView issueCoverImageView;
                public TextView searchResultsHeader;

                public SearchHeaderViewHolder(View itemView) {
                    super(itemView);
                    issueCoverImageView = (ImageView) itemView.findViewById(R.id.search_results_cover);
                    searchResultsHeader = (TextView) itemView.findViewById(R.id.search_results_header);
                }
            }
        }
    }

    // Search logic

    static public void filterArticlesForSearchQuery(String query) {
//        Log.i("Search", "Search for " + query);

        // This is an AND search by default.

        // Split the search terms so they can be searched without being in sequence
        ArrayList<String> quotedTerms = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(query);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                quotedTerms.add(Pattern.quote(regexMatcher.group(1)));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                quotedTerms.add(Pattern.quote(regexMatcher.group(2)));
            } else {
                // Add unquoted word
                quotedTerms.add(Pattern.quote(regexMatcher.group()));
            }
        }

        filteredIssueArticlesArray = new ArrayList<>();

        for (Issue issue : issuesArray) {
            filteredIssueArticlesArray.add(issue.getArticles());
        }

        for (String term : quotedTerms) {
            filteredIssueArticlesArray = filterArticleList(term, filteredIssueArticlesArray);
        }
    }

    static ArrayList<ArrayList<Article>> filterArticleList(String searchTerm, ArrayList<ArrayList<Article>> arrayOfArticleArrays) {

        ArrayList<ArrayList<Article>> filteredArrayOfArticleArrays = new ArrayList<>();

        // Create a pattern to match
        Pattern pattern = Pattern.compile(searchTerm, Pattern.CASE_INSENSITIVE);
//        Log.i("Search", pattern.toString());

        for (ArrayList<Article> articlesArray : arrayOfArticleArrays) {

            ArrayList <Article> filteredArticles = new ArrayList<Article>();

            for (Article article : articlesArray) {
                // Search title
                Matcher titleMatcher = pattern.matcher(article.getTitle());
                if (titleMatcher.find()) {
                    filteredArticles.add(article);
                } else {
                    // Search teaser
                    Matcher teaserMatcher = pattern.matcher(article.getTeaser());
                    if (teaserMatcher.find()) {
                        filteredArticles.add(article);
                    } else {
                        // Search the body if it exists on file
                        if (article.isBodyOnFilesystem()) {
                            Matcher bodyMatcher = pattern.matcher(article.getExpandedBody(null));
                            if (bodyMatcher.find()) {
                                filteredArticles.add(article);
                            }
                        }
                    }
                }
            }

            if (filteredArticles.size() > 0) {
                filteredArrayOfArticleArrays.add(filteredArticles);
            }
        }

        return filteredArrayOfArticleArrays;
    }
}
