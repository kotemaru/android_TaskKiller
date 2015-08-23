package org.kotemaru.android.taskkiller.monitor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.kotemaru.android.taskkiller.persistent.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class ProcessMonitor {
    private static final String TAG = "ProcessMonitor";

    private static final File PROC_DIR = new File("/proc");
    private PackageManager mPackageManager;
    private HashMap<String, PackageInfo> mPackageMap = new HashMap<String, PackageInfo>();
    private HashMap<String, ProcessInfo> mProcessMap = new HashMap<String, ProcessInfo>();
    private byte[] mBuff = new byte[4096];
    private long lastTotalCpuTime = 0;

    public ProcessMonitor(Context context) {
        mPackageManager = context.getPackageManager();
        List<ApplicationInfo> all = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : all) {
            getPackageInfo(appInfo.packageName);
        }
    }

    public void getProcessInfoList(List<ItemInfo> result, boolean isShowSystem) {
        result.clear();
        for (ProcessInfo info : mProcessMap.values()) {
            if (isShowSystem || info.packageInfo != null) result.add(info);
        }
    }

    public void getPackageInfoList(List<ItemInfo> result) {
        result.clear();
        for (PackageInfo info : mPackageMap.values()) {
            result.add(info);
        }
    }

    public void resetCpuLog() {
        for (ProcessInfo info : mProcessMap.values()) {
            info.resetCpuRateLog();
        }
        for (PackageInfo info : mPackageMap.values()) {
            info.resetCpuRateLog();
        }
    }

    public void refresh(Context context) {
        long currentTime = System.currentTimeMillis();
        long totalCpuTime = getTotalCpuTime();
        long subTotalCpuTime = totalCpuTime - lastTotalCpuTime;
        lastTotalCpuTime = totalCpuTime;

        for (PackageInfo pkgInfo : mPackageMap.values()) {
            pkgInfo.cpuRateLog.shift();
        }

        String files[] = PROC_DIR.list();
        for (String pid : files) {
            char ch = pid.charAt(0);
            if ('0' > ch || ch > '9') continue;
            //String packageName = getCommandLine(pid);
            long cpuTime = getCpuTime(pid);
            ProcessInfo info = getProcessInfo(pid);
            info.cpuRateLog.shift();
            float cpuRate = info.cpuRateLog.pushCpuTime(cpuTime, subTotalCpuTime);
            if (info.packageInfo != null) {
                info.packageInfo.cpuRateLog.margeCpuRate(cpuRate);
            }
            info.isAlive = true;
        }

        Iterator<Map.Entry<String, ProcessInfo>> ite = mProcessMap.entrySet().iterator();
        while (ite.hasNext()) {
            Map.Entry<String, ProcessInfo> ent = ite.next();
            ProcessInfo info = ent.getValue();
            if (!info.isAlive) {
                Log.d(TAG, "isDead:" + info.getSubTitle());
                ite.remove();
            }
            info.isAlive = false;
        }

        //MyApplication app = (MyApplication) context.getApplicationContext();
        //app.postRefresh();
    }

    private ProcessInfo getProcessInfo(String pid) {
        //Log.d(TAG, "getProcessInfo:" + pid);
        ProcessInfo info = mProcessMap.get(pid);
        if (info != null) return info;

        String commandLine = getCommandLine(pid);
        info = new ProcessInfo();
        info.pid = pid;
        info.commandLine = commandLine;
        if (commandLine.charAt(0) != '/' && commandLine.charAt(0) != '(') {
            int idx = commandLine.indexOf(':');
            String packageName = (idx >= 0) ? commandLine.substring(0, idx) : commandLine;
            info.packageInfo = getPackageInfo(packageName);
        }
        mProcessMap.put(pid, info);
        return info;
    }

    private PackageInfo getPackageInfo(String packageName) {
        Log.d(TAG, "getPackageInfo:" + packageName);
        PackageInfo info = mPackageMap.get(packageName);
        if (info != null) return info;

        try {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            info = new PackageInfo();
            info.packageName = packageName;
            info.appName = appInfo.loadLabel(mPackageManager).toString();
            mPackageMap.put(packageName, info);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e.toString() + ": " + packageName);
        }
        return info;
    }

    private String getCommandLine(String pid) {
        String name = readFile("/proc/" + pid + "/cmdline");
        if (name == null || name.isEmpty()) {
            return getProcessName(pid);
        }
        return name.trim();
    }

    private long getCpuTime(String pid) {
        String stat = readFile("/proc/" + pid + "/stat");
        if (stat == null) return 0L;
        String[] data = stat.split("\\s+");
        //Log.d(TAG,"getCpuTime:"+stat);
        return Long.parseLong(data[13]) + Long.parseLong(data[14]);
    }

    private String getProcessName(String pid) {
        String stat = readFile("/proc/" + pid + "/stat");
        if (stat == null) return "()";
        String[] data = stat.split("\\s+");
        //Log.d(TAG,"c:"+stat);
        return data[1];
    }


    private long getTotalCpuTime() {
        String stat = readFile("/proc/stat");
        //Log.d(TAG,"getTotalCpuTime:"+stat);
        String[] data = stat.split("\\s+");
        long total = 0;
        for (int i = 1; i < 11; i++) {
            total += Long.parseLong(data[i]);
        }
        return total;
    }

    private String readFile(String fileName) {
        try {
            InputStream in = new FileInputStream(fileName);
            try {
                int n;
                int offset = 0;
                int remain = mBuff.length;
                while ((n = in.read(mBuff, offset, remain)) != -1 && remain > 0) {
                    offset += n;
                    remain -= n;
                }
                return new String(mBuff, 0, offset, "utf8");
            } finally {
                in.close();
            }
        } catch (IOException e) {
            Log.w(TAG, e.toString() + ": " + fileName);
            return null;
        }
    }

}
