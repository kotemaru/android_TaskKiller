package org.kotemaru.android.taskkiller.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.kotemaru.android.taskkiller.MyApplication;
import org.kotemaru.android.taskkiller.persistent.Config;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    public static final String ACTION_KILL_PROC = "ACTION_KILL_PROC";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive:" + action);

        if (ACTION_KILL_PROC.equals(action)) {
            ScreenOffReceiver.killProcesses();
            Log.d(TAG, "screen off");
        }
    }

    private static PendingIntent getPendingIntent() {
        Intent intent = new Intent(MyApplication.getInstance(), AlarmReceiver.class);
        intent.setAction(ACTION_KILL_PROC);
        PendingIntent pi = PendingIntent.getBroadcast(MyApplication.getInstance(), 0, intent, 0);
        return pi;
    }

    public static void applyKillRepeat() {
        if (Config.isKillRepeat()) {
            startKillRepeat();
        } else {
            stopKillRepeat();
        }
    }

    public static void startKillRepeat() {
        PendingIntent pi = getPendingIntent();
        AlarmManager am = (AlarmManager) MyApplication.getInstance().getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
    }

    public static void stopKillRepeat() {
        PendingIntent pi = getPendingIntent();
        AlarmManager am = (AlarmManager) MyApplication.getInstance().getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

}
