package org.example.taskmanager;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CpuUsageTracker {

    private final Map<Long, Long> lastCpuTime = new HashMap<>();
    private final Map<Long, Instant> lastTimestamp = new HashMap<>();
    private final int cores = Runtime.getRuntime().availableProcessors();

    public double getCpuUsage(ProcessHandle process) {
        long pid = process.pid();

        Duration totalCpu = process.info().totalCpuDuration().orElse(Duration.ZERO);
        long currentCpuTime = totalCpu.toMillis();
        Instant now = Instant.now();

        long previousCpu = lastCpuTime.getOrDefault(pid, 0L);
        Instant previousTimestamp = lastTimestamp.getOrDefault(pid, now);

        long cpuDelta = currentCpuTime - previousCpu;
        long timeDelta = Duration.between(previousTimestamp, now).toMillis();

        lastCpuTime.put(pid, currentCpuTime);
        lastTimestamp.put(pid, now);

        if (timeDelta == 0) return 0.0;

        return (cpuDelta / (double) timeDelta) * 100 / cores;
    }
}
