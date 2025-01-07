package misc;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import misc.data.ChcResult;
import misc.data.Logger;
import misc.data.Schedule;
import misc.drop.DBClient;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static java.time.temporal.ChronoUnit.SECONDS;

public class Main {
    private static final String RESULT = "result.json";
    private static final String SCHEDULE = "schedule.json";
    public static final int PERIOD = 60_000; // 1 min
    public static final Duration MAX_TIME = Duration.ofMinutes(10);
    public static Duration maxTime = MAX_TIME;

    public static String id;
    public static Dialog dialog;
    public static ShutDownManager man = new ShutDownManager();
    public static volatile boolean isDialogVisible = false;
    public static volatile boolean isLocked = false;

    public static DBClient dbClient = new DBClient();
    public static LocalDateTime started = LocalDateTime.now();
    public static Duration preElapsed = Duration.ZERO;
    public static Duration elapsed = Duration.ZERO;
    public static Duration skipped = Duration.ZERO;
    public static ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static Timer timer = new Timer(1000, e -> {
        LocalDateTime now = LocalDateTime.now();
        if (!now.toLocalDate().equals(started.toLocalDate())) {
            started = now;
            skipped = Duration.ZERO;
            preElapsed = Duration.ZERO;
        }

        if (isLocked) skipped = skipped.plus(Duration.ofSeconds(1)); else {
            elapsed = preElapsed.plus(Duration.between(started, now)).minus(skipped);
            dialog.update(maxTime.minus(elapsed).toSeconds());
            if (elapsed.compareTo(maxTime) > 0) {
                man.shutdown();
            }
        }
    });

    public static void main(String[] args) throws Exception {
        if (args.length != 1) return;

        id = args[0];
        Logger.log(LocalDateTime.now().truncatedTo(SECONDS));
        try {
            Lock.lock();
            Processes.print();
        } catch (Exception e) {
            Logger.log(Arrays.deepToString(e.getStackTrace()));
            return;
        }

        Logger.log("Started");
        Thread hook = new Thread(() -> {
            try {
                upload(dbClient, mapper, true);
                Lock.release();
            } catch (Exception e) {
                Logger.log("Error writing into file: " + e.getMessage());
            }
            Logger.log("In the middle of a shutdown");
        });
        Runtime.getRuntime().addShutdownHook(hook);
        SwingUtilities.invokeLater(Main::createAndShowGUI);
        start();
    }

    private static void start() throws Exception {
        isLocked = false;
        timer.start();

        var schedule = getLocalSchedule();
        var chcResult = getLocalResult();

        var isInit = true;
        ListFolderResult result = dbClient.listFolder("/" + id);
        while (true) try {
            LocalDate now = LocalDate.now();
            for (Metadata metadata : result.getEntries()) {
                var path = metadata.getPathLower();
//                Logger.log("path: " + path);
                if (metadata instanceof FolderMetadata) continue;

                if (path.endsWith(SCHEDULE)) schedule = fetch(path, dbClient, mapper, Schedule.class);
                if (isInit && path.endsWith(now + RESULT)) chcResult = fetch(path, dbClient, mapper, ChcResult.class);
            }
            if (isInit) {
                preElapsed = chcResult != null && now.equals(chcResult.getDate()) ? chcResult.getElapsed() : Duration.ZERO;
            }
            maxTime = getMaxTime(schedule);
            isInit = false;

            if (!result.getHasMore()) upload(dbClient, mapper, false);
            result = dbClient.listFolderContinue(result.getCursor());
        } catch (Exception e) {
            Logger.log(Arrays.deepToString(e.getStackTrace()));
        }
    }

    private static Duration getMaxTime(Schedule schedule) {
        LocalDate now = LocalDate.now();
        if (schedule == null) return MAX_TIME;

        var calendar = schedule.getCalendar();
        var dateMaxTime = calendar == null ? null : calendar.get(now);
        if (dateMaxTime != null) return dateMaxTime;

        var week = schedule.getWeek();
        var weekMaxTime = switch (now.getDayOfWeek()) {
            case MONDAY -> week.getMonday();
            case TUESDAY -> week.getTuesday();
            case WEDNESDAY -> week.getWednesday();
            case THURSDAY -> week.getTuesday();
            case FRIDAY -> week.getFriday();
            case SATURDAY -> week.getSaturday();
            case SUNDAY -> week.getSunday();
        };
        if (weekMaxTime != null) return weekMaxTime;
        return schedule.getEveryDay();
    }

    private static ChcResult getLocalResult() throws IOException {
        var file = new File(HomePath.home, "result.json");
        if (file.exists()) {
            return mapper.readValue(file, ChcResult.class);
        }
        return null;
    }

    private static Schedule getLocalSchedule() throws IOException {
        var file = new File(HomePath.home, "schedule.json");
        if (file.exists()) {
            return mapper.readValue(file, Schedule.class);
        }
        return null;
    }

    private static void upload(DBClient dbClient, ObjectMapper mapper, boolean isForced) throws Exception {
        if (!isForced) {
            var rem = (PERIOD - System.currentTimeMillis() % PERIOD) % PERIOD;
            System.out.printf("rem: %.2fm\n", rem/1000/60.0);
            Thread.sleep(rem);
        }
        if (isLocked) return;

        var now = LocalDateTime.now();
        var elapsed = Duration.between(started, now);
        ChcResult result = new ChcResult(now, preElapsed.plus(elapsed.minus(skipped)), maxTime);
        byte[] bytes = mapper.writeValueAsBytes(result);

        try (OutputStream os = new FileOutputStream(HomePath.home + "/result.json")) {
            os.write(bytes);
        } catch (IOException e) {
            Logger.log("Can't write to file");
        }

        try (InputStream is = new ByteArrayInputStream(bytes)) {
            dbClient.upload("/" + id + "/" + now.toLocalDate() + "result.json", is);
        }
    }

    private static <T> T fetch(String path, DBClient dbClient, ObjectMapper mapper, Class<T> clazz) throws IOException, DbxException {
        try (
                DbxDownloader<FileMetadata> content = dbClient.download(path);
                InputStream is = content.getInputStream()
        ) {
            return mapper.readValue(is, clazz);
        }
    }

    private static void createAndShowGUI() {
        Desktop desktop = Desktop.getDesktop();
        desktop.addAppEventListener(new UserSessionListener() {
            @Override
            public void userSessionDeactivated(UserSessionEvent aE) {
                isLocked = true;
                Logger.log("out: " + LocalDateTime.now());
            }

            @Override
            public void userSessionActivated(UserSessionEvent aE) {
                isLocked = false;
                Logger.log("-in: " + LocalDateTime.now());
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
