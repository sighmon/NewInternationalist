package au.com.newint.newinternationalist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
//        setupActionBar();

        // Send Google Analytics if the user allows it
        Helpers.sendGoogleAnalytics(getResources().getString(R.string.title_activity_login));

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
//        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button forgotPasswordButton = (Button) findViewById(R.id.login_forgotten_password);
        String languageLocale = Locale.getDefault().toString();
        if (languageLocale.equals("en_GB") || languageLocale.equals("en_CA") || languageLocale.equals("en_US")) {
            // Remove forgot password button and don't add listener
            forgotPasswordButton.setVisibility(View.GONE);
        } else {
            forgotPasswordButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Helpers.getSiteURL() + "users/password/new"));
                    startActivity(browserIntent);
                }
            });
        }

        Button signUpButton = (Button) findViewById(R.id.login_sign_up);
        signUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Helpers.getSiteURL() + "users/sign_up"));
                startActivity(browserIntent);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        // Load the user credentials
        mEmailView.setText(Helpers.getFromPrefs(Helpers.LOGIN_USERNAME_KEY, ""));
        mPasswordView.setText(Helpers.getPassword(""));
    }

    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check that the password isn't empty.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check the the username/email isn't empty.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Removed the auto-fill from contacts stuff so we don't need to ask for access to users contacts. :-)
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
//        cursor.moveToFirst();
//        while (!cursor.isAfterLast()) {
//            emails.add(cursor.getString(ProfileQuery.ADDRESS));
//            cursor.moveToNext();
//        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    // User login to Rails.
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private String loginErrorString = getString(R.string.error_incorrect_password);

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            boolean success = false;

            // Try logging into Rails for authentication.
            DefaultHttpClient httpclient = new DefaultHttpClient();

            // List current cookies
            List<Cookie> cookies = Publisher.INSTANCE.cookieStore.getCookies();
            if( !cookies.isEmpty() ){
                for (Cookie cookie : cookies){
                    String cookieString = cookie.getName() + " : " + cookie.getValue();
                    Helpers.debugLog("Cookie", "Old cookie: " + cookieString);
                }
            }

            // Delete cookies.
            Publisher.INSTANCE.deleteCookieStore();

            // Try to connect
            HttpContext ctx = new BasicHttpContext();
            ctx.setAttribute(ClientContext.COOKIE_STORE, Publisher.INSTANCE.cookieStore);
            HttpPost post = new HttpPost(Helpers.getSiteURL() + "users/sign_in.json");
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse response = null;

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("user[login]", mEmail));
                nameValuePairs.add(new BasicNameValuePair("user[password]", mPassword));
                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Save username to SharedPreferences
                Helpers.saveToPrefs(Helpers.LOGIN_USERNAME_KEY,mEmail);

                // Execute HTTP Post Request
                response = httpclient.execute(post, ctx);

            } catch (ClientProtocolException e) {
                Helpers.debugLog("Login", "ClientProtocolException: " + e);
            } catch (IOException e) {
                Helpers.debugLog("Login", "IOException: " + e);
            }

            int responseStatusCode;
            Publisher.INSTANCE.loggedIn = false;
            if (response != null) {
                responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode > 200 && responseStatusCode < 300) {
                    // Login was successful, we should have a cookie
                    success = true;
                    Publisher.INSTANCE.loggedIn = true;

                    Helpers.savePassword(mPassword);

                } else if (responseStatusCode > 400 && responseStatusCode < 500) {
                    // Login was incorrect.
                    Helpers.debugLog("Login", "Failed with code: " + responseStatusCode);

                } else {
                    // Server error.
                    Helpers.debugLog("Login", "Failed with code: " + responseStatusCode + " and response: " + response.getStatusLine());
                    loginErrorString = getString(R.string.login_error_server_error);
                }

            } else {
                // Error logging in
                Helpers.debugLog("Login", "Failed! Response is null");
                loginErrorString = getString(R.string.login_error_no_internet);
            }

            return success;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();

                // Let listener know
                for (Publisher.LoginListener listener : Publisher.INSTANCE.loginListeners) {
                    Helpers.debugLog("Login", "Sending listener login success: True");
                    // Pass in login success boolean
                    listener.onUpdate(success);
                }

            } else {
                mPasswordView.setError(loginErrorString);
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}



