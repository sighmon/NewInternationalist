package au.com.newint.newinternationalist;

import android.content.Intent;
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
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;


public class CategoryActivity extends ActionBarActivity {

    static Category category;

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

        setTitle(category.getDisplayName());
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

                // Article
                Article article = category.getArticles().get(position);
                // TODO: This calls getArticles() that hits the filesystem Pix to fix.
                // Do we want to do this though, as we don't want to download
                // every article for every issue.. or do we???
                Issue issue = new Issue(article.issueID);
                CategoryViewHolder categoryViewHolder = ((CategoryViewHolder) holder);
                categoryViewHolder.articleTitleTextView.setText(article.getTitle());
                String articleTeaser = article.getTeaser();
                if (articleTeaser != null && !articleTeaser.isEmpty()) {
                    categoryViewHolder.articleTeaserTextView.setVisibility(View.VISIBLE);
                    categoryViewHolder.articleTeaserTextView.setText(Html.fromHtml(articleTeaser));
                } else {
                    // Remove teaser view.
                    categoryViewHolder.articleTeaserTextView.setVisibility(View.GONE);
                }

                DateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
                String issueInformation = Integer.toString(issue.getNumber()) + " - " + dateFormat.format(issue.getRelease());
                categoryViewHolder.articleIssueInformationTextView.setText(issueInformation);

                String categoriesTemporaryString = "";
                String separator = "";
                ArrayList<Category> categories = article.getCategories();
                for (Category category : categories) {
                    categoriesTemporaryString += separator;
                    categoriesTemporaryString += category.getName();
                    separator = "\n";
                }

                ((CategoryViewHolder) holder).articleCategoriesTextView.setText(categoriesTemporaryString);
            }


            public class CategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView articleTitleTextView;
                public TextView articleTeaserTextView;
                public TextView articleIssueInformationTextView;
                public TextView articleCategoriesTextView;

                public CategoryViewHolder(View itemView) {
                    super(itemView);
                    articleTitleTextView = (TextView) itemView.findViewById(R.id.toc_article_title);
                    articleTeaserTextView = (TextView) itemView.findViewById(R.id.toc_article_teaser);
                    articleIssueInformationTextView = (TextView) itemView.findViewById(R.id.category_article_issue_information);
                    articleCategoriesTextView = (TextView) itemView.findViewById(R.id.toc_article_categories);
                    itemView.setOnClickListener(this);
                }

                @Override
                public void onClick(View v) {
//                    Toast.makeText(MainActivity.applicationContext, "View clicked at position: " + getPosition(), Toast.LENGTH_SHORT).show();
                    Intent articleIntent = new Intent(MainActivity.applicationContext, ArticleActivity.class);
                    Article article = category.getArticles().get(getPosition());
                    Issue issue = new Issue(article.getIssueID());
                    // Pass issue through as a Parcel
                    articleIntent.putExtra("article", article);
                    articleIntent.putExtra("issue", issue);
                    startActivity(articleIntent);
                }
            }
        }
    }
}
