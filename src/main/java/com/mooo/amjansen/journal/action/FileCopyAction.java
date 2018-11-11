package com.mooo.amjansen.journal.action;

import com.mooo.amjansen.journal.JournalAction;
import com.mooo.amjansen.utils.NUIDGenerator;
import com.mooo.amjansen.utils.StreamUtils;

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
public class FileCopyAction implements JournalAction {

    private boolean readCommited;
    private File deletedFile;
    private File toTmpFile;
    private File fromFile;
    private File toFile;

    public FileCopyAction(File fromFile, File toFile, boolean readCommited) {
        this.readCommited = readCommited;
        this.toFile = toFile.getAbsoluteFile();
        this.fromFile = fromFile.getAbsoluteFile();
        this.toTmpFile = new File(toFile.getParent(),
            toFile.getName()+"."+ NUIDGenerator.unique()+".tmp").getAbsoluteFile();
        this.deletedFile = new File(toFile.getParent(),
            toFile.getName()+"."+ NUIDGenerator.unique()+".deleted").getAbsoluteFile();
    }

    @Override
    public void preJournalWrite(Object txContext, File backupDir) throws IOException {
    }

    @Override
    public void postJournalWrite(Object txContext) throws IOException {
    }

    @Override
    public void prepare(Object txContext) throws IOException {
        if (toFile.exists()==true)
            throw new IOException("file '"+toFile.getAbsolutePath()+"' already exists");
    }

    @Override
    public void undo(Object txContext) throws IOException {
        if (toTmpFile.exists()==true){
            toTmpFile.renameTo(deletedFile);
            deletedFile.delete();
        }
    }

    @Override
    public void cleanup(Object txContext) throws IOException {
    }

    @Override
    public Object execute(Object txContext) throws IOException {
        if (toFile.exists()==true)
            throw new IOException("file '"+toFile.getAbsolutePath()+"' already exists");
        if (toTmpFile.exists()==true)
            throw new IOException("file '"+toTmpFile.getAbsolutePath()+"' already exists");
        return StreamUtils.copyFile(fromFile, toTmpFile);
    }

    public void commit(Object txContext) throws IOException {
        if (toTmpFile.renameTo(toFile)==false){
            if (toTmpFile.exists()==false)
                throw new IOException("can't rename file '"+
                    toTmpFile.getAbsolutePath()+"' to '"+ toFile.getAbsolutePath()+
                    "', source doesn't exists");

            if (toFile.exists()==true)
                throw new IOException("can't rename file '"+
                    toTmpFile.getAbsolutePath()+"' to '"+ toFile.getAbsolutePath()+
                    "', destination already exists");

            throw new IOException("can't rename file '"+
                toTmpFile.getAbsolutePath()+"' to '"+ toFile.getAbsolutePath()+"'");
        }
    }

}
