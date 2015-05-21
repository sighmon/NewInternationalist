package au.com.newint.newinternationalist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.JsonArray;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;


public class TableOfContentsActivity extends ActionBarActivity {

    Issue issue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_of_contents);
        final Fragment tableOfContentsFragment = new TableOfContentsFragment();

        issue = getIntent().getParcelableExtra("issue");

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
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                // Send issue share information here...
                DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                String magazineInformation = issue.getTitle()
                        + " - New Internationalist magazine "
                        + dateFormat.format(issue.getRelease());
                shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm reading "
                                + magazineInformation
                                + ".\n\n"
                                + issue.getWebURL()
                );
                shareIntent.setType("text/plain");
                // TODO: When time permits, save the image to externalStorage and then share.
//                shareIntent.putExtra(Intent.EXTRA_STREAM, issue.getCoverUriOnFilesystem());
//                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "New Internationalist magazine, " + dateFormat.format(issue.getRelease()));
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share_toc)));
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

            issueFromActivity.preloadArticles(new CacheStreamFactory.CachePreloadCallback() {
                @Override
                public void onLoad(byte[] payload) {
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onLoadBackground(byte[] payload) {

                }
            });

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

                // Not recycling so that images don't appear in the wrong place
                holder.setIsRecyclable(false);

                if (holder instanceof TableOfContentsHeaderViewHolder) {
                    // Header
                    DateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
                    String issueNumberDate = Integer.toString(issue.getNumber()) + " - " + dateFormat.format(issue.getRelease());
                    ((TableOfContentsHeaderViewHolder) holder).issueNumberDateTextView.setText(issueNumberDate);

                    final ImageView coverImageView = ((TableOfContentsHeaderViewHolder) holder).issueCoverImageView;
                    issue.getCoverCacheStreamFactoryForSize((int) getResources().getDimension(R.dimen.toc_cover_width)).preload(new CacheStreamFactory.CachePreloadCallback() {
                        @Override
                        public void onLoad(byte[] payload) {
                            if (payload != null && payload.length > 0) {
                                Bitmap coverBitmap = BitmapFactory.decodeByteArray(payload,0,payload.length);
                                coverImageView.setImageBitmap(coverBitmap);

                            }
                        }

                        @Override
                        public void onLoadBackground(byte[] payload) {

                        }
                    });

                } else if (holder instanceof TableOfContentsViewHolder) {
                    // Article
                    Article article = getArticle(position);
                    ArrayList<Image> images = article.getImages();
                            ((TableOfContentsViewHolder) holder).articleTitleTextView.setText(article.getTitle());
                    String articleTeaser = article.getTeaser();
                    TableOfContentsViewHolder tableOfContentsViewHolder = ((TableOfContentsViewHolder) holder);
                    if (articleTeaser != null && !articleTeaser.isEmpty()) {
                        tableOfContentsViewHolder.articleTeaserTextView.setVisibility(View.VISIBLE);
                        tableOfContentsViewHolder.articleTeaserTextView.setText(Html.fromHtml(articleTeaser));
                    } else {
                        // Remove teaser view.
                        tableOfContentsViewHolder.articleTeaserTextView.setVisibility(View.GONE);
                    }

                    final ImageView articleImageView = ((TableOfContentsViewHolder) holder).articleImageView;
                    if (images.size() > 0) {
                        images.get(0).getImageCacheStreamFactoryForSize(MainActivity.applicationContext.getResources().getDisplayMetrics().widthPixels).preload(new CacheStreamFactory.CachePreloadCallback() {
                            @Override
                            public void onLoad(byte[] payload) {
                                if (payload != null && payload.length > 0) {
                                    Bitmap coverBitmap = BitmapFactory.decodeByteArray(payload,0,payload.length);
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

                } else if (holder instanceof TableOfContentsFooterViewHolder) {
                    // Footer
                    // Get editor image.
                    final ImageView editorImageView = ((TableOfContentsFooterViewHolder) holder).editorImageView;
                    if (editorImageView != null) {

                        issue.getEditorsImageCacheStreamFactoryForSize((int) getResources().getDimension(R.dimen.toc_editors_image_width), (int) getResources().getDimension(R.dimen.toc_editors_image_height)).preload(new CacheStreamFactory.CachePreloadCallback() {
                            @Override
                            public void onLoad(byte[] payload) {
                                if (payload != null && payload.length > 0) {
                                    Bitmap editorsImageBitmap = BitmapFactory.decodeByteArray(payload, 0, payload.length);
                                    editorImageView.setImageDrawable(Helpers.roundDrawableFromBitmap(editorsImageBitmap));
                                    TableOfContentsAdapter.this.notifyItemChanged(TableOfContentsAdapter.this.getItemCount()-1);
                                }
                            }

                            @Override
                            public void onLoadBackground(byte[] payload) {

                            }
                        });
                    }

                    ((TableOfContentsFooterViewHolder) holder).editorsLetterTextView.setText(Html.fromHtml(issue.getEditorsLetterHtml()));
                    ((TableOfContentsFooterViewHolder) holder).editorsNameTextView.setText("Edited by:\n" + issue.getEditorsName());
                }
            }


            public class TableOfContentsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView articleTitleTextView;
                public TextView articleTeaserTextView;
                public ImageView articleImageView;

                public TableOfContentsViewHolder(View itemView) {
                    super(itemView);
                    articleTitleTextView = (TextView) itemView.findViewById(R.id.toc_article_title);
                    articleTeaserTextView = (TextView) itemView.findViewById(R.id.toc_article_teaser);
                    articleImageView = (ImageView) itemView.findViewById(R.id.toc_article_image);
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
