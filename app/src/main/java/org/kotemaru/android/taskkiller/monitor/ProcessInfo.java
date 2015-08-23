package org.kotemaru.android.taskkiller.monitor;

import org.kotemaru.android.taskkiller.persistent.Config;

public class ProcessInfo implements ItemInfo {
    public String pid;
    public String commandLine;
    public PackageInfo packageInfo;
    public CpuRateLog cpuRateLog = new CpuRateLog(Config.getMonitorLoggingCount());
    public boolean isAlive = false;

    @Override
    public final String getTitle() {
        if (packageInfo == null) return "";
        return packageInfo.appName;
    }

    @Override
    public final String getSubTitle() {
        return commandLine;
    }

    @Override
    public final String getPackageName() {
        if (packageInfo == null) return null;
        return packageInfo.packageName;
    }

    @Override
    public final CpuRateLog getCpuRateLog() {
        return cpuRateLog;
    }

    @Override
    public void resetCpuRateLog() {
        cpuRateLog = new CpuRateLog(Config.getMonitorLoggingCount());
    }
}

