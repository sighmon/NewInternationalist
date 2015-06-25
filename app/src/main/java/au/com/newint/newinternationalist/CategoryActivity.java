package au.com.newint.newinternationalist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;


public class CategoryActivity extends ActionBarActivity {

    static Category category;
    static ArrayList<Article> articles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new CategoryFragment())
                    .commit();
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject)parser.parse(getIntent().getStringExtra("categoryJson"));

        category = new Category(jsonObject);
        articles = category.getArticles();

        setTitle(category.getDisplayName());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Send Google Analytics if the user allows it
        Helpers.sendGoogleAnalytics(category.getDisplayName() + " (" + getResources().getString(R.string.title_activity_category) + ")");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_category, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // int id = item.getItemId();

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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class CategoryFragment extends Fragment {

        public CategoryFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_category, container, false);

            // RecyclerView setup
            final RecyclerView recList = (RecyclerView) rootView.findViewById(R.id.category_card_list);
            recList.setHasFixedSize(false);
            LinearLayoutManager llm = new LinearLayoutManager(rootView.getContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            final CategoryAdapter adapter = new CategoryAdapter();
            recList.setAdapter(adapter);

            return rootView;
        }


        // Adapter for CardView
        public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

            public CategoryAdapter() {
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = null;
                // Article
                itemView = LayoutInflater.
                        from(parent.getContext()).
                        inflate(R.layout.fragment_category_card_view, parent, false);
                return new CategoryViewHolder(itemView);
            }

            @Override
            public int getItemCount() {
                return category.getArticles().size();
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                //holder.setIsRecyclable(false);

                // Article
                Article article = articles.get(position);
                Issue issue = article.parentIssue;
                final CategoryViewHolder categoryViewHolder = ((CategoryViewHolder) holder);
                categoryViewHolder.setCacheStreamFactory(null);
                categoryViewHolder.articleTitleTextView.setText(article.getTitle());
                String articleTeaser = article.getTeaser();
                if (articleTeaser != null && !articleTeaser.isEmpty()) {
                    categoryViewHolder.articleTeaserTextView.setVisibility(View.VISIBLE);
                    categoryViewHolder.articleTeaserTextView.setText(Html.fromHtml(articleTeaser));
                } else {
                    // Remove teaser view.
                    categoryViewHolder.articleTeaserTextView.setVisibility(View.GONE);
                }

                ArrayList<Image> images = article.getImages();
                final ImageView articleImageView = categoryViewHolder.articleImageView;
                if (images.size() > 0) {

                    articleImageView.setVisibility(View.VISIBLE);

                    final CacheStreamFactory cacheStreamFactory = images.get(0).getImageCacheStreamFactoryForSize(MainActivity.applicationContext.getResources().getDisplayMetrics().widthPixels);

                    categoryViewHolder.setCacheStreamFactory(cacheStreamFactory);

                    cacheStreamFactory.preload(new CacheStreamFactory.CachePreloadCallback() {
                        @Override
                        public void onLoad(byte[] payload) {
                            if (payload != null && payload.length > 0) {


                                if (categoryViewHolder.hasCacheStreamFactory(cacheStreamFactory))
                                    Log.i("CategoryActivity", "onBindViewHolder->preload->onLoad: expected callback");
                                else {
                                    Log.i("CategoryActivity", "onBindViewHolder->preload->onLoad: not expecting this callback");
                                    return;
                                }

                                Bitmap coverBitmap = Helpers.bitmapDecode(payload);
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

                DateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
                String issueInformation = Integer.toString(issue.getNumber()) + " - " + dateFormat.format(issue.getRelease());
                categoryViewHolder.articleIssueInformationTextView.setText(issueInformation);
            }


            public class CategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView articleTitleTextView;
                public TextView articleTeaserTextView;
                public ImageView articleImageView;
                public TextView articleIssueInformationTextView;

                public CacheStreamFactory cacheStreamFactory;

                public CategoryViewHolder(View itemView) {
                    super(itemView);
                    articleTitleTextView = (TextView) itemView.findViewById(R.id.category_article_title);
                    articleTeaserTextView = (TextView) itemView.findViewById(R.id.category_article_teaser);
                    articleImageView = (ImageView) itemView.findViewById(R.id.category_article_image);
                    articleIssueInformationTextView = (TextView) itemView.findViewById(R.id.category_article_issue_information);
                    itemView.setOnClickListener(this);
                }

                public void setCacheStreamFactory(CacheStreamFactory cacheStreamFactory) {
                    // find last cacheStreamFactory and kill it's process
                    if(this.cacheStreamFactory!=null) {
                        this.cacheStreamFactory.preloadTask.cancel(false);
                    }
                    this.cacheStreamFactory = cacheStreamFactory;
                }

                public boolean hasCacheStreamFactory(CacheStreamFactory cacheStreamFactory) {
                    return this.cacheStreamFactory==cacheStreamFactory;
                }



                @Override
                public void onClick(View v) {
//                    Toast.makeText(MainActivity.applicationContext, "View clicked at position: " + getPosition(), Toast.LENGTH_SHORT).show();
                    Intent articleIntent = new Intent(MainActivity.applicationContext, ArticleActivity.class);
                    Article article = articles.get(getPosition());
                    Issue issue = article.parentIssue;
                    // Pass issue through as a Parcel
                    articleIntent.putExtra("article", article);
                    articleIntent.putExtra("issue", issue);
                    startActivity(articleIntent);
                }
            }
        }
    }
}
