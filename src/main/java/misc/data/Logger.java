package misc.data;

public class Logger {
    private static final StringBuffer logs = new StringBuffer();

    public static void log(Object message) {
        System.out.println(message);
        logs.append(message);
    }

    public static String getLogs() {
        return logs.toString();
    }
}
