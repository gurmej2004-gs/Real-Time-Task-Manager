package org.example.taskmanager;

import java.util.Optional;

public class MemoryUsageFetcher {

    public static Optional<Long> getMemoryUsage(long pid) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                return Optional.of(WindowsMemoryReader.getMemory(pid));
            } else if (os.contains("linux")) {
                return Optional.of(LinuxMemoryReader.getMemory(pid));
            } else if (os.contains("mac")) {
                return Optional.of(MacMemoryReader.getMemory(pid));
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch memory for PID " + pid + ": " + e.getMessage());
        }
        return Optional.empty();
    }
}
