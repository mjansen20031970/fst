/*
 * Created on 12.01.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
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
public class InconsistentStateException extends Exception {
	/**
	 * @param cause
	 */
	public InconsistentStateException(Throwable cause) {
		super(cause);
	}

    public InconsistentStateException(String message) {
        super(message);
    }

	/**
	 * @param message
	 * @param cause
	 */
	public InconsistentStateException(String message, Throwable cause) {
		super(message, cause);
	}

}
