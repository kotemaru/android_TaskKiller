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

import org.kotemaru.android.taskkiller.MyApplication;
import org.kotemaru.android.taskkiller.persistent.Config;
import org.kotemaru.android.taskkiller.service.InstanceKeepService;

import java.util.Map;

public class MonitorReceiver extends BroadcastReceiver {
    private static final String TAG = "MonitorReceiver";
    private static MonitorReceiver sInstance = null;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private static final String ACTION_MONITOR = "ACTION_MONITOR";

    public static MonitorReceiver create(MyApplication context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        sInstance = new MonitorReceiver();
        context.registerReceiver(sInstance, filter);
        if (Config.isProcessMonitoring()) startMonitor(context);
        Log.d(TAG, "register");
        return sInstance;
    }

    public static void destroy(Context context) {
        stopMonitor(context);
        context.unregisterReceiver(sInstance);
        Log.d(TAG, "unregister");
    }

    public static void ctrlMonitor(Context context, boolean isStart) {
        Intent intent = new Intent(ACTION_MONITOR);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isStart) {
            long interval = Config.getMonitorInterval() * 60000; // min
            Log.d(TAG, "startMonitor():interval=" + interval);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        } else {
            Log.d(TAG, "stopMonitor()");
            alarmManager.cancel(pendingIntent);
        }
        updateNotification(context);
    }

    public static void startMonitor(Context context) {
        ctrlMonitor(context, true);
    }

    public static void stopMonitor(Context context) {
        ctrlMonitor(context, false);
    }

    private static void updateNotification(Context context) {
        Intent intent = new Intent(context, InstanceKeepService.class);
        context.startService(intent);
    }


    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive:" + action);
        if (ACTION_MONITOR.equals(action)) {
            final MyApplication app = (MyApplication) context.getApplicationContext();
            app.getProcessMonitor().refresh(context);
        }
    }
}
