package org.kotemaru.android.taskkiller;


import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.kotemaru.android.taskkiller.activity.MainActivity;
import org.kotemaru.android.taskkiller.monitor.ProcessMonitor;
import org.kotemaru.android.taskkiller.persistent.Config;
import org.kotemaru.android.taskkiller.persistent.Database;
import org.kotemaru.android.taskkiller.receiver.AlarmReceiver;
import org.kotemaru.android.taskkiller.receiver.ScreenOffReceiver;

import java.util.Map;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication sInstance;

    private ProcessMonitor mProcessMonitor;
    private MainActivity mMainActivity;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private Database mDatabase;
    private Map<String, Integer> mDbMap;


    public static MyApplication getInstance() {
        return sInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this ;
        Config.init(this);
        mDatabase = new Database(this);
        mDbMap = mDatabase.getMap();

        ScreenOffReceiver.create(this);
        mProcessMonitor = new ProcessMonitor(this);
        mProcessMonitor.refresh(this, true);
        AlarmReceiver.applyKillRepeat();
    }

    public ProcessMonitor getProcessMonitor() {
        return mProcessMonitor;
    }

    public synchronized void setMainActivity(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }

    public synchronized void postRefresh() {
        if (mMainActivity != null) {
            mMainActivity.postRefresh();
        }
    }

    public Map<String, Integer> getDbMap() {
        return mDbMap;
    }

    public boolean isKillOnSleep(String packageName) {
        Integer val = mDbMap.get(packageName);
        boolean b = val == null ? false : (val == 1);
        //Log.d(TAG, "isKillOnSleep:" + packageName + ":" + val + ":" + b);
        return b;
    }

    public void setKillOnSleep(String packageName, boolean b) {
        int killOnSleep = b ? 1 : 0;
        mDbMap.put(packageName, killOnSleep);
        mDatabase.put(packageName, killOnSleep);
        Log.d(TAG, "setKillOnSleep:" + packageName + ":" + killOnSleep + ":" + isKillOnSleep(packageName));
    }
}
