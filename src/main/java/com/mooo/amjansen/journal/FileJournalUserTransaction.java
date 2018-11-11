package com.mooo.amjansen.journal;

import javax.transaction.*;

/**
 * Created by IntelliJ IDEA
 * User: matthias
 * Date: 10.11.18
 * Time: 09:37
 * <p>
 * To change this template use File | Settings | File Templates.
 */
public class FileJournalUserTransaction implements UserTransaction {

    @Override
    public void begin() throws NotSupportedException, SystemException {

    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {

    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {

    }

    @Override
    public int getStatus() throws SystemException {
        return 0;
    }

    @Override
    public void setTransactionTimeout(int i) throws SystemException {

    }
}
