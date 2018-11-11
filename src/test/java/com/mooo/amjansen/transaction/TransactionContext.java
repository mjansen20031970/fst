package com.mooo.amjansen.transaction;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public class TransactionContext {

    private int counter = 1 ;

    public int getCounter(){
        return counter++;
    }

}
