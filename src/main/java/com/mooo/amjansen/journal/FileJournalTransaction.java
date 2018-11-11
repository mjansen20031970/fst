package com.mooo.amjansen.journal;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public class FileJournalTransaction implements JournalTransaction {

    private File backupDir = null;
    private Object txContext = null;
    private FileJournal journal = null;
    private boolean rollbackOnly = false;
    private FileJournalTransactionManager transactionManager = null;

    public FileJournalTransaction(FileJournalTransactionManager transactionManager, Object txContext, File journalFile, File backupDir) throws TransactionException {
		try {
			this.journal = new FileJournal(txContext, journalFile, null);
            this.transactionManager = transactionManager;
            this.txContext = txContext;
            this.backupDir = backupDir;
		} catch (IOException e) {
			throw new TransactionException(e);
        } catch (ClassNotFoundException e) {
            throw new TransactionException(e);
        }
    }

	@Override
    public boolean isActive() {
        return journal != null;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void setRollbackOnly(boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }

	@Override
    public synchronized Object run(JournalAction a) throws IllegalStateException, IOException, TransactionException, InconsistentStateException {
        if (journal==null){
            throw new IllegalStateException("transaction not active");
        }

		try {
			a.preJournalWrite(txContext, backupDir);
			journal.writeAction(a);
			a.postJournalWrite(txContext);
		} catch (RuntimeException e) {
			rollback();
			throw new TransactionException(e);
		} catch (IOException e) {
			rollback();
			throw new TransactionException(e);
		}

		try {
			Object result = a.execute(txContext);
            return result;
		} catch (IOException e) {
			try {
				journal.actionFailed();
			} catch (Exception e2) {
				rollback();
				throw new TransactionException(e2, e);
			}
			throw e;
		} catch (RuntimeException e) {
			try {
				journal.actionFailed();
			} catch (Exception e2) {
				rollback();
				throw new TransactionException(e2, e);
			}
			throw e;
		}
	}

	@Override
    public synchronized Object cancelAndRun(CancelableJournalAction canceled, CancelableJournalAction a) throws IllegalStateException, IOException, TransactionException, InconsistentStateException {
        if (journal==null){
            throw new IllegalStateException("transaction not active");
        }

		try {
            if (canceled!=null){
                a.setCancelId(journal.getActionId(canceled), canceled);
                canceled.cancel();
            }

			a.preJournalWrite(txContext, backupDir);

            journal.writeAction(a);

            a.postJournalWrite(txContext);

		} catch (RuntimeException e) {
			rollback();
			throw new TransactionException(e);

		} catch (IOException e) {
			rollback();
			throw new TransactionException(e);

		}

		try {
			Object result = a.execute(txContext);
            return result;

		} catch (RuntimeException e) {
			try {
				journal.actionFailed();
			} catch (Exception e2) {
				rollback();
				throw new TransactionException(e2, e);
			}
			throw e;
		}
	}

	@Override
    public synchronized void prepare() throws TransactionException, InconsistentStateException {
		if (journal!=null){
            journal.prepare();
        }
	}

	@Override
    public synchronized void commit() throws TransactionException, InconsistentStateException {
        if (journal!=null){
            journal.commit();
            txContext = null;
            journal = null;
        }
        if (transactionManager!=null){
            transactionManager.endTransaction(this);
            transactionManager = null;
        }
	}

	@Override
    public synchronized void rollback() throws InconsistentStateException {
        if (journal!=null){
            journal.rollback();
            txContext = null;
            journal = null;
        }
        if (transactionManager!=null){
            transactionManager.endTransaction(this);
            transactionManager = null;
        }
	}

    @Override
    public boolean isPrepared() {
        return journal.isPrepared();
    }

    @Override
    public void dump(PrintStream writer) {
        journal.dump(writer);
    }
}
