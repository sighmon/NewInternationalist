package au.com.newint.newinternationalist;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SearchActivity extends ActionBarActivity {

    static String searchQuery;
    static ArrayList<Issue> issuesArray;
    static ArrayList<ArrayList<Article>> filteredIssueArticlesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SearchFragment())
                    .commit();
        }

        // Build up the issuesArray
        issuesArray = Publisher.INSTANCE.getIssuesFromFilesystem();

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);

            // TODO: Perform the search
            filterArticlesForSearchQuery(searchQuery);
        }
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

            // TODO: Setup a list view
            TextView searchTermTextView = (TextView) rootView.findViewById(R.id.search_term_temporary);
            ArrayList<String> results = new ArrayList<String>();
            int matches = 0;
            if (filteredIssueArticlesArray != null && filteredIssueArticlesArray.size() > 0) {
                for (ArrayList<Article> articleList : filteredIssueArticlesArray) {
                    for (Article article : articleList) {
                        matches++;
                        results.add(article.getTitle());
                    }
                }
            }
            searchTermTextView.setText(Integer.toString(matches) + ": " + TextUtils.join(", ",results));

            return rootView;
        }
    }

    public void filterArticlesForSearchQuery(String query) {
        Log.i("Search", "Search for " + query);

        // TODO: This is an OR search not an AND search.. TOFIX?

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

        for (String term : query.split("\\s+")) {
            quotedTerms.add(Pattern.quote(term));
        }
        String searchString = "(" + TextUtils.join("|", quotedTerms) + ")";

        filteredIssueArticlesArray = new ArrayList<>();
        // Create a pattern to match
        Pattern pattern = Pattern.compile(searchString, Pattern.CASE_INSENSITIVE);
        Log.i("Search", pattern.toString());

        for (Issue issue : issuesArray) {
            ArrayList <Article> articlesArray = issue.getArticles();
            ArrayList <Article> filteredArticlesArray = new ArrayList<Article>();

            for (Article article : articlesArray) {
                // Search title
                Matcher titleMatcher = pattern.matcher(article.getTitle());
                if (titleMatcher.find()) {
                    filteredArticlesArray.add(article);
                } else {
                    // Search teaser
                    Matcher teaserMatcher = pattern.matcher(article.getTeaser());
                    if (teaserMatcher.find()) {
                        filteredArticlesArray.add(article);
                    } else {
                        // Search the body if it exists on file
                        if (article.isBodyOnFilesystem()) {
                            Matcher bodyMatcher = pattern.matcher(article.getBody());
                            if (bodyMatcher.find()) {
                                filteredArticlesArray.add(article);
                            }
                        }
                    }
                }
            }

            if (filteredArticlesArray.size() > 0) {
                filteredIssueArticlesArray.add(filteredArticlesArray);
            }
        }
        Log.i("Search", "Filtered issue articles array: " + filteredIssueArticlesArray);
    }
}
