package au.com.newint.newinternationalist;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.analytics.ecommerce.Product;

import java.util.ArrayList;
import java.util.Date;

import au.com.newint.newinternationalist.util.IabHelper;
import au.com.newint.newinternationalist.util.IabResult;
import au.com.newint.newinternationalist.util.Inventory;
import au.com.newint.newinternationalist.util.Purchase;
import au.com.newint.newinternationalist.util.SkuDetails;


public class SubscribeActivity extends ActionBarActivity {

    static IabHelper mHelper;
    static ArrayList<SkuDetails> mProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SubscribeActivityFragment())
                    .commit();
        }

        mProducts = new ArrayList<>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_subscribe, menu);
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
     * Subscribe fragment
     */
    public static class SubscribeActivityFragment extends Fragment {

        public SubscribeActivityFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            final View rootView = inflater.inflate(R.layout.fragment_subscribe, container, false);

            // Setup view

            final RecyclerView recList = (RecyclerView) rootView.findViewById(R.id.subscribe_card_list);
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(rootView.getContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);

            final SubscribeAdapter adapter = new SubscribeAdapter();
            recList.setAdapter(adapter);

            // Setup in-app billing
            mHelper = Helpers.setupIabHelper(getActivity().getApplicationContext());

            // Get inventory from Google Play
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem.
                        Log.d("Subscribe", "Problem setting up In-app Billing: " + result);
                    }
                    // Hooray, IAB is fully set up!
                    Log.i("Subscribe", "In-app billing setup result: " + result);

                    // Ask Google Play for a products list on a background thread
                    final ArrayList<String> additionalSkuList = new ArrayList<String>();
                    additionalSkuList.add(Helpers.TWELVE_MONTH_SUBSCRIPTION_ID);
                    additionalSkuList.add(Helpers.ONE_MONTH_SUBSCRIPTION_ID);

                    // Add all the individual magazines as products if we're not in debug mode
                    if (!Helpers.debugMode) {
                        ArrayList<Issue> issueList = Publisher.INSTANCE.getIssuesFromFilesystem();
                        for (Issue issue : issueList) {
                            additionalSkuList.add(Helpers.singleIssuePurchaseID(issue.getNumber()));
                        }
                    }

                    IabHelper.QueryInventoryFinishedListener mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
                        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

                            if (result.isFailure()) {
                                // handle error
                                Log.i("Subscribe", "Query failed: " + result);
                                return;
                            }

                            // Check subscription inventory
                            Log.i("Subscribe", "Inventory: " + inventory);

                            // Loop through products and add them to mProducts
                            for (String sku : additionalSkuList) {
                                SkuDetails product = inventory.getSkuDetails(sku);
                                if (product != null) {
                                    mProducts.add(product);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    };
                    mHelper.queryInventoryAsync(true, additionalSkuList, mQueryFinishedListener);
                }
            });

            return rootView;
        }

        // Adapter for CardView
        public class SubscribeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

            private static final int TYPE_HEADER = 0;
            private static final int TYPE_PRODUCT = 1;
            private static final int TYPE_FOOTER = 2;

            public SubscribeAdapter() {

            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = null;
                if (viewType == 0) {
                    // Header
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_subscribe_header_view, parent, false);
                    return new SubscribeHeaderViewHolder(itemView);

                } else if (viewType == 1) {
                    // Product
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_subscribe_card_view, parent, false);
                    return new SubscribeViewHolder(itemView);

                } else if (viewType == 2) {
                    // Footer
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.fragment_subscribe_footer_view, parent, false);
                    return new SubscribeFooterViewHolder(itemView);
                } else {
                    // Uh oh... didn't match view type.
                    return null;
                }
            }

            @Override
            public int getItemCount() {
//                return mProducts.size() + 3; // 3 = 2 x header + footer
                return mProducts.size();
            }

            @Override
            public int getItemViewType(int position) {
                if (isPositionHeader(position)) {
                    return TYPE_HEADER;
                } else if (isPositionFooter(position)) {
                    return TYPE_FOOTER;
                }
                return TYPE_PRODUCT;
            }

            private boolean isPositionHeader(int position) {
//                return position == 0;
                return false;
            }

            private boolean isPositionFooter(int position) {
//                return position == mProducts.size() + 2; // 0 position + 2 headers?
                return false;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                // Could we recycle? Not sure..
//                holder.setIsRecyclable(false);

                if (holder instanceof SubscribeHeaderViewHolder) {
                    // Header

                    // TODO: Setup header...
                    ((SubscribeHeaderViewHolder) holder).productType.setText("TODO: Set type text.");

                } else if (holder instanceof SubscribeViewHolder) {
                    // In-app product

                    // Setup product
                    ((SubscribeViewHolder) holder).productTitle.setText(mProducts.get(position).getTitle());
                    ((SubscribeViewHolder) holder).productDescription.setText(mProducts.get(position).getDescription());
                    ((SubscribeViewHolder) holder).productPrice.setText(mProducts.get(position).getPrice());

                } else if (holder instanceof SubscribeFooterViewHolder) {
                    // Footer

                    // TODO: Setup restore purchases???
                    ((SubscribeFooterViewHolder) holder).restorePurchases.setText("TODO: Restore!");
                }
            }


            public class SubscribeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView productTitle;
                public TextView productDescription;
                public TextView productPrice;

                public SubscribeViewHolder(View itemView) {
                    super(itemView);
                    productTitle = (TextView) itemView.findViewById(R.id.subscribe_product_title);
                    productDescription = (TextView) itemView.findViewById(R.id.subscribe_product_description);
                    productPrice = (TextView) itemView.findViewById(R.id.subscribe_product_price);
                    itemView.setOnClickListener(this);
                }

                @Override
                public void onClick(View v) {
                    // TODO: Purchase product!
                    Log.i("Subscribe", "Product tapped at position: " + getPosition());
                }
            }

            public class SubscribeHeaderViewHolder extends RecyclerView.ViewHolder {

                public TextView productType;

                public SubscribeHeaderViewHolder(View itemView) {
                    super(itemView);
                    productType = (TextView) itemView.findViewById(R.id.subscribe_header_product_type);
                }
            }

            public class SubscribeFooterViewHolder extends RecyclerView.ViewHolder {

                public Button restorePurchases;

                public SubscribeFooterViewHolder(View itemView) {
                    super(itemView);
                    restorePurchases = (Button) itemView.findViewById(R.id.subscribe_footer_restore);
                }
            }
        }
    }
}
