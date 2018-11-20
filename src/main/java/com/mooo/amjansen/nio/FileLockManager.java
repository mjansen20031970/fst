package com.mooo.amjansen.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.11
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public class FileLockManager {

    private static final byte[] MAGIC_NUMBER = new byte[]{'n', 'i', 'f'};
    private static final byte FILE_FORMAT_VERSION = 1;
    private static final long SINGLETON_LOCK_POSITION = 0;
    private static final long SINGLETON_LOCK_LENGTH = 8;
    private static final long GLOBAL_LOCK_POSITION = 8;
    private static final long GLOBAL_LOCK_LENGTH = 8;
    private static final long HEADER_LENGTH = 16;
    private static final int LOCK_REGION_LENGTH = 16;

    private File file;
    private int refCounter = 0;
    private int slotNameLength = 0;
    private boolean forceSync = false;
    private RandomAccessFile raf = null;
    private NIOFileLock singletonLock = null;
    private FileChannel fileChannel = null;
    private Map<String, FileLock> lockProxies = new HashMap();

    private NIOFileLockManager m;

    public FileLockManager(File file, int slotNameLength, boolean forceSync) throws IOException {

        m = NIOFileLockManager.getInstance(file);

        this.forceSync = forceSync;
        this.file = m.getFile();

        if (file.exists() == false) {
            boolean created = file.createNewFile();
            if (!created) {
                throw new IOException("Failed to create file: " + file);
            }
        }

        // Open a read/write channel to the file
        raf = new RandomAccessFile(file, "rw");
        fileChannel = m.getFileChannel();

        NIOFileLock singletonLock = m.tryLock(
                SINGLETON_LOCK_POSITION, SINGLETON_LOCK_LENGTH, false);

        if (singletonLock != null) {
            try {
                if (fileChannel.size() == 0L) {
                    // Empty file, write header
                    ByteBuffer buf = ByteBuffer.allocate((int) HEADER_LENGTH);
                    buf.put(MAGIC_NUMBER);
                    buf.put(FILE_FORMAT_VERSION);
                    buf.putInt(slotNameLength);
                    buf.put(new byte[]{0, 0, 0, 0});
                    buf.rewind();

                    fileChannel.write(buf, 0L);
                    sync();

                } else {
                    fileChannel.truncate(HEADER_LENGTH);
                    sync();

                }
            } finally {
                singletonLock.release();
            }
        }

        singletonLock = m.tryLock(
                SINGLETON_LOCK_POSITION, SINGLETON_LOCK_LENGTH, true);

        try {

            NIOFileLock lock = m.lock(GLOBAL_LOCK_POSITION, GLOBAL_LOCK_LENGTH, true);

            try {

                // Verify file header
                ByteBuffer buf = ByteBuffer.allocate((int) HEADER_LENGTH);
                fileChannel.read(buf, 0L);
                buf.rewind();

                if (buf.remaining() < HEADER_LENGTH) {
                    throw new IOException("File too short to be a compatible ID file");
                }

                byte[] magicNumber = new byte[MAGIC_NUMBER.length];
                buf.get(magicNumber);
                byte version = buf.get();

                if (!Arrays.equals(MAGIC_NUMBER, magicNumber)) {
                    throw new IOException("File doesn't contain compatible ID records");
                }

                if (version > FILE_FORMAT_VERSION) {
                    throw new IOException("Unable to read ID file; it uses a newer file format");
                } else if (version != FILE_FORMAT_VERSION) {
                    throw new IOException("Unable to read ID file; invalid file format version: " + version);
                }

                this.slotNameLength = buf.getInt();

                if (this.slotNameLength < slotNameLength) {
                    throw new IOException("Unable to read ID file; it uses a lesser slot-name-length");
                }

                this.singletonLock = singletonLock;
                singletonLock = null;

            } finally {
                lock.release();
                lock = null;
            }
        } finally {
            if (singletonLock != null) {
                singletonLock.release();
            }
        }
    }

    public long length() throws IOException {
        if (raf == null)
            return 0;
        return raf.length();
    }

    protected int incrRefCounter() {
        return ++refCounter;
    }

    protected int decrRefCounter() {
        return --refCounter;
    }

    private void sync() throws IOException {
        if (forceSync) {
            fileChannel.force(false);
        }
    }

    protected void close() throws IOException {
        fileChannel = null;
        raf.close();
        raf = null;
    }

    public FileLock openFileLock(String key) {
        FileLock proxy = null;
        synchronized (lockProxies) {
            if (((proxy = lockProxies.get(key)) == null)) {
                proxy = new FileLock(this, key);
                lockProxies.put(key, proxy);
            }
            proxy.incrRefCounter();
        }
        return proxy;
    }

    public void closeFileLock(FileLock proxy) {
        synchronized (lockProxies) {
            if (proxy.decrRefCounter() <= 0) {
                lockProxies.remove(proxy.getKey());
            }
        }
    }

    private boolean equals(byte[] a, byte[] a2) {

        if (a == null || a2 == null)
            return false;

        int length = a.length;

        for (int i = 0; i < slotNameLength; i++) {

            if (i >= length) {
                return a2[i] == 0;
            }

            if (a[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    protected NIOFileLock acquire(String keyStr, boolean shared) throws IOException {
        NIOFileLock slotLock = null;
        NIOFileLock regionLock = null;
        NIOFileLock headerLock = null;

        byte[] key = keyStr.getBytes("utf-8");

        try {
            long position = 0;
            ByteBuffer buf = ByteBuffer.allocate(slotNameLength);

            headerLock = m.lock(
                    GLOBAL_LOCK_POSITION, GLOBAL_LOCK_LENGTH, true);
            long fileLength = file.length();

            for (position = HEADER_LENGTH; position < fileLength;
                 position += slotNameLength + LOCK_REGION_LENGTH) {

                buf.rewind();

                try {
                    slotLock = m.lock(position, slotNameLength, true);
                    fileChannel.read(buf, position);
                    byte[] reedBuffer = ((ByteBuffer) (buf.flip())).array();

                    if (equals(key, reedBuffer) == true) {

                        headerLock.release();
                        headerLock = null;

                        regionLock = m.lock(
                                position + slotNameLength, LOCK_REGION_LENGTH, shared);

                        return regionLock;

                    }

                } catch (Exception e) {
                    try {
                        if (regionLock != null) {
                            regionLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }

                    throw e;
                } finally {
                    try {
                        if (slotLock != null) {
                            slotLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }
                }
            }

            headerLock.release();
            headerLock = null;

            headerLock = m.lock(
                    GLOBAL_LOCK_POSITION, GLOBAL_LOCK_LENGTH, false);
            fileLength = file.length();

            long freeSlotPosition = -1;

            for (position = HEADER_LENGTH; position < fileLength;
                 position += slotNameLength + LOCK_REGION_LENGTH) {

                buf.rewind();

                try {
                    slotLock = m.lock(
                            position, slotNameLength, true);

                    fileChannel.read(buf, position);
                    byte[] reedBuffer = ((ByteBuffer) (buf.flip())).array();

                    if (reedBuffer[0] == 0) {
                        if (freeSlotPosition < 0) {
                            freeSlotPosition = position;
                        }

                    } else if (equals(key, reedBuffer) == true) {

                        headerLock.release();
                        headerLock = null;

                        regionLock = m.lock(
                                position + slotNameLength, LOCK_REGION_LENGTH, shared);

                        return regionLock;
                    }

                } catch (Exception e) {
                    try {
                        if (regionLock != null) {
                            regionLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }

                    throw e;

                } finally {
                    try {
                        if (slotLock != null) {
                            slotLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }
                }
            }

            if (freeSlotPosition < 0) {
                freeSlotPosition = position;
            }

            try {
                slotLock = m.lock(
                        freeSlotPosition, slotNameLength, false);

                buf = ByteBuffer.wrap(key);
                fileChannel.write(buf, freeSlotPosition);
                sync();

                headerLock.release();
                headerLock = null;

                regionLock = m.lock(
                        freeSlotPosition + slotNameLength, LOCK_REGION_LENGTH, shared);

                return regionLock;

            } catch (Exception e) {
                try {
                    if (regionLock != null) {
                        regionLock.release();
                    }
                } catch (IOException e1) { /* Punt !!! */ }

                throw e;
            } finally {
                try {
                    if (slotLock != null) {
                        slotLock.release();
                    }
                } catch (IOException e1) { /* Punt !!! */ }
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;

        } finally {
            if (headerLock != null) {
                headerLock.release();
            }
        }
    }

    protected NIOFileLock tryAcquire(String keyStr, boolean shared) throws IOException {
        NIOFileLock slotLock = null;
        NIOFileLock regionLock = null;
        NIOFileLock headerLock = null;

        byte[] key = keyStr.getBytes("utf-8");

        try {
            long position = 0;
            ByteBuffer buf = ByteBuffer.allocate(slotNameLength);

            headerLock = m.lock(
                    GLOBAL_LOCK_POSITION, GLOBAL_LOCK_LENGTH, true);
            long fileLength = file.length();

            for (position = HEADER_LENGTH; position < fileLength;
                 position += slotNameLength + LOCK_REGION_LENGTH) {

                buf.rewind();

                try {
                    slotLock = m.lock(position, slotNameLength, true);

                    fileChannel.read(buf, position);

                    byte[] reedBuffer = ((ByteBuffer) (buf.flip())).array();

                    if (equals(key, reedBuffer) == true) {
                        headerLock.release();
                        headerLock = null;

                        regionLock = m.tryLock(
                                position + slotNameLength, LOCK_REGION_LENGTH, shared);

                        return regionLock;
                    }

                } catch (IOException e) {
                    try {
                        if (regionLock != null) {
                            regionLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }

                    throw e;
                } finally {
                    try {
                        if (slotLock != null) {
                            slotLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }
                }
            }

            headerLock.release();
            headerLock = null;

            headerLock = m.lock(
                    GLOBAL_LOCK_POSITION, GLOBAL_LOCK_LENGTH, false);
            fileLength = file.length();

            long freeSlotPosition = -1;

            for (position = HEADER_LENGTH; position < fileLength;
                 position += slotNameLength + LOCK_REGION_LENGTH) {

                buf.rewind();

                try {
                    slotLock = m.lock(
                            position, slotNameLength, true);
                    fileChannel.read(buf, position);
                    byte[] reedBuffer = ((ByteBuffer) (buf.flip())).array();

                    if (reedBuffer[0] == 0) {
                        if (freeSlotPosition < 0) {
                            freeSlotPosition = position;
                        }

                    } else if (equals(key, reedBuffer) == true) {

                        headerLock.release();
                        headerLock = null;

                        regionLock = m.tryLock(
                                position + slotNameLength, LOCK_REGION_LENGTH, shared);

                        return regionLock;
                    }

                } catch (IOException e) {

                    try {
                        if (regionLock != null) {
                            regionLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }

                    throw e;

                } finally {
                    try {
                        if (slotLock != null) {
                            slotLock.release();
                        }
                    } catch (IOException e1) { /* Punt !!! */ }
                }
            }

            if (freeSlotPosition < 0) {
                freeSlotPosition = position;
            }

            try {
                slotLock = m.lock(
                        freeSlotPosition, slotNameLength, false);

                buf = ByteBuffer.wrap(key);
                fileChannel.write(buf, freeSlotPosition);
                sync();

                headerLock.release();
                headerLock = null;

                regionLock = m.tryLock(
                        freeSlotPosition + slotNameLength, LOCK_REGION_LENGTH, shared);

                return regionLock;

            } catch (IOException e) {
                try {
                    if (slotLock != null) {
                        slotLock.release();
                    }
                } catch (IOException e1) { /* Punt !!! */ }

                try {
                    if (regionLock != null) {
                        regionLock.release();
                    }
                } catch (IOException e1) { /* Punt !!! */ }

                throw e;

            } finally {
                try {
                    if (slotLock != null) {
                        slotLock.release();
                    }
                } catch (IOException e1) { /* Punt !!! */ }
            }

        } finally {
            if (headerLock != null) {
                headerLock.release();
            }
        }
    }

    protected void release1(NIOFileLock regionLock) throws IOException {
        NIOFileLock headerLock = null;

        headerLock = m.lock(
                GLOBAL_LOCK_POSITION, GLOBAL_LOCK_LENGTH, true);

        try {

            long position = regionLock.position() - slotNameLength;

        } finally {
            try {
                regionLock.release();
            } catch (IOException e) { /* Punt !!! */ }
            try {
                headerLock.release();
            } catch (IOException e) { /* Punt !!! */ }
        }
    }

    protected void release(NIOFileLock regionLock) throws IOException {
        NIOFileLock slotLock = null;
        NIOFileLock headerLock = null;

        headerLock = m.lock(
                GLOBAL_LOCK_POSITION, GLOBAL_LOCK_LENGTH, false);

        try {
            long position = regionLock.position() - slotNameLength;

            slotLock = m.tryLock(position, slotNameLength, false);
            if (slotLock != null) {
                try {
                    ByteBuffer buf = ByteBuffer.wrap(new byte[slotNameLength]);
                    fileChannel.write(buf, position);
                    sync();
                } finally {
                    try {
                        slotLock.release();
                    } catch (IOException e) { /* Punt !!! */ }
                }
            }

        } finally {
            try {
                regionLock.release();
            } catch (IOException e) { /* Punt !!! */ }
            try {
                headerLock.release();
            } catch (IOException e) { /* Punt !!! */ }
        }
    }

}
