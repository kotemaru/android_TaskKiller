package org.kotemaru.android.taskkiller.monitor;

import java.util.ArrayList;

public class CpuRateLog {
    public final int logSize;
    public float[] log;
    public float lastCpuRate = 0.0F;
    public float avgCpuRate = 0.0F;
    private long lastAccumulateCpuTime = -1;
    public int availCount=0;

    public CpuRateLog(int size) {
        logSize = size;
        log = new float[logSize];
        for (int i = 0; i < logSize; i++) log[i] = 0.0F;
    }

    public void shift() {
        System.arraycopy(log, 1, log, 0, logSize - 1);
        log[logSize-1] = 0.0F;
        if (availCount<logSize) availCount++;
    }

    public float pushCpuTime(long accumulateProcessCpuTime, long systemTotalCpuTime) {
        if (lastAccumulateCpuTime != -1) {
            long cpuTime = accumulateProcessCpuTime - lastAccumulateCpuTime;
            lastCpuRate = (float) cpuTime / (float) systemTotalCpuTime;
            //Log.d(TAG,"pushCpuTime:"+lastCpuRate+":"+cpuTime+":"+systemTotalCpuTime);
            log[logSize - 1] = lastCpuRate;
            avgCpuRate = avg();
        }
        lastAccumulateCpuTime = accumulateProcessCpuTime;
        return lastCpuRate;
    }

    public void margeCpuRate(float cpuRate) {
        lastCpuRate = log[logSize - 1] + cpuRate;
        log[logSize - 1] = lastCpuRate;
        avgCpuRate = avg();
    }

    private float avg() {
        float t = 0;
        for (float v : log) t += v;
        return t / availCount;
    }
}
