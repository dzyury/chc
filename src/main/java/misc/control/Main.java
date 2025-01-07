package misc.control;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.lgooddatepicker.components.DatePicker;
import lombok.SneakyThrows;
import misc.data.ChcResult;
import misc.data.Schedule;
import misc.data.Week;
import misc.drop.DBClient;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.temporal.ChronoUnit.SECONDS;

public class Main {
    private static DBClient dbClient = new DBClient();
    public static ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static JFrame frame;
    private static JComboBox<String> combo;
    private static JComponent[] labels;
    private static JTextField[] texts;
    private static JTextField[] elaps;

    public static void main(String[] args) throws DbxException {
        var ids = new ArrayList<String>();
        ListFolderResult result = dbClient.listFolder("");
        for (Metadata metadata : result.getEntries()) {
            var path = metadata.getPathLower();
            if (metadata instanceof FolderMetadata) ids.add(path.substring(1));
        }

        frame = new JFrame();
        Container cp =  frame.getContentPane();

        JPanel week = new JPanel();
        SpringLayout layout = new SpringLayout();
        week.setLayout(layout);

        labels = new JComponent[11];
        labels[0] = new JLabel("По умолчанию");
        labels[1] = new JLabel("Понедельник");
        labels[2] = new JLabel("Вторник");
        labels[3] = new JLabel("Среда");
        labels[4] = new JLabel("Четверг");
        labels[5] = new JLabel("Пятница");
        labels[6] = new JLabel("Суббота");
        labels[7] = new JLabel("Воскресенье");
        labels[8] = new DatePicker();
        labels[9] = new DatePicker();
        labels[10] = new DatePicker();

        texts = new JTextField[11];
        for (int i = 0; i < 11; i++) texts[i] = new JTextField("", 6);
        elaps = new JTextField[11];
        for (int i = 0; i < 11; i++) {
            elaps[i] = new JTextField("", 6);
            elaps[i].setEnabled(false);
        }

        for (int i = 0; i < 11; i++) {
            week.add(labels[i]);
            week.add(texts[i]);
            week.add(elaps[i]);
        }

        SpringUtilities.makeCompactGrid(week, 11, 3, 3, 3, 3, 3);
        cp.add(week, BorderLayout.CENTER);

        JComponent north = new JPanel(new BorderLayout());
        combo = new JComboBox<>(ids.toArray(new String[0]));
        combo.addActionListener(event -> download(combo.getSelectedItem().toString()));
        north.add(combo);
        cp.add(north, BorderLayout.NORTH);

        JComponent south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("  OK  ");
        ok.addActionListener(event -> uploadSchedule());
        south.add(ok);
        cp.add(south, BorderLayout.SOUTH);

        download(combo.getSelectedItem().toString());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
//        frame.setSize(160, 100);
        frame.setVisible(true);
    }

    @SneakyThrows
    private static void download(String id) {
        LocalDate now = LocalDate.now();
        LocalDate[] dates = new LocalDate[7];
        for (int i = 0; i < 7; i++) dates[i] = now.minusDays(i);

        for (int i = 0; i < 7; i++) {
            var weekDay = dates[i].getDayOfWeek();
            var result = dbClient.fetch("/" + id + "/" + dates[i] + "result.json", mapper, ChcResult.class);
            var text = result == null ? "" : toText(result.getElapsed());
            elaps[weekDay.getValue()].setText(text);
            System.out.println("result[" + i + "/" + weekDay.getValue() + "]: " + text);
        }

        var schedule = dbClient.fetch("/" + id + "/schedule.json", mapper, Schedule.class);
        if (schedule != null) {
            texts[0].setText(toText(schedule.getEveryDay()));
            for (int i = 1; i <= 7; i++) {
                var text = switch(DayOfWeek.of(i)) {
                    case MONDAY -> schedule.getWeek().getMonday();
                    case TUESDAY -> schedule.getWeek().getTuesday();
                    case WEDNESDAY -> schedule.getWeek().getWednesday();
                    case THURSDAY -> schedule.getWeek().getThursday();
                    case FRIDAY -> schedule.getWeek().getFriday();
                    case SATURDAY -> schedule.getWeek().getSaturday();
                    case SUNDAY -> schedule.getWeek().getSunday();
                };
                texts[i].setText(toText(text));
            }

            var idx = new AtomicInteger(8);
            var calendar = schedule.getCalendar();
            if (calendar != null) {
                calendar.entrySet().stream().sorted().limit(3).forEach(entry -> {
                    var picker = (DatePicker) labels[idx.get()];
                    picker.setDate(entry.getKey());
                    texts[idx.get()].setText(toText(entry.getValue()));
                    idx.incrementAndGet();
                });
            }
            for (int i = idx.get(); i < 11; i++) {
                var picker = (DatePicker) labels[i];
                picker.setText("");
                texts[idx.get()].setText("");
                elaps[idx.get()].setText("");
            }
        }
        System.out.println("schedule: " + schedule);
    }

    private static String toText(Duration duration) {
        return duration == null ? "" : duration.truncatedTo(SECONDS).toString().substring(2);
    }

    @SneakyThrows
    private static void uploadSchedule() {
        Schedule schedule = new Schedule();
        schedule.setEveryDay(toDuration(texts[0]));

        var sWeek = new Week();
        sWeek.setMonday(toDuration(texts[1]));
        sWeek.setTuesday(toDuration(texts[2]));
        sWeek.setWednesday(toDuration(texts[3]));
        sWeek.setThursday(toDuration(texts[4]));
        sWeek.setFriday(toDuration(texts[5]));
        sWeek.setSaturday(toDuration(texts[6]));
        sWeek.setSunday(toDuration(texts[7]));
        schedule.setWeek(sWeek);

        var calendar = new HashMap<LocalDate, Duration>();
        putTo(calendar, labels[8], texts[8]);
        putTo(calendar, labels[9], texts[9]);
        putTo(calendar, labels[10], texts[10]);
        if (!calendar.isEmpty()) schedule.setCalendar(calendar);

        var id = combo.getSelectedItem();
        System.out.println(id + " расписание: " + schedule);

        byte[] bytes = mapper.writeValueAsBytes(schedule);
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            dbClient.upload("/" + id + "/schedule.json", is);
        }
    }

    private static void putTo(HashMap<LocalDate, Duration> calendar, JComponent label, JTextField text) {
        DatePicker datePicker = (DatePicker) label;
        Duration duration = toDuration(text);
        if (datePicker != null && datePicker.getDate() != null && duration != null) {
            calendar.put(datePicker.getDate(), duration);
        }
    }

    private static Duration toDuration(JTextField text) {
        try {
            return Duration.parse("pt" + text.getText());
        } catch (Exception e) {
            return null;
        }
    }
}
