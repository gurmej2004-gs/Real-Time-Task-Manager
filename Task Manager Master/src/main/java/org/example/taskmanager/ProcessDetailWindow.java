package org.example.taskmanager;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class ProcessDetailWindow {

    private Stage stage;
    private ProcessHandle process;
    private Set<Long> favoritePids;

    public ProcessDetailWindow(ProcessHandle process, Set<Long> favoritePids) {
        this.process = process;
        this.favoritePids = favoritePids;
        this.stage = new Stage();
        stage.setTitle("Process Details");

        // Tabs
        TabPane tabPane = new TabPane();

        // Info Tab
        Tab infoTab = new Tab("Info");
        infoTab.setClosable(false);
        VBox infoBox = createInfoTab();
        infoTab.setContent(infoBox);

        // Stats Tab
        Tab statsTab = new Tab("Stats");
        statsTab.setClosable(false);
        VBox statsBox = createStatsTab();
        statsTab.setContent(statsBox);

        // Logs Tab
        Tab logsTab = new Tab("Logs");
        logsTab.setClosable(false);
        VBox logsBox = createLogsTab();
        logsTab.setContent(logsBox);

        tabPane.getTabs().addAll(infoTab, statsTab, logsTab);

        // Scene and Stage Setup
        BorderPane root = new BorderPane(tabPane);
        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createInfoTab() {
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(10));

        Label pidLabel = new Label("PID: " + process.pid());
        Label commandLabel = new Label("Command: " + process.info().command().orElse("Unknown"));
        Label uptimeLabel = new Label("Uptime: " +
                process.info().startInstant()
                        .map(start -> Duration.between(start, Instant.now()).getSeconds() + " seconds")
                        .orElse("N/A"));

        Button favoriteButton = new Button(favoritePids.contains(process.pid()) ? "Remove from Favorites" : "Add to Favorites");
        favoriteButton.setOnAction(e -> toggleFavorite(favoriteButton));

        infoBox.getChildren().addAll(pidLabel, commandLabel, uptimeLabel, favoriteButton);
        return infoBox;
    }

    private VBox createStatsTab() {
        VBox statsBox = new VBox(10);
        statsBox.setPadding(new Insets(10));

        // CPU & Memory Live Graph
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<Number, Number> cpuChart = new LineChart<>(xAxis, yAxis);
        cpuChart.setAnimated(false);
        cpuChart.setTitle("CPU & Memory Usage");

        XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU Usage");

        XYChart.Series<Number, Number> memSeries = new XYChart.Series<>();
        memSeries.setName("Memory Usage");

        cpuChart.getData().addAll(cpuSeries, memSeries);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            double cpuUsage = SystemMonitor.getCpuLoad() * 100;
            long memUsage = SystemMonitor.getUsedMemory() / (1024 * 1024); // MB
            Platform.runLater(() -> {
                if (cpuSeries.getData().size() > 30) {
                    cpuSeries.getData().remove(0);
                    memSeries.getData().remove(0);
                }
                cpuSeries.getData().add(new XYChart.Data<>(System.currentTimeMillis() / 1000 % 30, cpuUsage));
                memSeries.getData().add(new XYChart.Data<>(System.currentTimeMillis() / 1000 % 30, memUsage));
            });
        }, 0, 2, TimeUnit.SECONDS);

        statsBox.getChildren().add(cpuChart);
        return statsBox;
    }

    private VBox createLogsTab() {
        VBox logsBox = new VBox(10);
        logsBox.setPadding(new Insets(10));

        TextArea logsArea = new TextArea();
        logsArea.setEditable(false);
        logsArea.setPrefHeight(150);

        // Simulating some logs output
        ScheduledExecutorService logScheduler = Executors.newSingleThreadScheduledExecutor();
        logScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                logsArea.appendText("Log entry for PID " + process.pid() + " at " + Instant.now() + "\n");
            });
        }, 0, 5, TimeUnit.SECONDS);

        logsBox.getChildren().add(logsArea);
        return logsBox;
    }

    private void toggleFavorite(Button favoriteButton) {
        if (favoritePids.contains(process.pid())) {
            favoritePids.remove(process.pid());
            favoriteButton.setText("Add to Favorites");
        } else {
            favoritePids.add(process.pid());
            favoriteButton.setText("Remove from Favorites");
        }
    }
}
