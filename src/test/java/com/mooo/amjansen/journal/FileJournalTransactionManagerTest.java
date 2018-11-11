package com.mooo.amjansen.journal;

import com.mooo.amjansen.journal.action.FileCopyAction;
import com.mooo.amjansen.journal.action.FileDeleteAction;
import com.mooo.amjansen.journal.action.FileRenameAction;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public class FileJournalTransactionManagerTest {

    public static void main(String[] args) throws IOException, InconsistentStateException, TransactionException {

        FileJournalTransactionManager transactionManager =
                new FileJournalTransactionManager(null, new File("transaction"));

        JournalTransaction transaction = transactionManager.beginTransaction(null);

        new File("test-data-1").createNewFile();

        transaction.run(new FileCopyAction(new File("test-data-1"),new File("test-data-2"), true));
        transaction.run(new FileRenameAction(new File("test-data-2"), new File("test-data-3"), true));
        transaction.run(new FileDeleteAction(new File("test-data-3"), true));

        transactionManager = null;
    }

}
