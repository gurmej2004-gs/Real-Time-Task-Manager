package org.example.taskmanager;

import java.lang.ProcessHandle;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessFetcher {
    public static List<ProcessHandle> getProcesses() {
        return ProcessHandle.allProcesses()
                .filter(ProcessHandle::isAlive)
                .collect(Collectors.toList());
    }

    public static void printProcesses() {
        getProcesses().forEach(process -> {
            ProcessHandle.Info info = process.info();
            System.out.println("PID: " + process.pid() + ", Command: " + info.command().orElse("Unknown"));
        });
    }
}
