package au.com.newint.newinternationalist;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import au.com.newint.newinternationalist.util.IabHelper;
import au.com.newint.newinternationalist.util.IabResult;
import au.com.newint.newinternationalist.util.Inventory;
import au.com.newint.newinternationalist.util.Purchase;
import au.com.newint.newinternationalist.util.SkuDetails;


public class SubscribeActivity extends ActionBarActivity {

    static IabHelper mHelper;
    static Inventory mInventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SubscribeActivityFragment())
                    .commit();
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

            // TODO: Get inventory from Google Play

            // Setup in-app billing
            mHelper = Helpers.setupIabHelper(getActivity().getApplicationContext());

            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem.
                        Log.d("Subscribe", "Problem setting up In-app Billing: " + result);
                    }
                    // Hooray, IAB is fully set up!
                    Log.i("Subscribe", "In-app billing setup result: " + result);

                    // Ask Google Play for a products list on a background thread
                    ArrayList<String> additionalSkuList = new ArrayList<String>();
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

                            TextView textView = (TextView) rootView.findViewById(R.id.subscribe_tmp);

                            if (result.isFailure()) {
                                // handle error
                                Log.i("Subscribe", "Query failed: " + result);
                                textView.setText(result.toString());
                                return;
                            }

                            // Check subscription inventory
                            Log.i("Subscribe", "Inventory: " + inventory);
                            mInventory = inventory;
                            textView.setText(mInventory.toString());

                        }
                    };
                    mHelper.queryInventoryAsync(true, additionalSkuList, mQueryFinishedListener);
                }
            });

            // TODO: Setup view

            return rootView;
        }
    }
}
