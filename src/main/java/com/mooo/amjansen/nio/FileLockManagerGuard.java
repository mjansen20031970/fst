package com.mooo.amjansen.nio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mjansen
 * Date: 30.03.2017
 * Time: 11:03:40
 * <p>
 * Diese Klasse fungiert
 */
public class FileLockManagerGuard {

    private final static Map<String, FileLockManager> managers = new HashMap();

    private File canonicalFile = null;
    private FileLockManager manager;
    private String absoluteFileName = null;

    public FileLockManagerGuard(File file, int slotNameLength) throws IOException {
        this(file, slotNameLength, false);
    }

    public FileLockManagerGuard(File file, int slotNameLength, boolean forceSync) throws IOException {

        canonicalFile = file.getCanonicalFile();
        absoluteFileName = canonicalFile.getAbsolutePath();

        synchronized (managers) {
            manager = managers.get(absoluteFileName);
            if (manager == null) {
                manager = new FileLockManager(canonicalFile, slotNameLength, forceSync);
                managers.put(absoluteFileName, manager);
            }
            manager.incrRefCounter();
        }
    }

    @Override
    protected void finalize() {
        try {
            close();
        } catch (IOException e) { /* Punt !!! */ }
    }

    public long length() throws IOException {
        return manager.length();
    }

    public FileLock openFileLock(String key) {
        return manager.openFileLock(key);
    }

    public void closeFileLock(FileLock lock) {
        manager.closeFileLock(lock);
    }

    public FileLockGuard openFileLockGuard(String key) {
        return new FileLockGuard(manager, key);
    }

    public void close() throws IOException {
        if (manager != null) {
            synchronized (managers) {
                if (manager.decrRefCounter() <= 0) {
                    managers.remove(absoluteFileName);
                    manager.close();
                }
                manager = null;
            }
        }
    }
}
