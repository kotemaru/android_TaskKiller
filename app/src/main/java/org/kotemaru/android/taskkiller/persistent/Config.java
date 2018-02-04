package org.kotemaru.android.taskkiller.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.kotemaru.android.taskkiller.MyApplication;

public class Config {
    public final static String K_SHOW_SYSTEM_PROC = "showSystemProcess";
    public final static String K_SORT_CONDITION = "sortCondition";
    public final static String K_KILL_REPEAT = "killRepeat";

    public enum SortCondition {
        CPU_LATEST, CPU_AVERAGE, NAME;
    }

    private static SharedPreferences sharedPrefs;

    public static void init(Context context) {

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isShowSystemProcess() {
        SharedPreferences sharedPrefs =  PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return sharedPrefs.getBoolean(K_SHOW_SYSTEM_PROC, true);
    }

    public static SortCondition getSortCondition() {
        SharedPreferences sharedPrefs =  PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        String val = sharedPrefs.getString(K_SORT_CONDITION, SortCondition.CPU_AVERAGE.name());
        try {
            return SortCondition.valueOf(val);
        } catch (IllegalArgumentException e) {
            return SortCondition.CPU_AVERAGE;
        }
    }

    public static boolean isProcessMonitoring() {
        SharedPreferences sharedPrefs =  PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return sharedPrefs.getBoolean("monitoring", false);
    }

    public static int getMonitorInterval() {
        SharedPreferences sharedPrefs =  PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        String val = sharedPrefs.getString("monitorInterval", "10");
        try {
            return Integer.valueOf(val);
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public static int getMonitorLoggingCount() {
        SharedPreferences sharedPrefs =  PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        String val = sharedPrefs.getString("monitorLoggingCount", "20");
        try {
            return Integer.valueOf(val);
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public static boolean isKillRepeat() {
        SharedPreferences sharedPrefs =  PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return sharedPrefs.getBoolean(K_KILL_REPEAT, false);
    }

}
