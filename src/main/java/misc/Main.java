package misc;

import misc.data.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;

public class Main {
    public static Duration maxTime = Duration.ofHours(1);

    public static Dialog dialog;
    public static File file;
    public static ShutDownManager man = new ShutDownManager();
    public static volatile boolean isDialogVisible = false;
    public static volatile boolean isLocked = false;

    public static LocalDate started = LocalDate.now();
    public static Duration preElapsed = Duration.ofSeconds(0);
    public static Duration elapsed = preElapsed;
    public static Timer timer = new Timer(1000, e -> {
        LocalDate now = LocalDate.now();
        if (!now.equals(started)) {
            started = now;
            preElapsed = Duration.ofSeconds(0);
        }

        if (!isLocked) {
            elapsed = preElapsed.plus(Duration.between(started, now));
            dialog.update(maxTime.minus(elapsed).toSeconds());
            if (elapsed.compareTo(maxTime) > 0) {
                man.shutdown();
            }
        }
    });

    public static void main(String[] args) {
        Logger.log(LocalDateTime.now().truncatedTo(SECONDS));
        try {
            Lock.lock();
            Processes.print();
        } catch (Exception e) {
            Logger.log(e.getMessage());
            return;
        }

        start();
        Logger.log("Started");
        Thread hook = new Thread(() -> {
            try {
                fine(file);
                Lock.release();
            } catch (Exception e) {
                Logger.log("Error writing into file: " + e.getMessage());
            }
            Logger.log("In the middle of a shutdown");
        });
        Runtime.getRuntime().addShutdownHook(hook);
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void fine(File file) {
        isLocked = true;
        try (PrintWriter writer = new PrintWriter(file, UTF_8)) {
            LocalDate now = LocalDate.now();
            if (started.equals(now)) {
                writer.println(now);
                writer.println(elapsed);
            }
        } catch (IOException e) {
            Logger.log("Can't write to file");
        }
    }

    private static void start() {
        isLocked = false;
        started = LocalDate.now();
        try {
            file = new File(HomePath.home, "data");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, UTF_8))) {
                    LocalDate now = LocalDate.now();
                    LocalDate date = LocalDate.parse(reader.readLine());
                    if (date.equals(now)) preElapsed = Duration.parse(reader.readLine());
                }
            }

            File file = new File(HomePath.home, "data.ko");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, UTF_8))) {
                    maxTime = Duration.parse(reader.readLine());
                }
            }
        } catch (Exception e) {
            Logger.log("Can't read file");
        }
        timer.start();
    }

    private static void createAndShowGUI() {
        Desktop desktop = Desktop.getDesktop();
        desktop.addAppEventListener(new UserSessionListener() {
            @Override
            public void userSessionDeactivated(UserSessionEvent aE) {
                Logger.log("out: " + LocalDateTime.now());
                fine(file);
            }

            @Override
            public void userSessionActivated(UserSessionEvent aE) {
                Logger.log("-in: " + LocalDateTime.now());
                start();
            }
        });

        dialog = new Dialog();
        if (!SystemTray.isSupported()) {
            Logger.log("SystemTray is not supported");
            return;
        }
        TrayIcon trayIcon = new TrayIcon(createImage());
        trayIcon.setImageAutoSize(true);
        SystemTray tray = SystemTray.getSystemTray();

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            Logger.log("TrayIcon could not be added.");
            return;
        }

        trayIcon.addActionListener(e -> {
            isDialogVisible = !isDialogVisible;
            dialog.setVisible(isDialogVisible);
            if (isDialogVisible) dialog.setAlwaysOnTop(true);
        });
    }

    public static Image createImage() {
        String path = "/bulb.gif";
        URL imageURL = Main.class.getResource(path);
        return (new ImageIcon(imageURL)).getImage();
    }
}
