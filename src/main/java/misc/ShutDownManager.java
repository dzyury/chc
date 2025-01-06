package misc;

import misc.data.Logger;

public class ShutDownManager {
    public void shutdown() {
        try {
            String shutdownCommand;
            String operatingSystem = System.getProperty("os.name");

            if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
                shutdownCommand = "shutdown -h now";
            } else if (operatingSystem.startsWith("Windows")) {
                shutdownCommand = "shutdown.exe -s -t 0";
            } else {
                throw new RuntimeException("Unsupported operating system.");
            }

            Runtime.getRuntime().exec(shutdownCommand);
            System.exit(0);
        } catch (Exception ex) {
            Logger.log("KO");
        }
    }
}
