package org.kotemaru.android.taskkiller.monitor;

import org.kotemaru.android.taskkiller.persistent.Config;

public class PackageInfo implements ItemInfo {
    public String appName;
    public String packageName;
    public CpuRateLog cpuRateLog = new CpuRateLog(Config.getMonitorLoggingCount());

    @Override
    public final String getTitle() {
        return appName;
    }

    @Override
    public final String getSubTitle() {
        return packageName;
    }

    @Override
    public final String getPackageName() {
        return packageName;
    }

    @Override
    public CpuRateLog getCpuRateLog() {
        return cpuRateLog;
    }

    @Override
    public void resetCpuRateLog() {
        cpuRateLog = new CpuRateLog(Config.getMonitorLoggingCount());
    }
}
