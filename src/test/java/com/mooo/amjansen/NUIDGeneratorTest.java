package com.mooo.amjansen;

import com.mooo.amjansen.utils.NUIDGenerator;
import com.mooo.amjansen.utils.TimeDistanceFormater;
import junit.framework.TestCase;

public class NUIDGeneratorTest extends TestCase {

    public static void main(String[] args) {

        long endTime = 0;
        long startTime = 0;

        while (true) {
            startTime = System.nanoTime();
            String id = NUIDGenerator.unique();
            endTime = System.nanoTime();
            System.out.println("--> " + id + ", elapsed: " + TimeDistanceFormater.nanoSecondsToString(endTime - startTime));
        }

    }

}
