package au.com.newint.newinternationalist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
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
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.google.gson.JsonArray;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;


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

        // Set title to Home screen
        setTitle("Home");

        // Get SITE_URL
        String siteURLString = (String) Helpers.getSiteURL();

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

        //articlesJSONCache.addMethod(new MemoryByteCacheMethod());
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
            case R.id.menu_item_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                // TODO: Send issue share information here...
                sendIntent.putExtra(Intent.EXTRA_TEXT, "TODO: This is my text to send.");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.action_share_toc)));
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

        int editorsImageWidth = 400;
        int editorsImageHeight = 400;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_table_of_contents, container, false);

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
                issueFromActivity = new Issue(Publisher.INSTANCE.latestIssue().getID());
            }

            final TableOfContentsAdapter adapter = new TableOfContentsAdapter(issueFromActivity);
            recList.setAdapter(adapter);

            // Register for DownloadComplete listener
            Publisher.ArticlesDownloadCompleteListener listener = new Publisher.ArticlesDownloadCompleteListener() {

                @Override
                public void onArticlesDownloadComplete(JsonArray articles) {
                    Log.i("ArticlesReady", "Received listener, refreshing articles view.");
                    // Refresh adapter data
                    adapter.notifyDataSetChanged();
                    Publisher.articleListeners.clear();
                }
            };
            Publisher.INSTANCE.setOnArticlesDownloadCompleteListener(listener);

            // Register for editors photo complete listener.
            // Register for DownloadComplete listener
            Publisher.UpdateListener editorImageListener = new Publisher.UpdateListener() {
                @Override
                public void onUpdate(Object object) {

                    // Tell the adapter to update the footer view so it loads the editor image
                    adapter.notifyItemChanged(adapter.getItemCount() - 1);
                }
            };
            Publisher.INSTANCE.setOnDownloadCompleteListener(editorImageListener);

            return rootView;
        }

        // Adapter for CardView
        public class TableOfContentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

            public Issue issue;
            private static final int TYPE_HEADER = 0;
            private static final int TYPE_ARTICLE = 1;
            private static final int TYPE_FOOTER = 2;

            public TableOfContentsAdapter(Issue issue) {
                this.issue = issue;
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = null;
                if (viewType == 0) {
                    // Header
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_table_of_contents_header_view, parent, false);
                    return new TableOfContentsHeaderViewHolder(itemView);

                } else if (viewType == 1) {
                    // Article
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_table_of_contents_card_view, parent, false);
                    return new TableOfContentsViewHolder(itemView);

                } else if (viewType == 2) {
                    // Footer
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_table_of_contents_footer_view, parent, false);
                    return new TableOfContentsFooterViewHolder(itemView);
                } else {
                    // Uh oh... didn't match view type.
                    return null;
                }
            }

            @Override
            public int getItemCount() {
                return issue.getArticles().size() + 2; // 2 = header + footer
            }

            @Override
            public int getItemViewType(int position) {
                if (isPositionHeader(position)) {
                    return TYPE_HEADER;
                } else if (isPositionFooter(position)) {
                    return TYPE_FOOTER;
                }
                return TYPE_ARTICLE;
            }

            private boolean isPositionHeader(int position) {
                return position == 0;
            }

            private boolean isPositionFooter(int position) {
                return position == issue.getArticles().size() + 1;
            }

            private Article getArticle(int position) {
                return issue.getArticles().get(position - 1); // Header
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                if (holder instanceof TableOfContentsHeaderViewHolder) {
                    // Header
                    DateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy");
                    String issueNumberDate = Integer.toString(issue.getNumber()) + " - " + dateFormat.format(issue.getRelease());
                    ((TableOfContentsHeaderViewHolder) holder).issueNumberDateTextView.setText(issueNumberDate);

                    final ImageView coverImageView = ((TableOfContentsHeaderViewHolder) holder).issueCoverImageView;
                    if (coverImageView.getLayoutParams().width < 1) {

                        int coverWidth = 400;
                        int coverHeight = 576;
                        final File coverFile = issue.getCoverForSize(coverWidth, coverHeight);

                        // Expand the image to the right size
                        ViewGroup.LayoutParams params = coverImageView.getLayoutParams();
                        params.width = coverWidth;
                        params.height = coverHeight;
                        coverImageView.setLayoutParams(params);

                        // Set default loading cover...
                        Bitmap defaultCoverBitmap = BitmapFactory.decodeResource(MainActivity.applicationContext.getResources(), R.drawable.home_cover);
                        coverImageView.setImageBitmap(defaultCoverBitmap);
                        issue.getCoverCacheStreamFactoryForSize(coverWidth).preload(new CacheStreamFactory.CachePreloadCallback() {
                            @Override
                            public void onLoad(CacheStreamFactory streamCache) {
                                Bitmap coverBitmap = BitmapFactory.decodeStream(streamCache.createInputStream());
                                coverImageView.setImageBitmap(coverBitmap);

                            }
                        });

                    }

                } else if (holder instanceof TableOfContentsViewHolder) {
                    // Article
                    Article article = getArticle(position);
                    ((TableOfContentsViewHolder) holder).articleTitleTextView.setText(article.getTitle());
                    ((TableOfContentsViewHolder) holder).articleTeaserTextView.setText(Html.fromHtml(article.getTeaser()));

                    String categoriesTemporaryString = "";
                    String separator = "";
                    ArrayList<HashMap<String,Object>> categories = article.getCategories();
                    for (HashMap<String,Object> category : categories) {
                        categoriesTemporaryString += separator;
                        categoriesTemporaryString += category.get("name");
                        separator = "\n";
                    }

                    ((TableOfContentsViewHolder) holder).articleCategoriesTextView.setText(categoriesTemporaryString);

                } else if (holder instanceof TableOfContentsFooterViewHolder) {
                    // Footer
                    // Get editor image.
                    ImageView editorImageView = ((TableOfContentsFooterViewHolder) holder).editorImageView;
                    if (editorImageView != null) {

                        File imageFile = issue.getEditorsImageForSize(editorsImageWidth, editorsImageHeight);

                        // Expand the imageView to the right size
                        ViewGroup.LayoutParams params = editorImageView.getLayoutParams();
                        params.width = editorsImageWidth;
                        params.height = editorsImageHeight;
                        editorImageView.setLayoutParams(params);

                        if (imageFile != null && imageFile.exists()) {
                            Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getPath());
                            editorImageView.setImageDrawable(Helpers.roundDrawableFromBitmap(imageBitmap));
                        } else {
                            // Set default loading image...
                            Bitmap defaultImageBitmap = BitmapFactory.decodeResource(MainActivity.applicationContext.getResources(), R.drawable.editors_photo);
                            editorImageView.setImageDrawable(Helpers.roundDrawableFromBitmap(defaultImageBitmap));
                        }
                    }

                    ((TableOfContentsFooterViewHolder) holder).editorsLetterTextView.setText(Html.fromHtml(issue.getEditorsLetterHtml()));
                    ((TableOfContentsFooterViewHolder) holder).editorsNameTextView.setText("Edited by:\n" + issue.getEditorsName());
                }
            }


            public class TableOfContentsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView articleTitleTextView;
                public TextView articleTeaserTextView;
                public TextView articleCategoriesTextView;

                public TableOfContentsViewHolder(View itemView) {
                    super(itemView);
                    articleTitleTextView = (TextView) itemView.findViewById(R.id.toc_article_title);
                    articleTeaserTextView = (TextView) itemView.findViewById(R.id.toc_article_teaser);
                    articleCategoriesTextView = (TextView) itemView.findViewById(R.id.toc_article_categories);
                    itemView.setOnClickListener(this);
                }

                @Override
                public void onClick(View v) {
//                    Toast.makeText(MainActivity.applicationContext, "View clicked at position: " + getPosition(), Toast.LENGTH_SHORT).show();
                    Intent articleIntent = new Intent(MainActivity.applicationContext, ArticleActivity.class);
                    // Pass issue through as a Parcel
                    articleIntent.putExtra("article", issue.articles.get(getPosition() - 1));
                    articleIntent.putExtra("issue", issue);
                    startActivity(articleIntent);
                }
            }

            public class TableOfContentsHeaderViewHolder extends RecyclerView.ViewHolder {

                public ImageView issueCoverImageView;
                public TextView issueNumberDateTextView;

                public TableOfContentsHeaderViewHolder(View itemView) {
                    super(itemView);
                    issueCoverImageView = (ImageView) itemView.findViewById(R.id.toc_cover);
                    issueNumberDateTextView = (TextView) itemView.findViewById(R.id.toc_issue_number_date);
                }
            }

            public class TableOfContentsFooterViewHolder extends RecyclerView.ViewHolder {

                public ImageView editorImageView;
                public TextView editorsLetterTextView;
                public TextView editorsNameTextView;

                public TableOfContentsFooterViewHolder(View itemView) {
                    super(itemView);
                    editorImageView = (ImageView) itemView.findViewById(R.id.toc_editor_image);
                    editorsLetterTextView = (TextView) itemView.findViewById(R.id.toc_editors_letter);
                    editorsNameTextView = (TextView) itemView.findViewById(R.id.toc_editors_name);
                }
            }
        }
    }
}
