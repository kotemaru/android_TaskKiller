package org.kotemaru.android.taskkiller.monitor;

public interface ItemInfo {
    public String getTitle();
    public String getSubTitle();
    public String getPackageName();
    public CpuRateLog getCpuRateLog();
    public void resetCpuRateLog();
}
