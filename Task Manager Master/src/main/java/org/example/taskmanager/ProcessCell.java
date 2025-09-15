// File: ProcessCell.java
package org.example.taskmanager;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Set;

public class ProcessCell extends ListCell<ProcessHandle> {
    private final CpuUsageTracker cpuUsageTracker;
    private final Set<Long> favoritePids;

    public ProcessCell(CpuUsageTracker tracker, Set<Long> favorites) {
        this.cpuUsageTracker = tracker;
        this.favoritePids = favorites;
    }

    @Override
    protected void updateItem(ProcessHandle process, boolean empty) {
        super.updateItem(process, empty);
        if (empty || process == null) {
            setGraphic(null);
            setText(null);
        } else {
            VBox box = new VBox();
            box.setPadding(new Insets(5));
            box.setSpacing(2);

            String cmd = process.info().command().orElse("Unknown");
            long pid = process.pid();
            long mem = MemoryUsageFetcher.getMemoryUsage(pid).orElse(0L) / (1024 * 1024);
            double usage = cpuUsageTracker.getCpuUsage(process);

            Label title = new Label("PID: " + pid + " — " + cmd);
            ProgressBar progressBar = new ProgressBar(Math.min(usage / 100.0, 1.0));
            Label usageLabel = new Label(String.format("CPU: %.1f%%  |  Mem: %d MB", usage, mem));

            box.getChildren().addAll(title, progressBar, usageLabel);
            setGraphic(box);

            setOnMouseClicked(event -> {
                new ProcessDetailWindow(process, favoritePids);
            });
        }
    }
}
