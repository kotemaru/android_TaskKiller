package org.kotemaru.android.taskkiller.receiver;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.kotemaru.android.taskkiller.persistent.Config;
import org.kotemaru.android.taskkiller.service.InstanceKeepService;
import org.kotemaru.android.taskkiller.MyApplication;

import java.util.Map;

public class ScreenOffReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenOffReceiver";
    private static ScreenOffReceiver sInstance = null;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    public static ScreenOffReceiver create(MyApplication context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        sInstance = new ScreenOffReceiver();
        context.registerReceiver(sInstance, filter);
        Log.d(TAG, "register");
        return sInstance;
    }

    public static void destroy(Context context) {
        context.unregisterReceiver(sInstance);
        Log.d(TAG, "unregister");
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive:" + action);
        final MyApplication app = (MyApplication) context.getApplicationContext();

        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Log.d(TAG, "screen off");
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startMain);

            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
                    Map<String, Integer> map = app.getDbMap();
                    for (Map.Entry<String, Integer> item : map.entrySet()) {
                        String pkgName = item.getKey();
                        if (item.getValue() == 1 && pkgName != null) {
                            Log.i(TAG, "kill on sleep:" + pkgName);
                            am.killBackgroundProcesses(pkgName);
                        }
                    }
                }
            }, 1000);
        }
    }
}
