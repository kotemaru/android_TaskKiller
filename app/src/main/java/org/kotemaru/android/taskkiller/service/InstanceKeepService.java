package org.kotemaru.android.taskkiller.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Notification;
import android.content.Intent;
import android.util.Log;

import org.kotemaru.android.taskkiller.persistent.Config;
import org.kotemaru.android.taskkiller.R;
import org.kotemaru.android.taskkiller.activity.MainActivity;

public class InstanceKeepService extends IntentService {
    private static final String TAG = InstanceKeepService.class.getSimpleName();

    public InstanceKeepService() {
        super(InstanceKeepService.class.getCanonicalName());
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
    }

    public void startForeground() {
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.icon_black);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setWhen(System.currentTimeMillis());
        builder.setContentText(Config.isProcessMonitoring()
                ? "Process monitored (interval="+Config.getMonitorInterval()+"min)"
                : "");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, -1, intent, 0);
        builder.setContentIntent(pi);
        startForeground(1, builder.build());
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent == null ? null : intent.getAction();
        Log.d(TAG, "InstanceKeepService.onHandleIntent:" + action);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        Log.d(TAG, "InstanceKeepService.onStartCommand:" + action);
        startForeground();
        return START_STICKY;
    }
}