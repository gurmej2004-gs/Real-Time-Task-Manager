package org.example.taskmanager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class TrayIconManager {

    private TrayIcon trayIcon;

    public void setupTray() {
        if (!SystemTray.isSupported()) return;

        try {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            trayIcon = new TrayIcon(image);
            trayIcon.setToolTip("System Monitor");

            final SystemTray tray = SystemTray.getSystemTray();
            tray.add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTray(double cpu, double memRatio) {
        if (trayIcon == null) return;

        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // CPU usage bar (green)
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, (int) (cpu * 0.16), 8);

        // Memory usage bar (blue)
        g.setColor(Color.BLUE);
        g.fillRect(0, 8, (int) (memRatio * 16), 8);

        g.dispose();
        trayIcon.setImage(image);
        trayIcon.setToolTip(String.format("CPU: %.1f%%  |  Mem: %.1f%%", cpu, memRatio * 100));
    }
}
