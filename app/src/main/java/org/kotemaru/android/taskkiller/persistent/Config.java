package org.kotemaru.android.taskkiller.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Config  {
    public final static String K_SHOW_SYSTEM_PROC = "showSystemProcess";
    public final static String K_SORT_CONDITION = "sortCondition";

    public enum SortCondition {
        CPU_LATEST, CPU_AVERAGE, NAME;
    }

    private static SharedPreferences sSharedPrefs;

    public static void init(Context context) {
        sSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isShowSystemProcess() {
        return sSharedPrefs.getBoolean(K_SHOW_SYSTEM_PROC, true);
    }

    public static SortCondition getSortCondition() {
        String val = sSharedPrefs.getString(K_SORT_CONDITION, SortCondition.CPU_LATEST.name());
        try {
            return SortCondition.valueOf(val);
        } catch (IllegalArgumentException e) {
            return SortCondition.CPU_LATEST;
        }
    }
    public static boolean isProcessMonitoring() {
        return sSharedPrefs.getBoolean("monitoring", true);
    }

    public static int getMonitorInterval() {
        String val = sSharedPrefs.getString("monitorInterval", "5");
        try {
            return Integer.valueOf(val);
        } catch(NumberFormatException e) {
            return 5;
        }
    }
    public static int getMonitorLoggingCount() {
        String val = sSharedPrefs.getString("monitorLoggingCount", "20");
        try {
            return Integer.valueOf(val);
        } catch(NumberFormatException e) {
            return 5;
        }
    }


}
