package com.mooo.amjansen.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mja
 * Date: 27.09.11
 * Time: 13:20
 * To change this template use File | Settings | File Templates.
 */
public class NIOFileLockManager {

    private File file = null;
    private RandomAccessFile raf = null;
    private FileChannel fileChannel = null;

    private Map<Long, NIOFileLock> endPositions = new HashMap();
    private Map<Long, NIOFileLock> startPositions = new HashMap();

    private static Map<String, NIOFileLockManager> managers;

    public static NIOFileLockManager getInstance(File file) throws IOException {
        synchronized (NIOFileLockManager.class) {
            if (managers == null) {
                managers = new HashMap<String, NIOFileLockManager>();
            }

            String absolutPath = file.getAbsolutePath();
            NIOFileLockManager manager = managers.get(absolutPath);
            if (manager == null) {
                managers.put(absolutPath, manager = new NIOFileLockManager(file));
            }
            return manager;
        }
    }

    protected NIOFileLockManager(File file) throws IOException {

        if (file.exists() == false) {
            boolean created = file.createNewFile();
            if (!created) {
                throw new IOException("Failed to create file: " + file);
            }
        }

        // Open a read/write channel to the file
        raf = new RandomAccessFile(this.file = file, "rw");
        fileChannel = raf.getChannel();
    }

    public File getFile() {
        return file;
    }

    public FileChannel getFileChannel() {
        return fileChannel;
    }

    public void close() throws IOException {
        raf.close();
    }

    public NIOFileLock lock(long position, long size, boolean shared) throws IOException {

        NIOFileLock fileLock = null;

        synchronized (startPositions) {
            if ((fileLock = startPositions.get(position)) == null) {
                startPositions.put(position, fileLock = new NIOFileLock(
                        fileChannel, position, size));
            }
        }

        return fileLock.acquire(shared);
    }

    public NIOFileLock tryLock(long position, long size, boolean shared) throws IOException {

        NIOFileLock fileLock = null;

        synchronized (startPositions) {
            if ((fileLock = startPositions.get(position)) == null) {
                startPositions.put(position, fileLock = new NIOFileLock(
                        fileChannel, position, size));
            }
        }

        return (fileLock.tryAcquire(shared) ? fileLock : null);

    }
}
