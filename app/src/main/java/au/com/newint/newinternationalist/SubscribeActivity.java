package au.com.newint.newinternationalist;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SubscribeActivity extends AppCompatActivity {

    static Billing mBilling;
    static List<ProductDetails> mProducts;
    static int mPositionTapped;
    static ArrayList<Issue> mIssueList;
    static SubscribeActivity.SubscribeActivityFragment.SubscribeAdapter mSubscribeAdapter;
    static ProgressDialog mProgressDialog;

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
            mSubscribeAdapter = adapter;

            // Setup in-app billing
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setTitle(getResources().getString(R.string.subscribe_loading_progress_title));
            mProgressDialog.setMessage(getResources().getString(R.string.subscribe_loading_progress_message));
            mProgressDialog.show();

            // Billing v5
            mBilling = new Billing();
            mBilling.productDetailsResponseListener = (billingResult, productDetailsResult) -> {
                Helpers.debugLog("Billing", "Debug... " + billingResult.getDebugMessage());
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // Process returned productDetailsList
                    List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();
                    Helpers.debugLog("Billing", "Product details list: " + productDetailsList);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            List<ProductDetails> productsList = new ArrayList();
                            for (ProductDetails item: productDetailsList) {
                                if (item.getProductType().equals(BillingClient.ProductType.SUBS)) {
                                    mProducts.add(0, item);
                                }
                            }
                            for (ProductDetails item: productDetailsList) {
                                if (item.getProductType().equals(BillingClient.ProductType.INAPP)) {
                                    productsList.add(item);
                                }
                            }
                            Collections.sort(productsList, new Comparator<ProductDetails>() {
                                @Override
                                public int compare(ProductDetails p1, ProductDetails p2) {
                                    return p2.getProductId().compareTo(p1.getProductId());
                                }
                            });
                            mProducts.addAll(productsList);
                            mSubscribeAdapter.notifyDataSetChanged();
                        }
                    };
                    mainHandler.post(myRunnable);
                } else {
                    Helpers.debugLog("Billing", "Failed... " + billingResult.getDebugMessage());
                }
            };
            mBilling.setupBillingClient(getActivity().getApplicationContext());

            // TODO: Purchase history? https://developer.android.com/google/play/billing/integrate#fetch

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
                    ProductDetails product = mProducts.get(position);
                    String productSku = product.getProductId();
                    final SubscribeViewHolder viewHolder = ((SubscribeViewHolder) holder);

                    // Setup product image
                    if (productSku.contains("single")) {
                        // It's a single magazine purchase, so load the cover
                        int productID = Integer.parseInt(productSku.replaceAll("\\D+",""));
                        int issueListPosition = 0;
                        boolean issueFound = false;
                        mIssueList = Publisher.INSTANCE.getIssuesFromFilesystem();
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
                    viewHolder.productPrice.setText("$-");
                    if (product.getProductType().equals(BillingClient.ProductType.SUBS)) {
                        try {
                            String price = product.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
                            viewHolder.productPrice.setText(price);
                        } catch (Exception e) {
                            Helpers.debugLog("Subscribe", "Setup subscription failed: " + e);
                        }
                    } else {
                        try {
                            String price = product.getOneTimePurchaseOfferDetails().getFormattedPrice();
                            viewHolder.productPrice.setText(price);
                        } catch (Exception e) {
                            Helpers.debugLog("Subscribe", "Setup product failed: " + e);
                        }
                    }

                    // If product has been purchased
                    if (mBilling.isPurchased(product)) {
                        CardView cardView = (CardView) viewHolder.itemView.findViewById(R.id.subscribe_card_view);
                        cardView.setCardBackgroundColor(getResources().getColor(R.color.material_deep_teal_200));
                        viewHolder.productPrice.setText(viewHolder.productPrice.getText() + " (Purchased)");
                        viewHolder.setIsRecyclable(false);
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

                    // Initiate purchase
                    mBilling.launchBillingFlow((AppCompatActivity) getActivity(), mProducts.get(getPosition()));
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
