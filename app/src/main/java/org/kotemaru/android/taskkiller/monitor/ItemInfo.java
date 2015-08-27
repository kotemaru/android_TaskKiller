package org.kotemaru.android.taskkiller.monitor;

import android.content.pm.ApplicationInfo;

public interface ItemInfo {
    public String getTitle();
    public String getSubTitle();
    public String getPackageName();
    public CpuRateLog getCpuRateLog();
    public void resetCpuRateLog();
}
