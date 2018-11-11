package com.mooo.amjansen.journal;

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
public interface Journal {

    void dump(PrintStream writer);

    Object run(JournalAction a) throws IllegalStateException, IOException, TransactionException, InconsistentStateException;

    Object cancelAndRun(CancelableJournalAction canceled, CancelableJournalAction a) throws IllegalStateException, IOException, TransactionException, InconsistentStateException;

}
