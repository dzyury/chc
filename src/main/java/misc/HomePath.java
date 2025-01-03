package misc;

import java.io.File;

public class HomePath {
    public static final File home = create();

    private static File create() {
        String home = System.getProperty("user.home");
        File dir = new File(home, "/.ch");
        if (!dir.exists()) dir.mkdir();
        return dir;
    }
}
