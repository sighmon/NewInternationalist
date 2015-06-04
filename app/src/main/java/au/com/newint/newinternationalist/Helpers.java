package au.com.newint.newinternationalist;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import au.com.newint.newinternationalist.util.IabHelper;

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

    public static final boolean debugMode = (MainActivity.applicationContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

    public static RoundedBitmapDrawable roundDrawableFromBitmap(Bitmap bitmap) {
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(MainActivity.applicationResources, bitmap);
        roundedBitmapDrawable.setAntiAlias(true);
        roundedBitmapDrawable.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
        return roundedBitmapDrawable;
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

    public static String getDeveloperPayload() {
        // TODO: generate userID string?
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
        return "<html><head><link href='article-body.css' type='text/css' rel='stylesheet'/></head><body>" + htmlToWrap + "</body></html>";
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
}
