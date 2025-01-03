package misc;

import java.util.Optional;
import java.util.TreeMap;

public class Processes {
    public static void print() {
        var map = new TreeMap<String, String>();
        ProcessHandle.allProcesses().forEach(process -> {
            process.info().command().ifPresent(cmd -> {
                map.putIfAbsent(cmd, getDetails(process));
            });
        });

        map.forEach((key, val) -> {
            if (key.contains("PowerToys")) return;
            if (key.contains("Microsoft OneDrive")) return;
            if (key.contains("WindowsApps")) return;
            if (key.contains("Windows\\System32")) return;
            if (key.contains("Huawei\\PCManager")) return;
            if (key.contains("\\Telegram.exe")) return;
            if (key.contains("\\Far.exe")) return;
            if (key.contains("C:\\Windows")) return;
            System.out.println(val);
        });
    }

    private static String getDetails(ProcessHandle process) {
        return String.format("%8d %10s %26s %-40s",
                process.pid(),
                text(process.info().user()),
                text(process.info().startInstant()),
                text(process.info().command())
        );
    }

    private static String text(Optional<?> optional) {
        return optional.map(Object::toString).orElse("-");
    }
}
