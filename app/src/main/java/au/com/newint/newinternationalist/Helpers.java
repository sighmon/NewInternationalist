package au.com.newint.newinternationalist;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.ads.conversiontracking.AdWordsConversionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import au.com.newint.newinternationalist.util.IabHelper;
import au.com.newint.newinternationalist.util.SkuDetails;

/**
 * Created by New Internationalist on 26/02/15.
 */
public class Helpers {

    public static final String LOGIN_USERNAME_KEY = "newintLogin" ;
    public static final String LOGIN_PASSWORD_KEY = "newintHarmless" ;

    public static final String TWELVE_MONTH_SUBSCRIPTION_ID = "12monthauto";
    public static final String ONE_MONTH_SUBSCRIPTION_ID = "1monthauto";

    public static final int GOOGLE_PLAY_REQUEST_CODE = 5000;
    public static final int GOOGLE_PLAY_MAX_SKU_LIST_SIZE = 16;
    public static final String GOOGLE_PLAY_APP_URL = "https://play.google.com/store/apps/details?id=" + ((MainActivity.applicationContext.getPackageName() == null) ? "" : MainActivity.applicationContext.getPackageName());

    public static final boolean debugMode = BuildConfig.DEBUG;

    public static boolean emulator = Build.FINGERPRINT.contains("generic");

    private static FirebaseAnalytics mFirebaseAnalytics;

    public static float screenHeight() {
        DisplayMetrics displayMetrics = MainActivity.applicationContext.getResources().getDisplayMetrics();

        return MainActivity.applicationContext.getResources().getDisplayMetrics().heightPixels / displayMetrics.density;
    }

    public static float screenWidth() {
        DisplayMetrics displayMetrics = MainActivity.applicationContext.getResources().getDisplayMetrics();

        return MainActivity.applicationContext.getResources().getDisplayMetrics().widthPixels / displayMetrics.density;
    }

    public static RoundedBitmapDrawable roundDrawableFromBitmap(Bitmap bitmap) {
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(MainActivity.applicationResources, bitmap);
        roundedBitmapDrawable.setAntiAlias(true);
        roundedBitmapDrawable.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
        return roundedBitmapDrawable;
    }

    public static File getStorageDirectory() {
        return getStorageDirectory(Helpers.getFromPrefs(MainActivity.applicationContext.getResources().getString(R.string.use_external_storage), false));
    }

    public static File getStorageDirectory(boolean userRequestsExternalStorage) {
        File externalStorage = MainActivity.applicationContext.getExternalFilesDir(null);
        boolean emulated = Environment.isExternalStorageEmulated();
        boolean mounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        if (Build.VERSION.SDK_INT >= 21) {
            // If API is >= 21 check to see if external SD card is present, not emulated and mounted
            File[] externalFilesDirs = MainActivity.applicationContext.getExternalFilesDirs(null);
            if (externalFilesDirs != null && externalFilesDirs.length > 0) {
                for (File dir : externalFilesDirs) {
                    if (dir != null && !Environment.isExternalStorageEmulated(dir) && Environment.getExternalStorageState(dir).equals(Environment.MEDIA_MOUNTED)) {
                        emulated = false;
                        mounted = true;
                        externalStorage = dir;
                        break;
                    }
                }
            }
        }

        if (userRequestsExternalStorage && externalStorage != null && !emulated && mounted) {
            // This device has external storage, so use that to store data
            return externalStorage;
        } else {
            // This device doesn't have external storage, so use internal
            return MainActivity.applicationContext.getFilesDir();
        }
    }

    public static String getSiteURL() {
        return getVariableFromConfig("SITE_URL");
    }

    public static String getVariableFromConfig(String string) {
        Resources resources = MainActivity.applicationContext.getResources();
        AssetManager assetManager = resources.getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(string);
        } catch (IOException e) {
            Log.e("Properties", "Failed to open config property file");
            return null;
        }
    }

    public static void saveToPrefs(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.applicationContext);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key,value);
        editor.apply();
    }

    public static void saveToPrefs(String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.applicationContext);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static String getFromPrefs(String key, String defaultValue) {
        // Default value will be returned of no value found or error occurred.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.applicationContext);
        try {
            return sharedPrefs.getString(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue; // Nothing for key or error, defaultValue returned
        }
    }

    public static boolean getFromPrefs(String key, boolean defaultValue) {
        // Default value will be returned of no value found or error occurred.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.applicationContext);
        try {
            return sharedPrefs.getBoolean(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue; // Nothing for key or error, defaultValue returned
        }
    }

    public static String getDeveloperPayload() {
        // We don't want to collect any extra data
        return "";
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static byte[] getKey() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // we know that the device ID isn't very secure but this is just to obscure naive attacks
            byte[] id = hexStringToByteArray(Settings.Secure.ANDROID_ID);
            md.update(id, 0, id.length);
            return md.digest();
        } catch (Exception e) {
            Log.e("Helper.getKey","error digesting ANDROID_ID: "+e);
            return null;
        }
    }

    public static void savePassword(String value) {

        SecretKeySpec skeySpec = new SecretKeySpec(getKey(), "AES");
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes("UTF-8"));
            String encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
            saveToPrefs(LOGIN_PASSWORD_KEY, encryptedString);
        } catch (Exception e) {
            // TODO: let the user know
            Log.e("Helper.getPassword","Error encrypting password "+e);
        }

    }

    public static String getPassword(String defaultValue) {
        byte[] encryptedBytes = Base64.decode(getFromPrefs(LOGIN_PASSWORD_KEY, defaultValue), Base64.DEFAULT);


        SecretKeySpec skeySpec = new SecretKeySpec(getKey(), "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            return new String(decrypted,"UTF-8");
        } catch (Exception e) {
            // TODO: let the user know?
            Log.e("Helper.getPassword","Error decrypting password "+e);
            return defaultValue;
        }
    }

    public static String wrapInHTML(String htmlToWrap) {
        // Load CSS from file and wrap it in HTML
        return "<html><head><link href='bootstrap.css' type='text/css' rel='stylesheet'/><link href='article-body.css' type='text/css' rel='stylesheet'/></head><body>" + htmlToWrap + "</body></html>";
    }

    public static String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    public static String singleIssuePurchaseID(int magazineNumber) {
        return Integer.toString(magazineNumber) + "single";
    }

    public static boolean isSubscriptionValid(Date purchaseDate, int numberOfMonths) {

        Date todaysDate = new Date();

        return subscriptionExpiryDate(purchaseDate, numberOfMonths).after(todaysDate);
    }

    public static Date subscriptionExpiryDate(Date purchaseDate, int numberOfMonths) {

        // Add numberOfMonths to determine expiry date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(purchaseDate);
        calendar.add(Calendar.MONTH, numberOfMonths);

        return calendar.getTime();
    }

    public static IabHelper setupIabHelper(Context context) {

        String base64EncodedPublicKey = "";
        String publicKey = getVariableFromConfig("PUBLIC_KEY");
        if (publicKey != null) {
            try {
                base64EncodedPublicKey = au.com.newint.newinternationalist.util.Base64.encode(URLDecoder.decode(publicKey, "UTF-8").getBytes("ISO8859_1"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return new IabHelper(context, base64EncodedPublicKey);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
    }

    return inSampleSize;
}

    public static Bitmap scaledBitmapDecode(byte[] bytes, int reqWidth, int reqHeight) {

        if (bytes != null && bytes.length > 0) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } else {
            return null;
        }
    }

    // not actually synchronized any more
    public static Bitmap bitmapDecode(byte[] bytes) {

        //Helpers.debugLog("bitmapDecode", "start");
        Bitmap bitmap = null;
        if (bytes != null && bytes.length > 0) {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            Log.e("bitmapDecode","bytes.length <= 0");
        }
        //Helpers.debugLog("bitmapDecode", "end");

        return bitmap;
    }

    public static void sendGoogleAnalytics(String screenName) {
        boolean allowAnonymousStatistics = getFromPrefs(MainActivity.applicationContext.getResources().getString(R.string.allow_anonymous_statistics_key), false);
        if (allowAnonymousStatistics) {

            // Obtain the FirebaseAnalytics instance.
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(MainActivity.applicationContext);
            try {
                Bundle bundle = new Bundle();
//                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "pageView");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, screenName);
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                Helpers.debugLog("Analytics", "Analytics sent: " + bundle);
            } catch (Exception e) {
                Log.e("FirebaseAnalytics", "ERROR: Failed to log event. " + e);
            }
        }
    }

    public static void sendGoogleAnalyticsEvent(String category, String action, String label) {
        sendGoogleAnalyticsEvent(category, action, label, "0");
    }

    public static void sendGoogleAnalyticsEvent(String category, String action, String label, String value) {
        boolean allowAnonymousStatistics = getFromPrefs(MainActivity.applicationContext.getResources().getString(R.string.allow_anonymous_statistics_key), false);
        if (allowAnonymousStatistics) {
            // Send analytics event
//            try {
//                App.tracker.send(new HitBuilders.EventBuilder()
//                        .setCategory(category)
//                        .setAction(action)
//                        .setLabel(label)
//                        .setValue((long) Float.parseFloat(value))
//                        .build());
//            } catch (Exception e) {
//                Helpers.debugLog("Analytics", "ERROR: Couldn't send inapp purchase analytics - " + e);
//            }

            mFirebaseAnalytics = FirebaseAnalytics.getInstance(MainActivity.applicationContext);
            try {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, category);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, action);
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, label);
                bundle.putString(FirebaseAnalytics.Param.VALUE, value);
//                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, bundle);
            } catch (Exception e) {
                Log.e("FirebaseAnalytics", "ERROR: Failed to log event. " + e);
            }
        }
    }

    public static void sendGoogleAdwordsConversion(SkuDetails productPurchased) {
        boolean allowAnonymousStatistics = getFromPrefs(MainActivity.applicationContext.getResources().getString(R.string.allow_anonymous_statistics_key), false);
        if (allowAnonymousStatistics) {
            // Send conversion
            String purchasePrice = "0";
            if (productPurchased != null) {
                purchasePrice = productPurchased.getPrice();
            }
            try {
                AdWordsConversionReporter.reportWithConversionId(MainActivity.applicationContext,
                        Helpers.getVariableFromConfig("ADWORDS_ID"),
                        Helpers.getVariableFromConfig("ADWORDS_KEY"),
                        purchasePrice, true);
            } catch (Exception e) {
                Helpers.debugLog("Analytics", "ERROR: Couldn't send inapp purchase adwords conversion - " + e);
            }
        }
    }

    public static void registerGoogleConversionsReferrer(Intent intent) {
        boolean allowAnonymousStatistics = getFromPrefs(MainActivity.applicationContext.getResources().getString(R.string.allow_anonymous_statistics_key), false);
        if (allowAnonymousStatistics) {
            AdWordsConversionReporter.registerReferrer(MainActivity.applicationContext, intent.getData());
        }
    }

    public static boolean moveDirectoryToDirectory(File sourceDirectory, File targetDirectory) {
        try {
            FileUtils.copyDirectoryToDirectory(sourceDirectory, targetDirectory.getParentFile());
            FileUtils.deleteDirectory(sourceDirectory);
        } catch (IOException e) {
            Helpers.debugLog("moveDirectoryToDirectory", "IOException: "+e.toString());
            return false;
        }
        return true;
    }



    public static long bytesAvailable(File f) {
        StatFs stat = new StatFs(f.getPath());
        if (Build.VERSION.SDK_INT >= 18) {
            return stat.getAvailableBytes();
        } else {
            return stat.getAvailableBlocks() * stat.getBlockSize();
        }
        //return (long)stat.getBlockSizeLong() * (long)stat.getAvailableBlocksLong();
    }

    public static long directorySize(File target) {
        long sum = 0;
        Iterator<File> fileIterator = FileUtils.iterateFiles(target, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        while(fileIterator.hasNext()) {
            File f = fileIterator.next();
            sum += f.length();
        }
        return sum;
    } 

    public static void debugLog(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.i(tag,msg);
        }
    }

    public static void restartApp(Context context) {
        Log.e("Helpers", "Restarting app");
        Intent restartIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, restartIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);
        restartIntent.putExtra("EXIT", true);
        context.startActivity(restartIntent);
    }

    public static String getNotificationTitle() {
        return "New Internationalist magazine";
    }

    public static void sendPushRegistrationToServer(final String token) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                // Try posting to the server
                URL url = null;
                try {
                    String pushRegistrationsString = "";
                    if (BuildConfig.DEBUG) {
                        // Local debug site
                        pushRegistrationsString = getVariableFromConfig("DEBUG_SITE_URL") + "push_registrations";
                    } else {
                        // Real server
                        pushRegistrationsString = Helpers.getSiteURL() + "push_registrations";
                    }
                    Helpers.debugLog("PushRegistrations", "Sending token: " + token + ", to server: " + pushRegistrationsString);
                    url = new URL(pushRegistrationsString);
                    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                    try {
                        urlConnection.setDoOutput(true);
                        urlConnection.setChunkedStreamingMode(0);
                        urlConnection.setRequestMethod("POST");

                        OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                        Uri.Builder builder = new Uri.Builder()
                                .appendQueryParameter("token", token)
                                .appendQueryParameter("device", "android");
                        String query = builder.build().getEncodedQuery();

                        BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(out, "UTF-8"));
                        writer.write(query);
                        writer.flush();
                        writer.close();

                        Helpers.debugLog("PushRegistrations", "Response from server: " + urlConnection.getResponseCode());

//                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//                        readStream(in);
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                //

            }
        }.execute();
    }

    public static void changeFontSize(Float fontChange, Context context) {
        if (context != null && Build.VERSION.SDK_INT >= 23) {
            Boolean canWriteSettings = Settings.System.canWrite(context);
            if (canWriteSettings) {
                // Get system font scale, check it's between 0.5 and 2.0
                Float currentFontScale = context.getResources().getConfiguration().fontScale;
                Float newFontScale = currentFontScale + fontChange;
                if (newFontScale < 0.5f || newFontScale > 2.0f) {
                    // Don't change it, and use the currentFontScale
                    newFontScale = currentFontScale;
                }
                if (fontChange == 1.0f) {
                    // Reset to the default
                    newFontScale = fontChange;
                }
                Helpers.debugLog("ChangeFontSize", String.format("Changing from %f to %f", currentFontScale, newFontScale));
                Settings.System.putFloat(context.getContentResolver(),
                        Settings.System.FONT_SCALE, newFontScale);
            } else {
                // Request permission
                Intent writeIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                context.startActivity(writeIntent);
            }
        } else if (context != null) {
            // Take the user to font size settings
            Intent settingsIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            context.startActivity(settingsIntent);
        }
    }
}
