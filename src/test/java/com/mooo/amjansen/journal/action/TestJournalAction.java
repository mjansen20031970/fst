package com.mooo.amjansen.journal.action;


import com.mooo.amjansen.journal.JournalAction;
import com.mooo.amjansen.transaction.TransactionContext;
import com.mooo.amjansen.utils.NUIDGenerator;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 * <p>
 * To change this template use File | Settings | File Templates.
 */
public class TestJournalAction implements JournalAction {

    public final static int PREJOURNALWRITE = 0;
    public final static int POSTJOURNALWRITE = 1;
    public final static int EXECUTE = 2;
    public final static int PREPARE = 3;
    public final static int UNDO = 4;
    public final static int COMMIT = 5;
    public final static int CLEANUP = 6;

    private transient String key = null;
    private transient int exceptionOn = -1;
    private final transient int[] methods = new int[7];
    private transient TransactionContext context = null;

    public TestJournalAction(TransactionContext context) {
        this.context = context;
        this.key = NUIDGenerator.unique();
    }

    public TestJournalAction(TransactionContext context, int exceptionOn) {
        this.exceptionOn = exceptionOn;
        this.context = context;
        this.key = NUIDGenerator.unique();
    }

    public int[] getMethods() {
        return methods;
    }

    private int nextCounter() {
        if (context != null) {
            return context.getCounter();
        }
        return -1;
    }

    @Override
    public void preJournalWrite(Object txContext, File backupDir) throws IOException {
        System.out.println(" --- preJournalWrite  - " + key);

        if (exceptionOn == PREJOURNALWRITE) {
            throw new IOException("preJournalWrite - " + key);
        }
        methods[PREJOURNALWRITE] = nextCounter();
    }

    @Override
    public void postJournalWrite(Object txContext) throws IOException {
        System.out.println(" --- postJournalWrite - " + key);

        if (exceptionOn == POSTJOURNALWRITE) {
            throw new IOException("postJournalWrite - " + key);
        }
        methods[POSTJOURNALWRITE] = nextCounter();
    }

    @Override
    public Object execute(Object txContext) throws IOException {
        System.out.println(" --- execute          - " + key);

        if (exceptionOn == EXECUTE) {
            throw new IOException("execute - " + key);
        }
        methods[EXECUTE] = nextCounter();
        return null;
    }

    @Override
    public void prepare(Object txContext) throws IOException {
        System.out.println(" --- prepare          - " + key);

        if (exceptionOn == PREPARE) {
            throw new IOException("prepare - " + key);
        }
        methods[PREPARE] = nextCounter();
    }

    @Override
    public void undo(Object txContext) throws IOException {
        System.out.println(" --- undo             - " + key);

        if (exceptionOn == UNDO) {
            throw new IOException("undo - " + key);
        }
        if (methods != null) {
            methods[UNDO] = nextCounter();
        }
    }

    @Override
    public void cleanup(Object txContext) throws IOException {
        System.out.println(" --- cleanup          - " + key);

        if (exceptionOn == CLEANUP) {
            throw new IOException("cleanup - " + key);
        }
        if (methods != null) {
            methods[CLEANUP] = nextCounter();
        }
    }

    @Override
    public void commit(Object txContext) throws IOException {
        System.out.println(" --- commit           - " + key);

        if (exceptionOn == COMMIT) {
            throw new IOException("commit - " + key);
        }
        if (methods != null) {
            methods[COMMIT] = nextCounter();
        }

    }

    @Override
    public String toString() {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);

        writer.println("TestJournalAction ------------------------ " + key);

        switch (exceptionOn) {
            case PREJOURNALWRITE :
                writer.println("exceptionOn: PREJOURNALWRITE");
                break;
            case POSTJOURNALWRITE :
                writer.println("exceptionOn: POSTJOURNALWRITE");
                break;
            case EXECUTE :
                writer.println("exceptionOn: EXECUTE");
                break;
            case PREPARE :
                writer.println("exceptionOn: PREPARE");
                break;
            case UNDO :
                writer.println("exceptionOn: UNDO");
                break;
            case COMMIT :
                writer.println("exceptionOn: COMMIT");
                break;
            case CLEANUP :
                writer.println("exceptionOn: CLEANUP");
                break;
            default:
                writer.println("exceptionOn: NONE");
                break;

        }

        writer.println("methods: ");

        writer.println("   PREJOURNALWRITE : " + methods[PREJOURNALWRITE]);
        writer.println("   POSTJOURNALWRITE: " + methods[POSTJOURNALWRITE]);
        writer.println("   EXECUTE         : " + methods[EXECUTE]);
        writer.println("   PREPARE         : " + methods[PREPARE]);
        writer.println("   UNDO            : " + methods[UNDO]);
        writer.println("   COMMIT          : " + methods[COMMIT]);
        writer.println("   CLEANUP         : " + methods[CLEANUP]);

        writer.println("-------------------------------------------------------------------------------------------");

        writer.close();

        return buffer.toString();

    }
}
