package au.com.newint.newinternationalist;

import android.content.Context;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class Billing {
    public BillingClient billingClient;
    public ProductDetailsResponseListener productDetailsResponseListener;
    public List<Purchase> allPurchases;

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NotNull BillingResult billingResult, List<Purchase> purchases) {
            Helpers.debugLog("Billing", "Billing result: " + billingResult);
            Helpers.debugLog("Billing", "On Purchases Updated Purchases: " + purchases);

            if (allPurchases == null) {
                allPurchases = purchases;
            } else if (purchases != null) {
                allPurchases.addAll(purchases);
            }

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
            } else {
                // Handle any other error codes.
            }
        }
    };

    private PurchasesResponseListener purchasesResponseListener = new PurchasesResponseListener() {
        public void onQueryPurchasesResponse(BillingResult billingResult, List purchases) {
            // check billingResult
            Helpers.debugLog("Billing", "Purchases Response Listener Billing result: " + billingResult);
            Helpers.debugLog("Billing", "Purchases Response Listener Purchases: " + purchases);

            if (allPurchases == null) {
                allPurchases = purchases;
            } else if (purchases != null) {
                allPurchases.addAll(purchases);
            }

            // process returned purchase list, e.g. display the plans user owns
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Object purchase : purchases) {
                    Helpers.debugLog("Billing", "Purchases Response Listener Purchase: " + purchase);
                }
            }
        }
    };

    public void setupBillingClient(Context context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NotNull BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryProducts();
                    queryPurchases();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Helpers.debugLog("Billing", "Billing disconnected...");
            }
        });
    };

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> products  = new ArrayList();

        // Add subscriptions
        products.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Helpers.TWELVE_MONTH_SUBSCRIPTION_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );
        products.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Helpers.ONE_MONTH_SUBSCRIPTION_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    products
                )
                .build();

        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams,
            productDetailsResponseListener
        );

        // Add single issues
        products  = new ArrayList();
        List<Issue> issues = Publisher.INSTANCE.getIssuesFromFilesystem();
        for (Issue issue : issues) {
            products.add(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(issue.getNumber() + "single")
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
            );
        }

        queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                products
                        )
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                productDetailsResponseListener
        );
    }

    private void queryPurchases() {
        // To check for purchases on other devices
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                purchasesResponseListener
        );
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                purchasesResponseListener
        );
    }

    public void launchBillingFlow(AppCompatActivity activity, ProductDetails productDetails) {
        List productDetailsParamsList;
        if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS)) {
            productDetailsParamsList =
                    ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                    .setProductDetails(productDetails)
                                    // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                    // for a list of offers that are available to the user
                                    .setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken())
                                    .build()
                    );
        } else {
            productDetailsParamsList =
                    ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                    .setProductDetails(productDetails)
                                    // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                    // for a list of offers that are available to the user
                                    .build()
                    );
        }

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        // Launch the billing flow
        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        Helpers.debugLog("Billing", "Launch Billing Flow Purchase result: " + billingResult);
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        Helpers.debugLog("Billing", "Handle Purchase Acknowledgement: " + billingResult);
                    }
                };
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }
    }

    public boolean isPurchased(ProductDetails productDetails) {
        if (allPurchases == null) {
            return false;
        }
        for (Purchase purchase : allPurchases) {
            if (purchase.getProducts().get(0).equals(productDetails.getProductId())) {
                Helpers.debugLog("Billing", "isPurchased: " + purchase);
                return true;
            }
        }
        return false;
    }
}
