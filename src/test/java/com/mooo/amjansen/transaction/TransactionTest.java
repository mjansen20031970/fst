package com.mooo.amjansen.transaction;

import com.mooo.amjansen.journal.*;
import com.mooo.amjansen.journal.action.TestJournalAction;
import junit.framework.TestCase;

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
public class TransactionTest extends TestCase {

    private FileJournalTransactionManager fileJournalTransactionManager = null;


    public void setUp() throws IOException, InconsistentStateException {

        fileJournalTransactionManager = new FileJournalTransactionManager(null, new File("./journals"));

    }

    public void tearDown() throws InconsistentStateException {

        fileJournalTransactionManager.close();
        fileJournalTransactionManager = null;

    }

    public void test0() throws TransactionException, IOException, InconsistentStateException {

        JournalTransaction transaction = fileJournalTransactionManager.beginTransaction(this);

        TransactionContext context = new TransactionContext();

        TestJournalAction[] actions = new TestJournalAction[]{
                new TestJournalAction(context),
                new TestJournalAction(context)
        };

        for (JournalAction action : actions) {
            transaction.run(action);
        }

        transaction.commit();

        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREJOURNALWRITE] == 1);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 2);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.EXECUTE] == 3);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREPARE] == 7);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.UNDO] == 0);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.CLEANUP] == 12);

        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREJOURNALWRITE] == 4);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 5);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.EXECUTE] == 6);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREPARE] == 8);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.UNDO] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.CLEANUP] == 11);


    }

    public void test1() throws TransactionException, IOException, InconsistentStateException {

        JournalTransaction transaction = fileJournalTransactionManager.beginTransaction(this);

        TransactionContext context = new TransactionContext();

        TestJournalAction[] actions = new TestJournalAction[]{
                new TestJournalAction(context),
                new TestJournalAction(context)
        };

        for (JournalAction action : actions) {
            transaction.run(action);
        }

        transaction.rollback();

        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREJOURNALWRITE] == 1);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 2);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.EXECUTE] == 3);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.UNDO] == 8);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.CLEANUP] == 10);

        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREJOURNALWRITE] == 4);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 5);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.EXECUTE] == 6);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.UNDO] == 7);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.CLEANUP] == 9);

        transaction = null;

    }

    public void test2() throws TransactionException, IOException, InconsistentStateException {

        JournalTransaction transaction = fileJournalTransactionManager.beginTransaction(this);

        TransactionContext context = new TransactionContext();

        TestJournalAction[] actions = new TestJournalAction[]{
                new TestJournalAction(context),
                new TestJournalAction(context, TestJournalAction.PREJOURNALWRITE)
        };

        try {
            for (JournalAction action : actions) {
                transaction.run(action);
            }
            transaction.commit();
            assertTrue("sollte einen Fehler geben", false);

        } catch (Exception e) {
            transaction.rollback();
        }

        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREJOURNALWRITE] == 1);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 2);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.EXECUTE] == 3);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.UNDO] == 4);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.CLEANUP] == 5);

        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREJOURNALWRITE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.EXECUTE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.UNDO] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.CLEANUP] == 0);

        transaction = null;

    }

    public void test3() throws TransactionException, IOException, InconsistentStateException {

        JournalTransaction transaction = fileJournalTransactionManager.beginTransaction(this);

        TransactionContext context = new TransactionContext();

        TestJournalAction[] actions = new TestJournalAction[]{
                new TestJournalAction(context),
                new TestJournalAction(context, TestJournalAction.POSTJOURNALWRITE)
        };

        try {
            for (JournalAction action : actions) {
                transaction.run(action);
            }
            transaction.commit();
            assertTrue("sollte einen Fehler geben", false);

        } catch (Exception e) {
            transaction.rollback();
        }

        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREJOURNALWRITE] == 1);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 2);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.EXECUTE] == 3);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.UNDO] == 6);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.CLEANUP] == 8);

        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREJOURNALWRITE] == 4);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.EXECUTE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.UNDO] == 5);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.CLEANUP] == 7);

        transaction = null;

    }

    public void test4() throws TransactionException, IOException, InconsistentStateException {

        JournalTransaction transaction = fileJournalTransactionManager.beginTransaction(this);

        TransactionContext context = new TransactionContext();

        TestJournalAction[] actions = new TestJournalAction[]{
                new TestJournalAction(context),
                new TestJournalAction(context, TestJournalAction.EXECUTE)
        };

        try {
            for (JournalAction action : actions) {
                transaction.run(action);
            }
            transaction.commit();
            assertTrue("sollte einen Fehler geben", false);

        } catch (Exception e) {
            transaction.rollback();
        }

        /**
         * MJA
         * Wenn das Execute fehlschlägt, wird kein
         * UNDO oder CLEANUP auf der Action ausgeführt
         */
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREJOURNALWRITE] == 1);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 2);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.EXECUTE] == 3);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.UNDO] == 6);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.CLEANUP] == 7);

        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREJOURNALWRITE] == 4);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 5);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.EXECUTE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.UNDO] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.CLEANUP] == 0);

        transaction = null;

    }

    public void test5() throws TransactionException, IOException, InconsistentStateException {

        JournalTransaction transaction = fileJournalTransactionManager.beginTransaction(this);

        TransactionContext context = new TransactionContext();

        TestJournalAction[] actions = new TestJournalAction[]{
                new TestJournalAction(context),
                new TestJournalAction(context, TestJournalAction.PREPARE),
                new TestJournalAction(context),
        };

        try {
            for (JournalAction action : actions) {
                transaction.run(action);
            }
            transaction.commit();
            assertTrue("sollte einen Fehler geben", false);

        } catch (Exception e) {
            transaction.rollback();
        }

        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREJOURNALWRITE] == 1);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 2);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.EXECUTE] == 3);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREPARE] == 10);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.UNDO] == 13);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.CLEANUP] == 16);

        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREJOURNALWRITE] == 4);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 5);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.EXECUTE] == 6);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.UNDO] == 12);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.CLEANUP] == 15);

        assertTrue("sollte einen Fehler geben", actions[2].getMethods()[TestJournalAction.PREJOURNALWRITE] == 7);
        assertTrue("sollte einen Fehler geben", actions[2].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 8);
        assertTrue("sollte einen Fehler geben", actions[2].getMethods()[TestJournalAction.EXECUTE] == 9);
        assertTrue("sollte einen Fehler geben", actions[2].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[2].getMethods()[TestJournalAction.UNDO] == 11);
        assertTrue("sollte einen Fehler geben", actions[2].getMethods()[TestJournalAction.CLEANUP] == 14);

        transaction = null;

    }

    public void test6() throws TransactionException, IOException, InconsistentStateException {

        JournalTransaction transaction = fileJournalTransactionManager.beginTransaction(this);

        TransactionContext context = new TransactionContext();

        TestJournalAction[] actions = new TestJournalAction[]{
                new TestJournalAction(context),
                new TestJournalAction(context, TestJournalAction.UNDO)
        };

        try {
            for (JournalAction action : actions) {
                transaction.run(action);
            }

            transaction.rollback();
            assertTrue("sollte einen Fehler geben", false);

        } catch (Exception e){
            /**
             * Diese Transaktion ist in einer Rollback-Aktion fehlgeschlagen.
             * Das ist der schlimmste Fall der eintreten kann.
             * Eigentlich kann man jetzt nur noch versuchen die Transakton
             * zu Dumpen, das Rollback manuell auszuführen und das Journal
             * dann zu löschen.
             */
            transaction.dump(System.out);
        }

        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREJOURNALWRITE] == 1);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 2);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.EXECUTE] == 3);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.UNDO] == 0);
        assertTrue("sollte einen Fehler geben", actions[0].getMethods()[TestJournalAction.CLEANUP] == 0);

        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREJOURNALWRITE] == 4);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.POSTJOURNALWRITE] == 5);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.EXECUTE] == 6);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.PREPARE] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.UNDO] == 0);
        assertTrue("sollte einen Fehler geben", actions[1].getMethods()[TestJournalAction.CLEANUP] == 0);

        transaction = null;

    }

}

