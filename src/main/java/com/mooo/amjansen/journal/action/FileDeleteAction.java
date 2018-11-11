package com.mooo.amjansen.journal.action;

import com.mooo.amjansen.journal.JournalAction;
import com.mooo.amjansen.utils.NUIDGenerator;

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
public class FileDeleteAction implements JournalAction {

	private static final long serialVersionUID = 0L;

    private File existingfile;
    private File deletedFile;

    public FileDeleteAction(File file, boolean readCommited) {
        this.existingfile = file.getAbsoluteFile();
        this.deletedFile = new File(file.getParent(),
            file.getName()+"."+ NUIDGenerator.unique()+".deleted").getAbsoluteFile();
    }

    @Override
    public void preJournalWrite(Object txContext, File backupDir) throws IOException {
    }

    @Override
    public void postJournalWrite(Object txContext) throws IOException {
    }

    @Override
    public void prepare(Object txContext) throws IOException {

    }

    @Override
    public void undo(Object txContext) throws IOException {
        if (deletedFile.renameTo(existingfile)==false){
            throw new IOException("can't rename file "+
                deletedFile.getAbsolutePath()+" to "+existingfile.getAbsolutePath());
        }
    }

    @Override
    public void cleanup(Object txContext) throws IOException {
        if ((deletedFile.exists()==true)&&(deletedFile.delete()==false)){
            throw new IOException("Can't delete file "+deletedFile.getAbsolutePath());
        }
    }

    @Override
    public Object execute(Object txContext) throws IOException {
        if (existingfile.renameTo(deletedFile)==false){
            throw new IOException("can't rename file "+
                existingfile.getAbsolutePath()+" to "+deletedFile.getAbsolutePath());
        }
        return deletedFile;
    }

    public void commit(Object txContext) throws IOException {
        if ((deletedFile.exists()==true)&&(deletedFile.delete()==false)){
            throw new IOException("Can't delete file "+deletedFile.getAbsolutePath());
        }
    }
}
