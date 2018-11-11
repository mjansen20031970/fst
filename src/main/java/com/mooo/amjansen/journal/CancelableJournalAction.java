package com.mooo.amjansen.journal;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public interface CancelableJournalAction extends JournalAction {

    /**
     * Mit dieser Methode wird diese JournalAction aufgehoben.
     */
    public void cancel();

    /**
     * Diese Methode liefert die Id der JournalAction
     * die von dieser JournalAction aufgehoben werden
     * soll.Wenn diese JournalAction keine Andere
     * aufheben will, muss sie -1 liefern.
     *
     * @return
     */
    public int getCancelId();

    /**
     * Mit dieser Methode wird die Id der JournalAction,
     * die von dieser JournalAction aufgehoben werden
     * soll. Der Wert sollte als nicht transienter Wert
     * in der JournalAction gespeichert werden.
     *
     * @param id
     */
    public void setCancelId(int id, CancelableJournalAction canceledAction);

}
