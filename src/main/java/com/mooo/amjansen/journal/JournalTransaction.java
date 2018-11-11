package com.mooo.amjansen.journal;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public interface JournalTransaction extends Journal {

    boolean isActive();

    boolean isRollbackOnly();

    void setRollbackOnly(boolean rollbackOnly);

    void prepare() throws TransactionException, InconsistentStateException;

    void commit() throws TransactionException, InconsistentStateException;

    void rollback() throws InconsistentStateException;

    boolean isPrepared();
}
