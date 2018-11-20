package com.mooo.amjansen.nio;

import java.io.IOException;

public class FileLockGuard {

    private String key = null;
    private FileLock proxy = null;
    private FileLockManager manager = null;

    protected FileLockGuard(FileLockManager manager, String key) {
        proxy = (this.manager = manager).openFileLock(this.key = key);
    }

    @Override
    protected void finalize() {
        if (proxy != null) {
            manager.closeFileLock(proxy);
        }
    }

    public void acquire(boolean shared) throws Exception {
        proxy.acquire(shared);
    }

    public boolean tryAcquire(boolean shared) throws Exception {
        return proxy.tryAcquire(shared);
    }

    public void release() throws IOException {
        proxy.release();
    }

    public void close() {
        if (proxy != null) {
            manager.closeFileLock(proxy);
            proxy = null;
        }
    }

}
