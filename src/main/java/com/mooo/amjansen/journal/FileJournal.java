package com.mooo.amjansen.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.Xid;
import java.io.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 * <p>
 * To change this template use File | Settings | File Templates.
 */
public class FileJournal {

    private static Logger logger = LoggerFactory.getLogger(FileJournal.class);

    private static final int PREPARE = -1;
    private static final int COMMIT = -2;
    private static final int UNDO = -3;

    private Xid xid = null;
    private int undos = 0;
    private long markPoint = 0;
    private Object txContext = null;
    private File journalFile = null;
    private boolean prepared = false;
    private boolean commited = false;
    private RandomAccessFile raf = null;
    private boolean rollbackOnly = false;
    private ArrayList<JournalAction> actions = new ArrayList();
    private ArrayList<JournalAction> actionsBackup = actions;
    private Map<CancelableJournalAction, Integer> cancelableActions = new IdentityHashMap();

    public FileJournal(Object txContext, File journalFile) throws IOException, ClassNotFoundException {
        this.raf = new RandomAccessFile(this.journalFile = journalFile, "rw");
        this.txContext = txContext;
        switch (undos = read()) {
            case COMMIT:
                commited = false;
                break;
            case PREPARE:
                prepared = true;
                break;
            default:
                rollbackOnly = true;
                break;
        }
    }

    public FileJournal(Object txContext, File journalFile, Xid xid) throws IOException, ClassNotFoundException {
        this.raf = new RandomAccessFile(this.journalFile = journalFile, "rw");
        this.txContext = txContext;
        this.xid = xid;
        writeHeader();
    }

    public Xid getXid() {
        return xid;
    }

    protected File getJournalFile() {
        return journalFile;
    }

    protected void close() throws IOException {
        if (raf != null)
            raf.close();
        raf = null;
        this.txContext = null;
        this.actions = new ArrayList<>();
    }

    public boolean isPrepared() {
        return prepared;
    }

    public int getActionId(CancelableJournalAction canceled) {
        return cancelableActions.get(canceled);
    }

    private void writeHeader() throws IOException {
        mark();
        if (xid != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(xid);
            oos.close();
            byte[] ser = baos.toByteArray();
            raf.writeInt(ser.length);
            raf.write(ser);
        } else raf.writeInt(0);
        flush();
    }

    public void writeAction(JournalAction a) throws IOException {
        mark();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(a);
        oos.close();
        byte[] ser = baos.toByteArray();
        raf.writeInt(ser.length);
        raf.write(ser);
        flush();
        if (a instanceof CancelableJournalAction)
            cancelableActions.put((CancelableJournalAction) (a), actions.size());
        actions.add(a);
    }

    public void actionFailed() throws IOException {
        reset();
        flush();
        actions.remove(actions.size() - 1);
    }

    public void flush() throws IOException {
        raf.getFD().sync();
    }

    private void mark() throws IOException {
        markPoint = raf.getFilePointer();
    }

    private void reset() throws IOException {
        raf.seek(markPoint);
        raf.setLength(markPoint);
    }

    private void writeMarker(int marker) throws IOException {
        raf.writeInt(marker);
        flush();
    }

    public void prepare() throws TransactionException, InconsistentStateException {
        if (prepared == false) {
            try {
                for (JournalAction action : actions) {
                    action.prepare(txContext);
                }
                writeMarker(PREPARE);
                prepared = true;
            } catch (Exception e) {
                rollback();
                throw new TransactionException(e);
            }
        }
    }

    public List<IOException> commit() throws TransactionException, InconsistentStateException {

        prepare();

        int actionIndex = 0;
        JournalAction[] actions = this.actions.toArray(
                new JournalAction[this.actions.size()]);
        try {
            for (; actionIndex < actions.length; actionIndex++) {
                actions[actionIndex].commit(txContext);
            }
            writeMarker(COMMIT);
        } catch (Exception e) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < actions.length; i++) {
                buffer.append(i < actionIndex ? " C " : " P ");
                buffer.append(actions[i].toString());
                buffer.append(System.getProperty("line.separator"));
            }
            logger.error(buffer.toString(), e);
            throw new TransactionException(e);
        }
        return cleanup();
    }

    public List<IOException> rollback() throws InconsistentStateException {
        try {
            if (journalFile.exists() == false) {
                logger.info("journal " + journalFile.getAbsolutePath() + " is already deleted!!!");
            } else if (raf==null){
                logger.info("journal " + journalFile.getAbsolutePath() + " is already closed!!!");
            } else flush();
        } catch (Exception e) {
            throw new InconsistentStateException(e);
        }
        return rollback(0);
    }

    /**
     * Liefert eine Liste Exceptions die in der Cleanup-Phase aufgetreten sind.
     * Das Cleanup wird im Ganzen gemacht, um die Aufrufe während der Undo-Phase
     * nicht zu beeinflussen.
     *
     * @param nUndos Anzahl der Undos die gemacht werden sollen
     * @return Liste der aufgetretenen Exceptions
     * @throws InconsistentStateException
     */
    private List<IOException> rollback(int nUndos) throws InconsistentStateException {

        try {
            play(nUndos);
        } catch (Exception e) {
            try {
                close();
            } catch (IOException e2) { /* Punt !!! */ }
            throw new InconsistentStateException(e);
        }
        return cleanup();
    }

    /**
     * Führt ein Undo auf jede Aktion in umgekehrter Reihenfolge durch
     *
     * @param undos Anzahl der
     * @throws IOException
     */
    void play(int undos) throws IOException {
        int start = actions.size() - 1 - undos;
        for (int i = start; i >= 0; i--) {
            JournalAction action = actions.get(i);
            action.undo(txContext);

            /**
             * Markiert die Aktion als zurückgespielt, so dass
             * sie nicht noch einmal zurückgespielt werden muss.
             */

            writeMarker(UNDO);
        }
    }

    /**
     * Kürzt das Journal um den letzten Eintrag, wenn er nur teilweise
     * vorhanden ist und liefert eine Liste der Exceptions, die hierbei
     * aufgreten sind.
     *
     * @param txContext Umgebungs-Objekt das für ein Recovery benötigt wird.
     * @param journalFile Die Journal-Datei
     * @return Liste der aufgetretenen Exceptions
     * @throws InconsistentStateException
     */
    public static List<IOException> recover(Object txContext, File journalFile) throws InconsistentStateException {
        try {
            FileJournal j = new FileJournal(txContext, journalFile);
            return j.recover();
        } catch (Exception e) {
            throw new InconsistentStateException(e);
        }
    }

    private List<IOException> recover() throws InconsistentStateException {
        if (undos == COMMIT) {
            return cleanup();
        } else if (undos == PREPARE) {
            return rollback(0);
        } else return rollback(undos);
    }

    private int read() throws IOException, ClassNotFoundException {
        long pos = 0;
        int undoCount = 0;
        int recordSize = 0;
        byte[] buf = new byte[100];
        long fileLength = raf.length();

        if (fileLength == 0) {
            return 0;
        }

        raf.seek(0);

        /**
         * Als erstes wird der Header gelesen.
         */
        recordSize = raf.readInt();
        if (recordSize > buf.length)
            buf = new byte[recordSize];
        if (recordSize > 0) {
            raf.read(buf, 0, recordSize);
            ByteArrayInputStream bain = new ByteArrayInputStream(buf);
            ObjectInputStream oin = new ObjectInputStream(bain);
            xid = (Xid) oin.readObject();
        } else xid = null;

        while ((pos = raf.getFilePointer()) < fileLength) {
            try {
                recordSize = raf.readInt();
            } catch (EOFException e) {
                // There was a crash while writing the int.  Ignore this record.
                break;
            }
            if (undoCount > 0) { // we are reading the undo markers at the end
                if (recordSize != UNDO)
                    throw new IOException("journal corrupted: non-undo after undo");
                undoCount++;
            } else if (recordSize == COMMIT) {
                if (raf.getFilePointer() == fileLength)
                    return COMMIT;
                throw new IOException("journal corrupted: COMMIT marker not at end");
            } else if (recordSize == PREPARE) {
                if (raf.getFilePointer() == fileLength)
                    return PREPARE;
                throw new IOException("journal corrupted: COMMIT marker not at end");
            } else if (recordSize == UNDO) {
                undoCount++;
            } else if (recordSize <= 0) {
                throw new IOException("journal corrupted: record size <= 0");
            } else if (pos + recordSize > fileLength) {
                // There was a crash while writing the record.  Ignore the record.
                break;
            } else {
                // Read the record.
                if (recordSize > buf.length)
                    buf = new byte[recordSize];
                raf.read(buf, 0, recordSize);
                ByteArrayInputStream bain = new ByteArrayInputStream(buf);
                ObjectInputStream oin = new ObjectInputStream(bain);
                JournalAction a = (JournalAction) oin.readObject(); // This will check whether

                if (a instanceof CancelableJournalAction) {
                    int id = ((CancelableJournalAction) (a)).getCancelId();
                    if (id >= 0)
                        ((CancelableJournalAction) (actions.get(id))).cancel();
                }

                actions.add(a);
            }
        } // end while

        if (pos < fileLength) { // remove partial junk at end
            raf.setLength(pos);
            flush();
        }
        return undoCount;
    }

    /* Cleanup IOExceptions are considered minor enough that they don't
         get thrown, but instead accumulated into a list for optional
         processing by clients.
    */
    private List<IOException> cleanup() {
        // Delete all temporary files created as part of this transaction.
        ArrayList<IOException> exceptions = new ArrayList();
        for (int i = actions.size() - 1; i >= 0; i--) {
            try {
                actions.get(i).cleanup(txContext);
            } catch (IOException e) {
                exceptions.add(e);
            }
        }
        // Close and delete the journal file itself.
        try {
            close();
        } catch (IOException e) {
            exceptions.add(e);
        }
        try {
            if (journalFile.delete() == false) {
                throw new IOException("can't delete " + journalFile.getAbsolutePath());
            }
        } catch (IOException e) {
            exceptions.add(e);
        }
        return exceptions;
    }

    public void dump(PrintStream writer) {

        writer.println("start dumping journal: "+ journalFile.getAbsolutePath());

        writer.println("start dumping actions, count: " + actionsBackup.size());

        for (int i = 0; i < actionsBackup.size(); i++) {
            writer.println("action: " + i);
            try {
                writer.println(actionsBackup.get(i).toString());
            } catch (Throwable throwable) {
                writer.println("dumping action failed!");
                throwable.printStackTrace(writer);
            }
        }

        writer.println("end dumping journal: "+ journalFile.getAbsolutePath());

    }

    public void dump(PrintStream writer, File journalFile) {

        FileJournal j = null;

        writer.println("start dumping journal: "+ journalFile.getAbsolutePath());

        try {
            j = new FileJournal(null, journalFile);

        } catch (Throwable throwable){
            writer.println("opening journal failed!");
            throwable.printStackTrace(writer);
            writer.println("end dumping journal: "+ journalFile.getAbsolutePath());
            return;

        }

        try {
            try {
                j.read();

            } catch (Throwable throwable) {
                writer.println("reading journal failed!");
                throwable.printStackTrace(writer);
                writer.println("end dumping journal: " + journalFile.getAbsolutePath());
                return;

            }


            writer.println("start dumping actions, count: " + j.actionsBackup.size());

            for (int i = 0; i < j.actionsBackup.size(); i++) {
                writer.println("action: " + i);
                try {
                    writer.println(j.actionsBackup.get(i).toString());
                } catch (Throwable throwable) {
                    writer.println("dumping action failed!");
                    throwable.printStackTrace(writer);
                }
            }

            writer.println("end dumping journal: "+ journalFile.getAbsolutePath());


        } finally {
            try {
                j.close();
            } catch (IOException e) { /* Punt !!! */ }
        }
    }

}
	
