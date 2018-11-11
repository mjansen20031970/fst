package com.mooo.amjansen.journal;

import com.mooo.amjansen.utils.NUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 * <p>
 * To change this template use File | Settings | File Templates.
 */
public class FileJournalTransactionManager {

    private static Logger logger = LoggerFactory.getLogger(FileJournalTransactionManager.class);

    public final static String JOURNAL_FILENAME_BASE = "journal-";

    private File backupDir;
    private File journalDir;
    private boolean renameCanDelete;
    private boolean recoverInProgress = false;
    private List<JournalTransaction> transactions;

    public FileJournalTransactionManager(Object txContext, File journalDir) throws IOException, InconsistentStateException {
        this(txContext, journalDir, new File(journalDir, "backups"));
    }

    public FileJournalTransactionManager(Object txContext, File journalDir, File backupDir) throws IOException, InconsistentStateException {

        this.journalDir = journalDir.getAbsoluteFile();
        if ((journalDir.exists() == false) && (journalDir.mkdirs() == false))
            throw new IOException("journal directory '" + journalDir.getAbsolutePath() + "' doesn't exist and can't be created");
        if (journalDir.isDirectory() == false)
            throw new IOException("journal directory '" + journalDir.getAbsolutePath() + "' is no directory");
        if (journalDir.canRead() == false)
            throw new IOException("journal directory '" + journalDir.getAbsolutePath() + "' is not readable");
        if (journalDir.canWrite() == false)
            throw new IOException("journal directory '" + journalDir.getAbsolutePath() + "' is not writable");

        this.backupDir = backupDir.getAbsoluteFile();
        if ((backupDir.exists() == false) && (backupDir.mkdirs() == false))
            throw new IOException("journal-backup directory '" + backupDir.getAbsolutePath() + "' doesn't exist and can't be created");
        if (backupDir.isDirectory() == false)
            throw new IOException("journal-backup directory '" + backupDir.getAbsolutePath() + "' is no directory");
        if (backupDir.canRead() == false)
            throw new IOException("journal-backup directory '" + backupDir.getAbsolutePath() + "' is not readable");
        if (backupDir.canWrite() == false)
            throw new IOException("journal-backup directory '" + backupDir.getAbsolutePath() + "' is not writable");

        checkRename();
        this.transactions = new ArrayList();
        recover(txContext);
    }


    public boolean renameCanDelete() {
        return renameCanDelete;
    }

    public File getJournalDir() {
        return journalDir;
    }

    public File getBackupDir() {
        return backupDir;
    }

    private void checkRename() throws IOException {
        File t1 = File.createTempFile("checkRename", "", journalDir);
        File t2 = File.createTempFile("checkRename", "", journalDir);
        renameCanDelete = t1.renameTo(t2);
        t1.delete();
        t2.delete();
    }

    public Journal getCurrentTransaction() {
        return null;
    }

    protected synchronized JournalTransaction createTransaction(Object txContext, File journalFile, File backupDir) throws TransactionException {
        return new FileJournalTransaction(this, txContext, journalFile, backupDir);
    }

    public synchronized JournalTransaction beginTransaction(Object txContext) throws TransactionException {
        File journalFile = new File(getJournalDir(), JOURNAL_FILENAME_BASE + NUIDGenerator.unique());
        JournalTransaction tx = createTransaction(txContext, journalFile, backupDir);
        transactions.add(tx);
        return tx;
    }

    public synchronized void endTransaction(JournalTransaction transaction) {
        if (transactions != null) {
            transactions.remove(transaction);
        }
    }

    public synchronized void close() throws InconsistentStateException {
        Collection<JournalTransaction> transactions = this.transactions;
        this.transactions = null;
        for (JournalTransaction transaction : transactions) {
            transaction.rollback();
        }
    }

    public boolean isRecoverInProgress() {
        return recoverInProgress;
    }

    protected void afterRecover(Object txContext) throws InconsistentStateException {
    }

    protected void recover(Object txContext, File journalFile) throws InconsistentStateException {
        FileJournal.recover(txContext, journalFile);
    }

    private void recover(Object txContext) throws InconsistentStateException {
        File[] files = journalDir.listFiles();
        if ((files != null) && (files.length > 0)) {
            logger.info("start recovery of " + files.length + " journals");
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().startsWith("journal-")) {
                    recoverInProgress = true;
                    recover(txContext, files[i]);
                }
            }
            logger.info("recovery finished");
        }
        afterRecover(txContext);
        recoverInProgress = false;
    }

    @Override
    protected void finalize() {
        try {
            close();
        } catch (InconsistentStateException e) {
            logger.error("INCONSISTENT STATE IN FINALIZE", e);
        }
    }

}

