package au.com.newint.newinternationalist.util;

import android.content.Context;
import android.content.Intent;

import com.parse.ParsePushBroadcastReceiver;

import au.com.newint.newinternationalist.MainActivity;

/**
 * Created by New Internationalist on 4/06/15.
 */
public class PushNotificationReceiver extends ParsePushBroadcastReceiver {

    // Handle Parse push notifications

    @Override
    public void onPushOpen(Context context, Intent intent) {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtras(intent.getExtras());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}