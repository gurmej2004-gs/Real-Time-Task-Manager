package org.example.taskmanager;

import java.io.*;

public class LinuxMemoryReader {

    public static long getMemory(long pid) {
        File statusFile = new File("/proc/" + pid + "/status");

        try (BufferedReader reader = new BufferedReader(new FileReader(statusFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("VmRSS:")) {
                    String[] parts = line.split("\\s+");
                    return Long.parseLong(parts[1]) * 1024; // from kB to bytes
                }
            }
        } catch (IOException e) {
            return 0;
        }

        return 0;
    }
}
