package com.mooo.amjansen;

import com.mooo.amjansen.utils.NUIDGenerator;
import com.mooo.amjansen.utils.TimeDistanceFormatter;
import junit.framework.TestCase;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public class NUIDGeneratorTest extends TestCase {

    public void testTiming(){

        long endTime = 0;
        long startTime = 0;

        int rounds = 1000;

        long elapsed = 0;

        for (int i=0; i<rounds; i++){

            startTime = System.nanoTime();
            String id = NUIDGenerator.unique();
            endTime = System.nanoTime();

            elapsed += endTime -startTime;
        }

        System.out.println("--> average time: " + TimeDistanceFormatter.nanoSecondsToString(elapsed / rounds ));

    }

    public static void main(String[] args) {

        long endTime = 0;
        long startTime = 0;

        while (true) {
            startTime = System.nanoTime();
            String id = NUIDGenerator.unique();
            endTime = System.nanoTime();
            System.out.println("--> " + id + ", elapsed: " + TimeDistanceFormatter.nanoSecondsToString(endTime - startTime));
        }

    }

}
