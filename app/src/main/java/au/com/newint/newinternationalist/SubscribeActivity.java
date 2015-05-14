package au.com.newint.newinternationalist;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.com.newint.newinternationalist.util.IabException;
import au.com.newint.newinternationalist.util.IabHelper;
import au.com.newint.newinternationalist.util.IabResult;
import au.com.newint.newinternationalist.util.Inventory;
import au.com.newint.newinternationalist.util.Purchase;
import au.com.newint.newinternationalist.util.SkuDetails;


public class SubscribeActivity extends ActionBarActivity {

    static IabHelper mHelper;
    static ArrayList<SkuDetails> mProducts;
    static IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener;

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
    public void onDestroy() {
        super.onDestroy();
        // Dispose of the in-app helper
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Subscribe", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d("Subscribe", "onActivityResult handled by IABUtil.");
        }
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

                    // Consume the test purchase..
                    if (BuildConfig.DEBUG) {
                        try {
                            Inventory inventory = mHelper.queryInventory(false, null);
                            Purchase purchase = inventory.getPurchase("android.test.purchased");
                            if (purchase != null) {
                                mHelper.consumeAsync(inventory.getPurchase("android.test.purchased"), null);
                            }
                        } catch (IabException e) {
                            e.printStackTrace();
                        }
                    }

                    // Ask Google Play for a products list on a background thread
                    final ArrayList<String> additionalSkuList = new ArrayList<String>();
                    additionalSkuList.add(Helpers.TWELVE_MONTH_SUBSCRIPTION_ID);
                    additionalSkuList.add(Helpers.ONE_MONTH_SUBSCRIPTION_ID);

                    ArrayList<Issue> issueList = Publisher.INSTANCE.getIssuesFromFilesystem();
                    for (Issue issue : issueList) {
                        additionalSkuList.add(Helpers.singleIssuePurchaseID(issue.getNumber()));
                    }

                    final ProgressDialog progress = new ProgressDialog(getActivity());
                    progress.setTitle("Loading");
                    progress.setMessage("Loading in-app purchases...");
                    progress.show();

                    new AsyncTask<Void, Integer, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {

                            int partitionSize = Helpers.GOOGLE_PLAY_MAX_SKU_LIST_SIZE;
                            for (int i = 0; i < additionalSkuList.size(); i+=partitionSize) {
                                final int loopNumber = i;
                                final List<String> partition = additionalSkuList.subList(i, Math.min(i + partitionSize, additionalSkuList.size()));
                                try {
                                    Inventory inventory = mHelper.queryInventory(true, partition);

                                    // Check subscription inventory
                                    Log.i("Subscribe", "Inventory (" + loopNumber + "): " + inventory);

                                    // Loop through products and add them to mProducts
                                    for (String sku : partition) {
                                        SkuDetails product = inventory.getSkuDetails(sku);
                                        if (product != null) {
                                            mProducts.add(product);
                                        }
                                    }
                                } catch (IabException e) {
                                    e.printStackTrace();
                                }
                            }

                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void nothing) {
                            super.onPostExecute(nothing);

                            // Update adapter
                            adapter.notifyDataSetChanged();
                            progress.dismiss();
                        }

                    }.execute();

                    mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
                        public void onIabPurchaseFinished(IabResult result, Purchase purchase)
                        {
                            if (result.isFailure()) {
                                Log.d("Subscribe", "Error purchasing: " + result);
                                return;
                            } else if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                                // User cancelled the purchase, so ignore, move on
                                Log.i("Subscribe", "User cancelled purchase.");
                                return;
                            } else if (purchase.getSku().equals(Helpers.TWELVE_MONTH_SUBSCRIPTION_ID)) {
                                // TODO: Update subscription status.
                                Log.i("Subscribe", "Purchase succeeded: " + purchase.getItemType());

                            } else if (purchase.getSku().equals(Helpers.ONE_MONTH_SUBSCRIPTION_ID)) {
                                // TODO: Update subscription status.
                                Log.i("Subscribe", "Purchase succeeded: " + purchase.getItemType());
                            } else {
                                // TODO: Handle individual purchases
                                Log.i("Subscribe", "Individual purchase: " + purchase.getItemType());
                            }
                        }
                    };
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
                return mProducts.size() + 1; // Plus footer
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
                return position == mProducts.size();
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
                    ((SubscribeFooterViewHolder) holder).restorePurchases.setText("TODO: Restore purchases!");
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
                    // TODO: Generate developerPayload in helper, now just returns an empty string
                    String developerPayload = Helpers.getDeveloperPayload();
                    // For testing:
                    // mHelper.launchPurchaseFlow(getActivity(), "android.test.purchased", Helpers.GOOGLE_PLAY_REQUEST_CODE, mPurchaseFinishedListener, developerPayload);
                    // For real:
                    mHelper.launchPurchaseFlow(getActivity(), mProducts.get(getPosition()).getSku(), Helpers.GOOGLE_PLAY_REQUEST_CODE, mPurchaseFinishedListener, developerPayload);
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
