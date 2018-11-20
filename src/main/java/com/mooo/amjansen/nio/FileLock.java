package com.mooo.amjansen.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

public class FileLock {

    private static Logger logger = LoggerFactory.getLogger(FileLock.class);

    /**
     * Standard-Wert, in Millisekunden, für die maximale Dauer einer
     * Anforderung für einen Lock.
     */
    private long timeout = 3600000;

    private String key = null;
    private int refCounter = 0;

    /**
     * Dieses Objekt repräsentiert den Lock auf den Bereich
     * in der Lock-Datei, der zur Synchronisation über
     * Prozessgrenzen benutzt wird.
     */
    private NIOFileLock position = null;


    private FileLockManager manager = null;

    /**
     * Zeigt an wieviele unterschiedliche Threads
     * einen Read-Lock in diesem Prozess halten
     */
    private int readerCount = 0;

    /**
     * Zeigt an wieviele unterschiedliche Threads
     * einen Write-Lock in diesem Prozess halten.
     * Dieser Wert darf maximal 1 sein!
     */
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

    protected FileLock(FileLockManager manager, String key) {
        this.manager = manager;
        this.key = key;
    }

    protected String getKey() {
        return key;
    }

    protected void incrRefCounter() {
        ++refCounter;
    }

    protected int decrRefCounter() {
        return --refCounter;
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
                throw new IOException(new InterruptedException("interrupting cause of shutdown"));

            } else if ((token.recursions == 0) && (System.currentTimeMillis() >= endTime)) {
                waiter.remove(token);
                throw new IOException(new TimeoutException("timeout is expired"));
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

            if (token.type == Token.READER) {
                /**
                 * Es wird ein Reader-Lock angefordert.
                 * Wenn es keinen bestehenden Write-Lock gibt,
                 * wird der Zugriff gewährt.
                 */
                if (writerCount == 0) {

                    waiter.removeLast();

                    /**
                     * Es muss geprüft werden, ob der zugreifende
                     * Thread bereits einen Lock hält. Entsprechend
                     * wird der Lock zur Liste hinzugefügt oder nur
                     * der Referenzzähler inkrementiert.
                     */
                    boolean found = false;
                    for (Token t : locks) {
                        if (t.owner == token.owner) {
                            t.recursions++;
                            found = true;
                            break;
                        }
                    }

                    /**
                     * Wenn noch kein Lock für diesen Thread existiert,
                     * wird der Token zu den Locks hinzugefügt und
                     * der Reader-Count inkrementiert.
                     */
                    if (found == false) {
                        locks.addFirst(token);
                        ++readerCount;
                    }

                    ++token.recursions;

                    notifyAll();

                } else return;

            } else if (token.type == Token.WRITER) {

                /**
                 * Es besteht bisher kein Lock, also wird
                 * die Anforderung zugelassen
                 */
                if ((writerCount == 0) && (readerCount == 0)) {

                    ++writerCount;

                    waiter.removeLast();

                    locks.addFirst(token);

                    ++token.recursions;

                    notifyAll();

                    /**
                     * Es besteht bereits ein Write-Lock, also
                     * darf nur der gleiche Thread passieren.
                     */
                } else if (writerCount > 0) {

                    /**
                     * Es darf nur einen geben!!!
                     */
                    Token t = locks.getFirst();

                    if (t.owner == token.owner) {

                        waiter.removeLast();

                        ++token.recursions;

                        ++t.recursions;

                        notifyAll();

                    }  else return;

                } else return;

            } else logger.error("unknown token-type: " + token.type);

        }

    }

    private void check2() {

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

    public void acquire(boolean shared) throws IOException {
        synchronized (this) {
            acquire(new Token(shared ? Token.READER : Token.WRITER), timeout);

            /**
             * Wenn position leer ist, dann ist noch kein
             * physikalischer File-Lock vorhanden
             */
            if (position == null) {
                position = manager.acquire(key, shared);
            }
        }
    }

    public boolean tryAcquire(boolean shared) throws Exception {

        Thread currentThread = Thread.currentThread();

        synchronized (this) {
            if (tryAcquire(new Token(shared ? Token.READER : Token.WRITER)) == false) {
                return false;
            }

            /**
             * Wenn position leer ist, dann ist noch kein
             * physikalischer File-Lock vorhanden
             */
            if (position != null) {
                return (true);
            }

            if ((position = manager.tryAcquire(key, shared)) == null) {

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
                manager.release(position);
                position = null;
            }

            check();
            notify();
        }
    }

    /**
     * Schließt diesen Lock. Wenn alle angeforderten Locks
     * geschlossen wurden, ist der Manager in der Lage den
     * Bereich des Schlüssels in der Lock-Datei wieder
     * für andere Schlüssel zu benutzen.
     */
    public void close() {
        manager.closeFileLock(this);
    }

}
