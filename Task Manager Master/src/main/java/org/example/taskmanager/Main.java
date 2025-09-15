package org.example.taskmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Main extends Application {

    private Set<Long> favoritePids = new HashSet<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Instant bootTime = Instant.now();

    @Override
    public void start(Stage primaryStage) {
        CpuUsageTracker cpuUsageTracker = new CpuUsageTracker();
        TrayIconManager trayManager = new TrayIconManager();
        trayManager.setupTray();

        // --- CPU & Memory Stats Cards ---
        Label cpuLabel = new Label("CPU Load: 0%");
        Label memoryLabel = new Label("Memory: 0 MB / 0 MB");
        Label diskLabel = new Label("Disk: 0 GB / 0 GB");
        Label netLabel = new Label("Network: -- KB/s");
        Label procLabel = new Label("Processes: -");
        Label uptimeLabel = new Label("Uptime: 0s");

        VBox cpuCard = createStatCard("🖥️ CPU", cpuLabel);
        VBox memCard = createStatCard("🧠 Memory", memoryLabel);
        VBox diskCard = createStatCard("💽 Disk", diskLabel);
        VBox netCard = createStatCard("🌐 Network", netLabel);
        VBox procCard = createStatCard("📈 Processes", procLabel);
        VBox uptimeCard = createStatCard("⏱️ Uptime", uptimeLabel);

        HBox cardContainer = new HBox(10, cpuCard, memCard, diskCard, netCard, procCard, uptimeCard);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(10));

        // --- CPU Chart ---
        final int WINDOW_SIZE = 30;
        XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
        NumberAxis xAxis = new NumberAxis(0, WINDOW_SIZE, 1);
        xAxis.setLabel("Time (s)");
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("CPU %");
        LineChart<Number, Number> cpuChart = new LineChart<>(xAxis, yAxis);
        cpuChart.setAnimated(false);
        cpuChart.setLegendVisible(false);
        cpuChart.setTitle("CPU Usage Over Time");
        cpuChart.setCreateSymbols(true);
        cpuChart.setPrefHeight(250);
        cpuChart.getData().add(cpuSeries);

        // --- Memory Pie Chart ---
        PieChart memoryPie = new PieChart();
        memoryPie.setTitle("Memory Usage");
        memoryPie.setLabelsVisible(true);

        // --- Refresh Interval Control ---
        ComboBox<Integer> intervalDropdown = new ComboBox<>();
        intervalDropdown.getItems().addAll(1, 2, 5, 10);
        intervalDropdown.setValue(2);
        intervalDropdown.setPromptText("Refresh Interval (s)");

        // --- Graph Control Buttons ---
        CheckBox darkModeToggle = new CheckBox("Dark Mode");
        CheckBox smoothToggle = new CheckBox("Smooth Graph");
        Button clearBtn = new Button("🧼 Clear History");
        HBox controls = new HBox(10, new Label("Update every:"), intervalDropdown, smoothToggle, clearBtn, darkModeToggle);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox topSection = new VBox(10, cardContainer, controls, cpuChart, memoryPie);
        topSection.setPadding(new Insets(10));
        topSection.setAlignment(Pos.TOP_CENTER);

        // --- Processes Tab ---
        TextField searchField = new TextField();
        searchField.setPromptText("Search by PID or Command");
        ObservableList<ProcessHandle> processes = FXCollections.observableArrayList(ProcessFetcher.getProcesses());
        FilteredList<ProcessHandle> filteredList = new FilteredList<>(processes, p -> true);
        ListView<ProcessHandle> listView = new ListView<>(filteredList);
        listView.setCellFactory(list -> new ProcessCell(cpuUsageTracker, favoritePids));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(proc -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                String command = proc.info().command().orElse("").toLowerCase();
                return command.contains(lower) || String.valueOf(proc.pid()).contains(lower);
            });
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> updateProcessListAsync(filteredList));

        Button killBtn = new Button("Terminate Process");
        killBtn.setOnAction(e -> {
            ProcessHandle selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean success = selected.destroy();
                String msg = success ? "Process terminated successfully." : "Failed to terminate. Forcing termination...";
                Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
                alert.showAndWait();
                if (!success) selected.destroyForcibly();
                updateProcessListAsync(filteredList);
            }
        });

        Button exportBtn = new Button("Export CSV");
        exportBtn.setOnAction(e -> exportToCSV(filteredList));

        HBox bottomBar = new HBox(10, refreshBtn, killBtn, exportBtn);
        bottomBar.setPadding(new Insets(10));
        bottomBar.setAlignment(Pos.CENTER);

        VBox processTabLayout = new VBox(10, searchField, listView, bottomBar);
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                new Tab("Overview", topSection),
                new Tab("Processes", processTabLayout)
        );
        tabPane.getTabs().forEach(t -> t.setClosable(false));

        BorderPane root = new BorderPane(tabPane);
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("Real-Time Task Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        darkModeToggle.selectedProperty().addListener((obs, oldVal, isDark) -> {
            if (isDark) scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            else scene.getStylesheets().clear();
        });

        AtomicReference<ScheduledFuture<?>> scheduledTask = new AtomicReference<>();

        Runnable updateTask = () -> {
            double cpu = SystemMonitor.getCpuLoad() * 100;
            long usedMem = SystemMonitor.getUsedMemory() / (1024 * 1024);
            long totalMem = SystemMonitor.getTotalMemory() / (1024 * 1024);
            double memRatio = (double) usedMem / totalMem;

            trayManager.updateTray(cpu, memRatio);

            File rootDrive = new File("/");
            long totalSpace = rootDrive.getTotalSpace();
            long usableSpace = rootDrive.getUsableSpace();
            String diskUsage = String.format("Disk: %.2f GB / %.2f GB",
                    (totalSpace - usableSpace) / 1e9,
                    totalSpace / 1e9);

            Platform.runLater(() -> {
                cpuLabel.setText(String.format("CPU Load: %.2f%%", cpu));
                memoryLabel.setText(String.format("Memory: %d MB / %d MB", usedMem, totalMem));
                diskLabel.setText(diskUsage);
                uptimeLabel.setText("Uptime: " + Duration.between(bootTime, Instant.now()).getSeconds() + "s");
                procLabel.setText("Processes: " + ProcessHandle.allProcesses().count());

                memoryPie.setData(FXCollections.observableArrayList(
                        new PieChart.Data("Used", usedMem),
                        new PieChart.Data("Free", totalMem - usedMem)
                ));

                long time = System.currentTimeMillis() / 1000 % WINDOW_SIZE;
                XYChart.Data<Number, Number> point = new XYChart.Data<>(time, cpu);

                if (cpuSeries.getData().size() > WINDOW_SIZE) {
                    cpuSeries.getData().remove();
                }
                cpuSeries.getData().add(point);

                long simulatedNetSpeed = (long) (Math.random() * 1000);
                netLabel.setText("Network: " + simulatedNetSpeed + " KB/s");

                listView.refresh();
            });
        };

        scheduledTask.set(scheduler.scheduleAtFixedRate(updateTask, 0, intervalDropdown.getValue(), TimeUnit.SECONDS));
        intervalDropdown.setOnAction(e -> {
            if (scheduledTask.get() != null) scheduledTask.get().cancel(false);
            scheduledTask.set(scheduler.scheduleAtFixedRate(updateTask, 0, intervalDropdown.getValue(), TimeUnit.SECONDS));
        });

        clearBtn.setOnAction(e -> cpuSeries.getData().clear());
    }

    private VBox createStatCard(String title, Label content) {
        Label titleLabel = new Label(title);
        VBox box = new VBox(5, titleLabel, content);
        box.setPadding(new Insets(10));
        box.setPrefWidth(140);
        box.getStyleClass().add("stat-card");
        return box;
    }

    private void updateProcessListAsync(FilteredList<ProcessHandle> filteredList) {
        executor.submit(() -> {
            ObservableList<ProcessHandle> refreshedList = FXCollections.observableArrayList(ProcessFetcher.getProcesses());
            Platform.runLater(() -> filteredList.setAll(refreshedList));
        });
    }

    private void exportToCSV(ObservableList<ProcessHandle> processes) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Process List");
        fileChooser.setInitialFileName("processes.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("PID,Command,Uptime (s),CPU Time (s)");
                for (ProcessHandle process : processes) {
                    long pid = process.pid();
                    String cmd = process.info().command().orElse("Unknown");
                    long uptime = process.info().startInstant()
                            .map(start -> Duration.between(start, Instant.now()).getSeconds())
                            .orElse(0L);
                    long cpuTime = process.info().totalCpuDuration().map(Duration::getSeconds).orElse(0L);
                    writer.printf("%d,%s,%d,%d%n", pid, cmd, uptime, cpuTime);
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Error saving CSV: " + e.getMessage()).showAndWait();
            }
        }
    }

    @Override
    public void stop() throws Exception {
        executor.shutdownNow();
        scheduler.shutdownNow();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
