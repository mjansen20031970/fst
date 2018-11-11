/*
 * Created on 12.01.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.mooo.amjansen.journal;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public interface JournalAction extends Serializable {

	/**
	 * Diese Methode kann dazu verwendet werden, um
     * die Action so zu initialisieren, dass ein
     * ewentuelles Rollback erfolgreich ausgeführt
     * werden kann.
     *
     * Die Methode wird als Erste aufgerufen.
	 * 
	 * @throws IOException Wenn diese Methode eine Exception
     * auswirft wird ein Rollback ausgeführt wobei nicht das
     * korispondirende undo() dieser Instanz angesprungen wird.
	 */
	public void preJournalWrite(Object txContext, File backupDir) throws IOException ;

    /**
     * Diese Methode wird direkt nach dem Schreiben des Journals
     * aufgerufen. Sie kann dazu verwendet werden, Vorkehrungen
     * für ein eventuelles Rollback.
     *
     * @throws IOException Wenn diese Methode eine Exception
     * auswirft wird ein Rollback und das zugehörige undo()
     * dieser Instanz ausgeführt.
     */
	public void postJournalWrite(Object txContext) throws IOException ;

    /**
     * Diese Methode wird direkt nach der erfolgreichen Ausführung
     * von postJournalWrite ausgeführt.
     *
     * @param txContext
     * @return Ein Anwendungsbezogenes Objekt
     * @throws IOException Wenn diese Methode eine Exception
     * auswirft wird ein Rollback und das zugehörige undo()
     * dieser Instanz ausgeführt
     */
	public Object execute(Object txContext) throws IOException ;

    /**
     * Diese Methode wird direkt vor dem commit() aufgerufen.
     * Diese Instanze muss darauf vorbereitet sein, dass
     * trotz erfolgreicher Ausführung dieser Methode trotzdem
     * ein Aufruf der undo()-Methode dieser Instanz erfolgen
     * kann.
     *
     * @param txContext
     * @throws IOException Wenn diese Methode eine Exception
     * auswirft wird ein Rollback und das zugehörige undo()
     * dieser Instanz ausgeführt
     */
    public void prepare(Object txContext) throws IOException;

	/**
	 * Diese Methode wird aufgerufen, wenn ein Rollback durchgeführt wird.
     * Es wird garantiert, das diese Instanze mindestens den Zustand
     * hat, den es hatte, nachdem preJournalWrite() aufgerufen wurde.
     * Es sollten alle Aktionen, die in execute() durchgeführt wurden
     * wieder rückgängig gemacht werden. 
     *
	 * @throws IOException
	 */
	public void undo(Object txContext) throws IOException ;

	public void commit(Object txContext) throws IOException;

	/**
	 * Diese Methode wird aufgerufen, wenn alle Aktionen durchgeführt
	 * wurden. Sie muss daführ sorgen, dass alle Ressourcen
	 * die innerhalb dieser Transaktion alloziert wurden gelöscht
	 * oder geschlossen wurden
	 *
	 * @param txContext
	 * @throws IOException
	 */
	public void cleanup(Object txContext) throws IOException;

}
