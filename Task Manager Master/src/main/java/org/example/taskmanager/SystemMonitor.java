package org.example.taskmanager;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class SystemMonitor {
    private static final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public static double getCpuLoad() {
        // Returns between 0.0 and 1.0 (multiply by 100 for percentage)
        return osBean.getSystemCpuLoad();
    }

    public static long getTotalMemory() {
        return osBean.getTotalPhysicalMemorySize();
    }

    public static long getFreeMemory() {
        return osBean.getFreePhysicalMemorySize();
    }

    public static long getUsedMemory() {
        return getTotalMemory() - getFreeMemory();
    }
}
