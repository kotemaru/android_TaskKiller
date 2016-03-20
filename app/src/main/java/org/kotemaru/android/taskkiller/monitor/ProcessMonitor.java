package org.kotemaru.android.taskkiller.monitor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
    private byte[] mBytes = new byte[4096];
    private ByteBuffer mBuffer = ByteBuffer.wrap(mBytes);
    private long lastTotalCpuTime = 0;

    public ProcessMonitor(Context context) {
        mPackageManager = context.getPackageManager();
        reload(context);
    }

    public ProcessMonitor reload(Context context) {
        mPackageMap.clear();
        List<ApplicationInfo> all = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : all) {
            if (appInfo.enabled) getPackageInfo(appInfo.packageName);
        }
        refresh(context, false);
        return this;
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

    public void refresh(Context context, boolean isMonitoring) {
        long totalCpuTime = getTotalCpuTime();
        long subTotalCpuTime = totalCpuTime - lastTotalCpuTime;
        lastTotalCpuTime = totalCpuTime;

        if (isMonitoring) {
            for (PackageInfo pkgInfo : mPackageMap.values()) {
                pkgInfo.cpuRateLog.shift();
            }
        }

        String files[] = PROC_DIR.list();
        for (String pid : files) {
            char ch = pid.charAt(0);
            if ('0' > ch || ch > '9') continue;
            //String packageName = getCommandLine(pid);
            ProcessInfo info = getProcessInfo(pid);
            if (isMonitoring) {
                long cpuTime = getCpuTime(pid);
                info.cpuRateLog.shift();
                float cpuRate = info.cpuRateLog.pushCpuTime(cpuTime, subTotalCpuTime);
                if (info.packageInfo != null) {
                    info.packageInfo.cpuRateLog.margeCpuRate(cpuRate);
                }
            }
            info.isAlive = true;
        }

        Iterator<Map.Entry<String, ProcessInfo>> ite = mProcessMap.entrySet().iterator();
        while (ite.hasNext()) {
            Map.Entry<String, ProcessInfo> ent = ite.next();
            ProcessInfo info = ent.getValue();
            if (!info.isAlive) {
                //Log.d(TAG, "isDead:" + info.getSubTitle());
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
        int len = readFile("/proc/" + pid + "/cmdline", mBuffer);
        if (len <= 0) return getProcessName(pid);
        String name = new String(mBytes, 0, len);
        return name.trim();
    }

    private String getProcessName(String pid) {
        int len = readFile("/proc/" + pid + "/stat", mBuffer);
        if (len <= 0) return "()";
        int start = skip(mBytes, 0, len);
        int end = skip(mBytes, start, len);
        String name = new String(mBytes, start, end - start);
        return name.trim();
    }

    private long getCpuTime(String pid) {
        int len = readFile("/proc/" + pid + "/stat", mBuffer);
        if (len <= 0) return 0L;
        int off = 0;
        for (int i = 1; i < 13; i++) off = skip(mBytes, off, len);
        long item13 = toLong(mBytes, off, len);
        off = skip(mBytes, off, len);
        long item14 = toLong(mBytes, off, len);
        //Log.d(TAG, "getCpuTime:" + item13 + "," + item14 + "  :" + new String(mBytes, 0, len));
        return item13 + item14;
    }


    private long getTotalCpuTime() {
        int len = readFile("/proc/stat", mBuffer);
        if (len <= 0) return 0L;
        int off = 0;
        long total = 0;
        //Log.d(TAG, "getTotalCpuTime:" + new String(mBytes, 0, len));

        for (int i = 1; i < 11; i++) {
            off = skip(mBytes, off, len);
            long val = toLong(mBytes, off, len);
            //Log.d(TAG, "getTotalCpuTime:" + i + "=" + val);
            total += val;
        }
        return total;
    }

    private int readFile(String fileName, ByteBuffer buffer) {
        try {
            FileInputStream in = new FileInputStream(fileName);
            FileChannel channel = in.getChannel();
            try {
                buffer.clear();
                int n = channel.read(buffer);
                return n;
            } finally {
                channel.close();
            }
        } catch (IOException e) {
            Log.w(TAG, e.toString() + ": " + fileName);
            return -1;
        }
    }

    private final int skip(byte[] buff, int off, int maxLen) {
        for (; off < maxLen; off++) {
            //Log.d(TAG,"skip:1:"+off+":"+buff[off]);
            if (buff[off] == ' ') break;
        }
        for (; off < maxLen; off++) {
            //Log.d(TAG,"skip:2:"+off+":"+buff[off]);
            if (buff[off] != ' ') break;
        }
        return off;
    }

    private final long toLong(byte[] buff, int off, int maxLen) {
        long val = 0;
        for (int i = 0; off < maxLen + i; i++) {
            char ch = (char) buff[off + i];
            if ('0' > ch || ch > '9') return val;
            val = val * 10 + (ch - '0');
        }
        return val;
    }

}
