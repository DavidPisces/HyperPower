package com.reiraku.hyperpower.privileged;

interface IPrivilegedMonitor {
    String readCpuStat();
    String readCpuInfo();
    int readCoreCount();
    long[] readCoreFrequencies(int coreCount);
    String[] readCoreMidrs(int coreCount);
    long[] readCoreCapacities(int coreCount);
    long[] readCoreMaxFrequencies(int coreCount);
}
