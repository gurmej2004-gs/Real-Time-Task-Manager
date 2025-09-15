package org.example.taskmanager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WindowsMemoryReader {

    public static long getMemory(long pid) {
        try {
            // Run `tasklist` and filter the output by PID
            Process process = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(String.valueOf(pid))) {
                    // Example output: chrome.exe       1234 Console    1    200,000 K
                    String[] parts = line.trim().split("\\s+");
                    String memStr = parts[parts.length - 2].replace(",", "").replace("K", "");
                    return Long.parseLong(memStr) * 1024; // Convert from KB to Bytes
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to get memory for PID " + pid + ": " + e.getMessage());
        }
        return 0;
    }
}
