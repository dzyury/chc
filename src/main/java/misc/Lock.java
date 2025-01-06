package misc;

import misc.data.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class Lock {
    private static FileLock lock;
    private static RandomAccessFile raf;
    private static final File lockFile = new File(HomePath.home, "data.lock");

    public static void lock() {
        try {
            raf = new RandomAccessFile(lockFile, "rw");
            FileChannel channel = raf.getChannel();

            lock = channel.tryLock();
            if (lock == null) {
                Logger.log("Lock is not acquired");
                throw new IllegalArgumentException("Can't lock file: " + lockFile);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't find lock file: " + lockFile);
        }
    }

    public static void release() {
        if (lock != null) {
            try {
                lock.release();
                raf.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't release lock", e);
            }
        }
    }
}
