package com.mooo.amjansen.journal;

import com.mooo.amjansen.utils.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA
 * User: matthias
 * Date: 10.11.18
 * Time: 10:19
 * <p>
 * To change this template use File | Settings | File Templates.
 */
public class FileJournalTransactionManagerFactory {

    private static Logger logger = LoggerFactory.getLogger(FileJournalTransactionManagerFactory.class);

    private static FileJournalTransactionManager transactionManager = null;

    public FileJournalTransactionManager getInstance(){

        if (transactionManager != null)
            return transactionManager;

        synchronized (FileJournalTransactionManagerFactory.class) {

            if (transactionManager != null)
                return transactionManager;

            throw new RuntimeException("Transaction-Manager isn't initialized");

        }
    }

    public static FileJournalTransactionManager initInstance(Object txContext, File journalDir) throws IOException, InconsistentStateException {

        if (transactionManager != null)
            throw new RuntimeException("Transaction-Manager is allready initialized");

        synchronized (FileJournalTransactionManagerFactory.class){
            if (transactionManager != null)
                throw new RuntimeException("Transaction-Manager is allready initialized");

            FileJournalTransactionManager temp = new FileJournalTransactionManager(
                    txContext,
                    journalDir);

            return transactionManager = temp;

        }

    }

    public static FileJournalTransactionManager initInstance(Object txContext, File journalDir, File backupDir) throws IOException, InconsistentStateException {

        if (transactionManager != null)
            throw new RuntimeException("Transaction-Manager is allready initialized");

        synchronized (FileJournalTransactionManagerFactory.class){

            if (transactionManager != null)
                throw new RuntimeException("Transaction-Manager is allready initialized");

            FileJournalTransactionManager temp = new FileJournalTransactionManager(
                    txContext,
                    journalDir,
                    backupDir);

            return transactionManager = temp;

        }

    }

    public static FileJournalTransactionManager initInstance(Object txContext) throws IOException, InconsistentStateException {

        if (transactionManager != null)
            throw new RuntimeException("Transaction-Manager is allready initialized");

        synchronized (FileJournalTransactionManagerFactory.class){
            if (transactionManager != null)
                throw new RuntimeException("Transaction-Manager is allready initialized");

            Configuration config = Configuration.getInstance();

            String journalDir = config.getValue("filejournal.journaldir");
            String backupDir = config.getValue("filejournal.backupdir", journalDir);

            FileJournalTransactionManager temp = new FileJournalTransactionManager(
                    txContext,
                    new File(journalDir),
                    new File(backupDir));

            return transactionManager = temp;

        }

    }



}
