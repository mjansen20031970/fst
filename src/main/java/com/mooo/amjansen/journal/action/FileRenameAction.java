package com.mooo.amjansen.journal.action;

import com.mooo.amjansen.journal.JournalAction;
import com.mooo.amjansen.utils.NUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class FileRenameAction implements JournalAction {

    private static Logger logger = LoggerFactory.getLogger(FileRenameAction.class);

    private boolean readCommited;
    private File toTmpFile;
    private File fromFile;
    private File toFile;

    public FileRenameAction(File fromFile, File toFile, boolean readCommited) {
        this.readCommited = readCommited;
        this.toFile = toFile.getAbsoluteFile();
        this.fromFile = fromFile.getAbsoluteFile();
        this.toTmpFile = new File(toFile.getParent(),
            toFile.getName()+"."+ NUIDGenerator.unique()+".renamed").getAbsoluteFile();
    }

    @Override
    public void preJournalWrite(Object txContext, File backupDir) throws IOException {
        if (fromFile.exists()==false)
            throw new IOException("file '"+
                fromFile.getAbsolutePath()+"' doesnt't exist");
        if (toFile.exists()==true)
            throw new IOException("file '"+
                toFile.getAbsolutePath()+"' already exists");
    }

    @Override
    public void postJournalWrite(Object txContext) throws IOException {
    }

    @Override
    public void prepare(Object txContext) throws IOException {
    }

    @Override
    public void undo(Object txContext) throws IOException {
        if (readCommited==true){
            if (toTmpFile.exists()==false){
                if (fromFile.exists()==true){
                    /**
                     * Alles in Ordnung das rename brauch
                     * nicht mehr gemacht werden, da der
                     * Ausgangszustand schon eingenommen wurde.
                     */
                } else {
                    /**
                     * Wenn die temporäre Datei nicht vorhanden ist,
                     * wurde sie entweder böswillig gelöscht oder
                     * aber das rename hat nie statt gefunden.
                     */
                    logger.error("unexpectedly can't find file '"+toTmpFile.getAbsolutePath()+"'");

                }

            } else if (toTmpFile.renameTo(fromFile)==false){

                throw new IOException("can't rename file "+
                    toTmpFile.getAbsolutePath()+" to "+ fromFile.getAbsolutePath());
            }

        } else {

            if (toFile.exists()==false){
                if (fromFile.exists()==true){
                    /**
                     * Alles in Ordnung das rename brauch
                     * nicht mehr gemacht werden, da der
                     * Ausgangszustand schon eingenommen wurde.
                     */
                } else {
                    /**
                     * Wenn die temporäre Datei nicht vorhanden ist,
                     * wurde sie entweder böswillig gelöscht oder
                     * aber das rename hat nie statt gefunden.
                     */
                    logger.error("unexpectedly can't find file '"+toFile.getAbsolutePath()+"'");

                }

            } else if (toFile.renameTo(fromFile)==false){

                throw new IOException("can't rename file "+
                    toFile.getAbsolutePath()+" to "+ fromFile.getAbsolutePath());
            }
        }
    }

    @Override
    public void cleanup(Object txContext) throws IOException {
    }

    @Override
    public Object execute(Object txContext) throws IOException {
        if (readCommited==true){
            if (fromFile.renameTo(toTmpFile)==false){
                throw new IOException("can't rename file "+
                    fromFile.getAbsolutePath()+" to "+ toTmpFile.getAbsolutePath());
            }
            return toTmpFile;
        } else {
            if (fromFile.renameTo(toFile)==false){
                throw new IOException("can't rename file "+
                    fromFile.getAbsolutePath()+" to "+ toFile.getAbsolutePath());
            }
            return toFile;
        }
    }

    public void commit(Object txContext) throws IOException {
        if (readCommited==true){
            if (toTmpFile.renameTo(toFile)==false){
                throw new IOException("can't rename file "+
                    toTmpFile.getAbsolutePath()+" to "+ toFile.getAbsolutePath());
            }
        }
    }
}
