package org.example.taskmanager;

import java.io.*;

public class MacMemoryReader {

    public static long getMemory(long pid) {
        try {
            Process proc = new ProcessBuilder("ps", "-o", "rss=", "-p", String.valueOf(pid)).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                return Long.parseLong(line.trim()) * 1024; // from KB to bytes
            }
        } catch (IOException | NumberFormatException e) {
            return 0;
        }

        return 0;
    }
}
