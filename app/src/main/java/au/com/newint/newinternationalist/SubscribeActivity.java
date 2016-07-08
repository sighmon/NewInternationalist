package au.com.newint.newinternationalist;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
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
    static int mPositionTapped;
    static ArrayList<Issue> mIssueList;
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
    public void onResume() {
        super.onResume();

        // Send Google Analytics if the user allows it
        Helpers.sendGoogleAnalytics(getResources().getString(R.string.title_activity_subscribe));
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

            if (!Helpers.emulator) {
                // Only startSetup if not running in an emulator
                // Get inventory from Google Play
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        if (!result.isSuccess()) {
                            // Oh noes, there was a problem.
                            Log.d("Subscribe", "Problem setting up In-app Billing: " + result);
                        }
                        // Hooray, IAB is fully set up!
                        Helpers.debugLog("Subscribe", "In-app billing setup result: " + result);

                        // Consume the test purchase..
                        if (BuildConfig.DEBUG) {
                            try {
                                Inventory inventory = mHelper.queryInventory(false, null);
                                Purchase purchase = inventory.getPurchase("android.test.purchased");
                                if (purchase != null) {
                                    mHelper.consumeAsync(inventory.getPurchase("android.test.purchased"), null);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Ask Google Play for a products list on a background thread
                        final ArrayList<String> additionalSkuList = new ArrayList<String>();
                        additionalSkuList.add(Helpers.TWELVE_MONTH_SUBSCRIPTION_ID);
                        additionalSkuList.add(Helpers.ONE_MONTH_SUBSCRIPTION_ID);

                        mIssueList = Publisher.INSTANCE.getIssuesFromFilesystem();
                        for (Issue issue : mIssueList) {
                            additionalSkuList.add(Helpers.singleIssuePurchaseID(issue.getNumber()));
                        }

                        final ProgressDialog progress = new ProgressDialog(getActivity());
                        progress.setTitle(getResources().getString(R.string.subscribe_loading_progress_title));
                        progress.setMessage(getResources().getString(R.string.subscribe_loading_progress_message));
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
                                        Helpers.debugLog("Subscribe", "Inventory (" + loopNumber + "): " + inventory);

                                        // Loop through products and add them to mProducts
                                        for (String sku : partition) {
                                            SkuDetails product = inventory.getSkuDetails(sku);
                                            if (product != null) {
                                                mProducts.add(product);
                                            }
                                        }
                                    } catch (Exception e) {
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
                            public void onIabPurchaseFinished(IabResult result, final Purchase purchase)
                            {
                                if (result.isFailure()) {
                                    if (result.getResponse() == 7) {
                                        // Already purchased!
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                        builder.setMessage(R.string.subscribe_already_purchased_message).setTitle(R.string.subscribe_already_purchased_title);
                                        builder.setPositiveButton(R.string.subscribe_already_purchased_read_issue_button, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                // User clicked Read this issue button
                                                Intent issueIntent = new Intent(rootView.getContext(), TableOfContentsActivity.class);
                                                int issueNumber = Integer.parseInt(mProducts.get(mPositionTapped).getSku().replaceAll("[\\D]", ""));
                                                Issue issueForIntent = null;
                                                for (Issue issue : mIssueList) {
                                                    if (issue.getNumber() == issueNumber) {
                                                        issueForIntent = issue;
                                                    }
                                                }
                                                if (issueForIntent != null) {
                                                    issueIntent.putExtra("issue", issueForIntent);
                                                    startActivity(issueIntent);
                                                }
                                            }
                                        });
                                        builder.setNegativeButton(R.string.subscribe_already_purchased_cancel_button, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                // User cancelled the dialog
                                            }
                                        });
                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                    } else {
                                        Log.d("Subscribe", "Error purchasing: " + result);
                                    }
                                    return;
                                } else if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                                    // User cancelled the purchase, so ignore, move on
                                    Helpers.debugLog("Subscribe", "User cancelled purchase.");
                                    return;
                                } else if (purchase.getSku().equals(Helpers.TWELVE_MONTH_SUBSCRIPTION_ID)) {
                                    // Update subscription status.
                                    Helpers.debugLog("Subscribe", "Purchase succeeded: " + purchase.getItemType());
                                    // Send analytics event if user permits
                                    SkuDetails productPurchased = null;
                                    productPurchased = getProductForPurchase(purchase);
                                    Helpers.sendGoogleAnalyticsEvent("Google Play", "Subscription", purchase.getItemType(), productPurchased.getPrice());
                                    Helpers.sendGoogleAdwordsConversion(productPurchased);

                                } else if (purchase.getSku().equals(Helpers.ONE_MONTH_SUBSCRIPTION_ID)) {
                                    // Update subscription status.
                                    Helpers.debugLog("Subscribe", "Purchase succeeded: " + purchase.getItemType());
                                    // Send analytics event if user permits
                                    SkuDetails productPurchased = null;
                                    productPurchased = getProductForPurchase(purchase);
                                    Helpers.sendGoogleAnalyticsEvent("Google Play", "Subscription", purchase.getItemType(), productPurchased.getPrice());
                                    Helpers.sendGoogleAdwordsConversion(productPurchased);
                                } else {
                                    // Handle individual purchases
                                    Helpers.debugLog("Subscribe", "Individual purchase: " + purchase.getItemType());
                                    adapter.notifyDataSetChanged();
                                    // Send analytics event if user permits
                                    SkuDetails productPurchased = null;
                                    productPurchased = getProductForPurchase(purchase);
                                    Helpers.sendGoogleAnalyticsEvent("Google Play", "Purchase", purchase.getItemType(), productPurchased.getPrice());
                                    Helpers.sendGoogleAdwordsConversion(productPurchased);
                                }
                            }
                        };
                    }
                });
            }

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
                // Don't need the footer in the end...
//                return mProducts.size() + 1; // Plus footer
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
                // Don't need the restore footer in the end...
//                return position == mProducts.size();
                return false;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                // Recycling the viewHolder for all but the purchases
                holder.setIsRecyclable(false);

                if (holder instanceof SubscribeHeaderViewHolder) {
                    // Header

                    // TODO: Do we need headers separating the Subscriptions from the single issues?
                    ((SubscribeHeaderViewHolder) holder).productType.setText("TODO: Set type text.");

                } else if (holder instanceof SubscribeViewHolder) {
                    // In-app product
                    SkuDetails product = mProducts.get(position);
                    String productSku = product.getSku();
                    final SubscribeViewHolder viewHolder = ((SubscribeViewHolder) holder);

                    // Setup product image
                    if (productSku.contains("single")) {
                        // It's a single magazine purchase, so load the cover
                        int productID = Integer.parseInt(productSku.replaceAll("\\D+",""));
                        int issueListPosition = 0;
                        boolean issueFound = false;
                        for (int i = 0; i < mIssueList.size(); i++) {
                            if (mIssueList.get(i).getNumber() == productID) {
                                issueListPosition = i;
                                issueFound = true;
                            }
                        }
                        if (issueFound) {
                            Issue issue = mIssueList.get(issueListPosition);
                            int coverWidth = Math.round(getResources().getDimension(R.dimen.subscribe_cover_width));
                            issue.getCoverCacheStreamFactoryForSize(coverWidth).preload(new CacheStreamFactory.CachePreloadCallback() {
                                @Override
                                public void onLoad(byte[] payload) {
                                    if (payload != null && payload.length > 0) {
                                        final Bitmap coverBitmap = Helpers.bitmapDecode(payload);

                                        Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
                                        final Animation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
                                        fadeOutAnimation.setDuration(100);
                                        fadeInAnimation.setDuration(200);
                                        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                                            @Override
                                            public void onAnimationStart(Animation animation) {

                                            }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                viewHolder.productImage.setImageBitmap(coverBitmap);
                                                viewHolder.productImage.startAnimation(fadeInAnimation);
                                            }

                                            @Override
                                            public void onAnimationRepeat(Animation animation) {

                                            }
                                        });
                                        viewHolder.productImage.startAnimation(fadeOutAnimation);
                                    }
                                }

                                @Override
                                public void onLoadBackground(byte[] payload) {

                                }
                            });
                        }
                    }

                    // Setup product
                    viewHolder.productTitle.setText(product.getTitle().replace(" (New Internationalist magazine)", ""));
                    viewHolder.productDescription.setText(product.getDescription());
                    viewHolder.productPrice.setText(product.getPrice());

                    // If product has been purchased
                    try {
                        Inventory inventory = mHelper.queryInventory(false, null);
                        Purchase purchase = inventory.getPurchase(product.getSku());
                        // NOTE: Purchase is double checked via Rails when actually trying to get the article body
                        if (purchase != null) {
                            CardView cardView = (CardView) viewHolder.itemView.findViewById(R.id.subscribe_card_view);
                            cardView.setCardBackgroundColor(getResources().getColor(R.color.material_deep_teal_200));
                            viewHolder.productPrice.setText(product.getPrice() + " (Purchased!)");
                            viewHolder.setIsRecyclable(false);
                        }
                    } catch (IabException e) {
                        e.printStackTrace();
                    }

                } else if (holder instanceof SubscribeFooterViewHolder) {
                    // Footer

                    // TODO: Make sure that purchases are restored each time the inventory & purchases are checked
                    ((SubscribeFooterViewHolder) holder).restorePurchases.setText("TODO: Restore purchases!");
                }
            }


            public class SubscribeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public ImageView productImage;
                public TextView productTitle;
                public TextView productDescription;
                public TextView productPrice;

                public SubscribeViewHolder(View itemView) {
                    super(itemView);
                    productImage = (ImageView) itemView.findViewById(R.id.subscribe_product_image);
                    productTitle = (TextView) itemView.findViewById(R.id.subscribe_product_title);
                    productDescription = (TextView) itemView.findViewById(R.id.subscribe_product_description);
                    productPrice = (TextView) itemView.findViewById(R.id.subscribe_product_price);
                    itemView.setOnClickListener(this);
                }

                @Override
                public void onClick(View v) {
                    // Purchase product!
                    Helpers.debugLog("Subscribe", "Product tapped at position: " + getPosition());
                    mPositionTapped = getPosition();
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

    private static SkuDetails getProductForPurchase(Purchase purchase) {
        SkuDetails productPurchased = null;
        if (mProducts != null && mProducts.size() > 0) {
            for (SkuDetails product : mProducts) {
                if (product.getSku().equals(purchase.getSku())) {
                    productPurchased = product;
                }
            }
        }
        return productPurchased;
    }
}
