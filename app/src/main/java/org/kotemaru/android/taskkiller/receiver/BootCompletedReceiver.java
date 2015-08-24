package org.kotemaru.android.taskkiller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.kotemaru.android.taskkiller.service.InstanceKeepService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:" + intent.getAction());
        Intent serviceIntent = new Intent(context, InstanceKeepService.class);
        context.startService(serviceIntent);
    }
}