package com.mooo.amjansen.nio;


import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

public class NIOFileLock {

    private long timeout = 3600000;

    private final long size;
    private final long position;
    private final FileChannel channel;

    private FileLock fileLock = null;

    private int readerCount = 0;
    private int writerCount = 0;
    private boolean shutdown = false;
    private LinkedList<Token> locks = new LinkedList();
    private LinkedList<Token> waiter = new LinkedList();

    private static class Token {
        public final static int READER = 0;
        public final static int WRITER = 1;

        public int type;
        public Thread owner;
        public int recursions = 0;

        public Token(int type) {
            owner = Thread.currentThread();
            this.type = type;
        }
    }

    protected NIOFileLock(FileChannel channel, long position, long size) {
        this.position = position;
        this.channel = channel;
        this.size = size;
    }

    public long position() {
        return position;
    }

    private int acquire(Token token, long timeout) throws IOException {

        long endTime = timeout + System.currentTimeMillis();

        waiter.addFirst(token);
        check();

        /**
         * Solange der recursions-Zähler leer ist,
         * konnte dieser Token keinen Besitz erlangen
         */
        while (token.recursions == 0) {
            try {
                long millisLeft = endTime - System.currentTimeMillis();
                wait(millisLeft);
            } catch (InterruptedException e) {
                waiter.remove(token);
                throw new IOException(e);
            }

            if (shutdown == true) {
                waiter.remove(token);
                throw new IOException(new InterruptedException());

            } else if ((token.recursions == 0) && (System.currentTimeMillis() >= endTime)) {
                waiter.remove(token);
                throw new IOException(new TimeoutException());
            }

        }
        return readerCount;
    }

    private boolean tryAcquire(Token token) {

        waiter.addFirst(token);
        check();

        /**
         * Solange der recursions-Zähler leer ist,
         * konnte dieser Token keinen Besitz erlangen
         */
        if (token.recursions == 0) {
            waiter.remove(token);
            return false;
        } else {
            return true;
        }
    }

    private void check() {
        while (waiter.isEmpty() == false) {
            Token token = waiter.getLast();

            /**
             * Erster Fall:
             *  Der Lock wird von keinem Writer besetzt und
             *  ein Reader versucht den Zugriff.
             */
            if ((writerCount == 0) && (token.type == Token.READER)) {
                ++readerCount;

                waiter.removeLast();

                boolean found = false;
                for (Token t : locks) {
                    if (t.owner == token.owner) {
                        t.recursions++;
                        found = true;
                        break;
                    }
                }
                if (found == false)
                    locks.addFirst(token);

                ++token.recursions;

                notifyAll();

                /**
                 * Zweiter Fall:
                 *  Der Lock wird von keinem Writer und keinem
                 *  Reader besetzt und ein Writer versucht den Zugriff
                 */
            } else if ((writerCount == 0) && (readerCount == 0) && (token.type == Token.WRITER)) {
                ++writerCount;

                waiter.removeLast();

                boolean found = false;
                for (Token t : locks) {
                    if (t.owner == token.owner) {
                        t.recursions++;
                        found = true;
                        break;
                    }
                }
                if (found == false)
                    locks.addFirst(token);

                ++token.recursions;

                notifyAll();

            } else return;
        }
    }

    public NIOFileLock acquire(boolean shared) throws IOException {
        synchronized (this) {
            acquire(new Token(shared ? Token.READER : Token.WRITER), timeout);

            if (((readerCount > 0) && (fileLock == null)) || (shared == false)) {
                fileLock = channel.lock(position, size, shared);
            }
        }
        return this;
    }

    public boolean tryAcquire(boolean shared) throws IOException {

        Thread currentThread = Thread.currentThread();

        synchronized (this) {
            if (tryAcquire(new Token(shared ? Token.READER : Token.WRITER)) == false) {
                return false;
            }

            if ((readerCount > 1) && (fileLock != null) && (shared == true)) {
                return (true);
            }

            if ((fileLock = channel.tryLock(position, size, shared)) == null) {

                for (Token token : locks) {
                    if ((token.owner == currentThread) && ((--token.recursions) == 0)) {
                        if (token.type == Token.WRITER) {
                            --writerCount;
                        } else {
                            --readerCount;
                        }
                        locks.remove(token);
                        break;
                    }
                }

                check();
                notify();

                return false;

            } else {
                return true;
            }
        }
    }

    public void release() throws IOException {

        Thread currentThread = Thread.currentThread();

        synchronized (this) {

            for (Token token : locks) {
                if ((token.owner == currentThread) && ((--token.recursions) == 0)) {
                    if (token.type == Token.WRITER) {
                        --writerCount;
                    } else {
                        --readerCount;
                    }
                    locks.remove(token);
                    break;
                }
            }

            if ((readerCount == 0) && (writerCount == 0)) {
                fileLock.release();
                fileLock = null;
            }

            check();
        }
    }

    @Override
    public String toString() {
        return "pos: " + position + ", size: " + size;
    }
}
