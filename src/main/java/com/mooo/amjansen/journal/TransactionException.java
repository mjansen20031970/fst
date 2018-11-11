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
public class TransactionException extends Exception {
	Exception nestedException2;

	public TransactionException() {}
	public TransactionException(Exception e) { super(e); }
	public TransactionException(Exception e1, Exception e2) { 
		super(e1);
		nestedException2 = e2;
	}

}
