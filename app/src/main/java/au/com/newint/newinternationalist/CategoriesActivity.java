package au.com.newint.newinternationalist;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


public class CategoriesActivity extends AppCompatActivity {

    static ProgressDialog loadingProgressDialog;
    static public ArrayList<CategoriesFragment.Section> sectionsList;
    static public ArrayList<Issue> issuesList;
    static RecyclerView recList;
    static TextView emptyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new CategoriesFragment())
                    .commit();
        }

        // Search intent
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Helpers.debugLog("Search","Searching for: " + query);
        }

        loadingProgressDialog = new ProgressDialog(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Send Google Analytics if the user allows it
        Helpers.sendGoogleAnalytics(getResources().getString(R.string.title_activity_categories));
    }

    @Override
    public void onStop() {
        super.onStop();

        //
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_categories, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
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
            case R.id.action_search:
                Helpers.debugLog("Search", "Search tapped on Home view.");
//                onSearchRequested();
            case R.id.categories_refresh:
                Helpers.debugLog("Categories", "Refresh requested.");
                refreshArticles();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshArticles() {
        final HashMap<Integer,Boolean> preloadedIssueArticles = new HashMap<Integer, Boolean>();

        for (final Issue issue : issuesList) {

            loadingProgressDialog.setTitle(getResources().getString(R.string.categories_preload_articles_loading_progress_title));
            loadingProgressDialog.setMessage(getResources().getString(R.string.categories_preload_articles_loading_progress_message));
            loadingProgressDialog.show();

            // Preload articles
            preloadedIssueArticles.put(issue.getID(),false);
            issue.preloadArticles(new CacheStreamFactory.CachePreloadCallback() {

                @Override
                public void onLoad(byte[] payload) {
                    preloadedIssueArticles.put(issue.getID(), true);
                    Boolean finished = true;
                    for (Map.Entry<Integer,Boolean> entry : preloadedIssueArticles.entrySet()) {
                        if (!entry.getValue()) {
                            finished = false;
                        }
                    }
                    if (finished) {
                        loadingProgressDialog.dismiss();
                        CategoriesFragment.adapter.loadCategories();
                    }
                }

                @Override
                public void onLoadBackground(byte[] payload) {

                }
            });
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class CategoriesFragment extends Fragment {

        static public CategoriesAdapter adapter;

        public CategoriesFragment() {
        }

        public class Section {
            public String name;
            public ArrayList<Category> categories;

            public Section(String sectionName) {
                this.name = sectionName;
                this.categories = new ArrayList<Category>();
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            // Set a light theme
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.ArticleTheme);

            // Clone the inflater using the ContextThemeWrapper to apply the theme
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
            View rootView = localInflater.inflate(R.layout.fragment_categories, container, false);

            // RecyclerView setup
            recList = (RecyclerView) rootView.findViewById(R.id.categories_recycler_view);
            emptyList = (TextView) rootView.findViewById(R.id.empty_view);

            // TODO: Work out how to change the colour to not-white
            recList.addItemDecoration(new DividerItemDecoration(getActivity(), null));
            recList.setHasFixedSize(false);
            LinearLayoutManager llm = new LinearLayoutManager(rootView.getContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            adapter = new CategoriesAdapter();
            recList.setAdapter(adapter);

            return rootView;
        }

        // Adapter for Categories RecyclerView
        public class CategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

            private static final int TYPE_HEADER = 0;
            private static final int TYPE_CATEGORY = 1;

            public CategoriesAdapter() {

                // Load new articles unless the user just wants to search cached articles
                issuesList = Publisher.INSTANCE.getIssuesFromFilesystem();
                loadCategories();
            }

            public void loadCategories() {
                loadingProgressDialog.setTitle(getResources().getString(R.string.categories_loading_progress_title));
                loadingProgressDialog.setMessage(getResources().getString(R.string.categories_loading_progress_message));
                loadingProgressDialog.show();
                new LoadCategoriesTask().execute();
            }

            public class LoadCategoriesTask extends AsyncTask<Void, Void, Void> {

                protected Void doInBackground(Void... unused) {
                    // Load Categories from file system in the background

                    sectionsList = new ArrayList<>();
                    ArrayList<Category> unfilteredCategoriesList = new ArrayList<>();
                    // Add unsorted categories to this list
                    issuesList = Publisher.INSTANCE.getIssuesFromFilesystem();
                    for (Issue issue : issuesList) {
                        ArrayList<Article> articlesList = issue.getArticles();
                        for (Article article : articlesList) {
                            ArrayList<Category> articleCategories = article.getCategories();
                            for (Category category : articleCategories) {
                                unfilteredCategoriesList.add(category);
                            }
                        }
                    }

                    // Sort by name
                    Collections.sort(unfilteredCategoriesList, new Comparator<Category>() {
                        @Override
                        public int compare(Category lhs, Category rhs) {
                            return lhs.getName().compareTo(rhs.getName());
                        }
                    });

                    // Add unique elements to categoriesList
                    Category lastCategory = null;
                    Section currentSection = null;
                    for (Category category : unfilteredCategoriesList) {
                        if (lastCategory == null || !lastCategory.equals(category)) {
                            // We have a new unique category
                            String sectionName = category.getSectionName();
                            if (currentSection == null || !currentSection.name.equals(sectionName)) {
                                currentSection = new Section(sectionName);
                                sectionsList.add(currentSection);
                            }
                            currentSection.categories.add(category);
                        }
                        lastCategory = category;
                    }

                    return null;
                }
                protected void onPostExecute(Void unused) {
                    // After loading finishes dismiss the loading dialog
                    loadingProgressDialog.dismiss();

                    if (sectionsList != null && sectionsList.size() > 0) {
                        recList.setVisibility(View.VISIBLE);
                        emptyList.setVisibility(View.GONE);
                    } else {
                        recList.setVisibility(View.GONE);
                        emptyList.setVisibility(View.VISIBLE);
                    }
                    notifyDataSetChanged();
                }
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = null;
                if (viewType == 0) {
                    // Header
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_categories_header, parent, false);
                    return new CategoryHeaderViewHolder(itemView);

                } else if (viewType == 1) {
                    // Article
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_categories_list_view, parent, false);
                    return new CategoryViewHolder(itemView);

                } else {
                    // Uh oh... didn't match view type.
                    return null;
                }
            }

            @Override
            public int getItemCount() {

                int itemCount = 0;
                if (sectionsList != null && sectionsList.size() > 0) {
                    for (Section section : sectionsList) {
                        itemCount++;
                        itemCount += section.categories.size();
                    }
                }
                return itemCount;
            }

            @Override
            public int getItemViewType(int position) {
                if (isPositionAHeader(position)) {
                    return TYPE_HEADER;
                }
                return TYPE_CATEGORY;
            }

            private boolean isPositionAHeader(int position) {

                return getSectionOrCategoryForPosition(position) instanceof Section;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                if (holder instanceof CategoryHeaderViewHolder) {
                    // Section
                    String sectionName = ((Section) getSectionOrCategoryForPosition(position)).name;
                    ((CategoryHeaderViewHolder) holder).categoryHeader.setText(sectionName);

                } else if (holder instanceof CategoryViewHolder) {
                    // Category
                    Category category = ((Category) getSectionOrCategoryForPosition(position));
                    if (category != null) {
                        String categoryName = category.getDisplayName();
                        ((CategoryViewHolder) holder).categoryTitleTextView.setText(categoryName);
                    }
                }
            }

            public Object getSectionOrCategoryForPosition(int position) {
                int index = 0;
                for (Section section : sectionsList) {
                    if (index == position) {
                        return section;
                    }
                    index++;
                    for (Category sectionCategory : section.categories) {
                        if (index == position) {
                            return sectionCategory;
                        }
                        index++;
                    }
                }
                return null;
            }

            public class CategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView categoryTitleTextView;

                public CategoryViewHolder(final View itemView) {
                    super(itemView);
                    categoryTitleTextView = (TextView) itemView.findViewById(R.id.category_name);
                    itemView.setOnClickListener(this);

                    itemView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    // Set 50% black overlay
                                    itemView.setBackgroundColor(Color.argb(125, 0, 0, 0));
                                    return false;
                                case MotionEvent.ACTION_UP:
                                    // Remove overlay
                                    itemView.setBackgroundColor(Color.argb(0, 0, 0, 0));
                                    return false;
                                case MotionEvent.ACTION_CANCEL:
                                    // Remove overlay
                                    itemView.setBackgroundColor(Color.argb(0, 0, 0, 0));
                                    return false;
                                default:
                                    // Do nothing
                                    return false;
                            }
                        }
                    });
                }

                @Override
                public void onClick(View v) {
                    Intent categoryIntent = new Intent(MainActivity.applicationContext, CategoryActivity.class);
//                    // Pass Category through as a Parcel
                    if (getSectionOrCategoryForPosition(getPosition()) instanceof Category) {
                        Category categoryTapped = (Category) getSectionOrCategoryForPosition(getPosition());
                        Helpers.debugLog("Categories", "Category tapped: " + categoryTapped.getDisplayName());
                        categoryIntent.putExtra("categoryJson", categoryTapped.categoryJson.toString());
                    }
                    startActivity(categoryIntent);
                }
            }

            public class CategoryHeaderViewHolder extends RecyclerView.ViewHolder {

                public TextView categoryHeader;

                public CategoryHeaderViewHolder(View itemView) {
                    super(itemView);
                    categoryHeader = (TextView) itemView.findViewById(R.id.category_header);
                }
            }
        }
    }
}
