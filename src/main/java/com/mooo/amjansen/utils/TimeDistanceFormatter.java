package com.mooo.amjansen.utils;

import java.text.DecimalFormat;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public class TimeDistanceFormatter {

    public static final String DELIM = ":";
    public static final String DELIMPOINT = ".";

    /**
     * Formatiert die übergebenen Nano-Sekunden in das
     * folgende Format:
     *    00:00:00.000.000.000
     *
     * @param time
     * @return
     */
    public static String nanoSecondsToString(long time) {
        long hour = (long) (time / 3600000000000L);
        time = time - (hour * 3600000000000L);
        long min = (long) (time / 60000000000L);
        time = time - (min * 60000000000L);
        long sec = (long) (time / 1000000000L);
        time = time - (sec * 1000000000L);
        long millisec = (long) (time / 1000000L);
        time = time - (millisec * 1000000L);
        long microsec = (long) (time / 1000L);
        time = time - (microsec * 1000L);
        long nanosec = time;

        return (new DecimalFormat("00")).format(hour) + DELIM +
                (new DecimalFormat("00")).format(min) + DELIM +
                (new DecimalFormat("00")).format(sec) + DELIMPOINT +
                (new DecimalFormat("000")).format(millisec) + DELIMPOINT +
                (new DecimalFormat("000")).format(microsec) + DELIMPOINT +
                (new DecimalFormat("000")).format(nanosec);
    }

    /**
     * Formatiert die übergebenen Milli-Sekunden in das
     * folgende Format:
     *    00:00:00.000
     *
     * @param time
     * @return
     */
    public static String milliSecondsToString(long time) {
        long hour = (long) (time / 3600000);
        time = time - (hour * 3600000);
        long min = (long) (time / 60000);
        time = time - (min * 60000);
        long sec = (long) (time / 1000);
        time = time - (sec * 1000);
        long millisec = time;

        return (new DecimalFormat("00")).format(hour) + DELIM +
                (new DecimalFormat("00")).format(min) + DELIM +
                (new DecimalFormat("00")).format(sec) + DELIMPOINT +
                (new DecimalFormat("000")).format(millisec);
    }

}
