package com.mooo.amjansen.nio;

import com.mooo.amjansen.utils.NUIDGenerator;
import com.mooo.amjansen.utils.TimeDistanceFormatter;
import junit.framework.TestCase;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;

public class FileLockManagerTest extends TestCase {

    private FileLockManagerGuard manager = null;

    public void setUp() throws IOException {
        File file = new File("transaction.lock");
        manager = new FileLockManagerGuard(file, 512, false);
    }

    public void tearDown() throws IOException {
        manager.close();
        manager = null;
    }

    public static class TestThread extends Thread {
        private FileLockManagerGuard manager  = null;
        private int rounds = 0;

        public TestThread(FileLockManagerGuard manager, int rounds) {
            this.manager = manager;
            this.rounds = rounds;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < rounds; i++) {
                    FileLock lock = manager.openFileLock("key" + i);
                    lock.acquire(false);
                    lock.release();
                    manager.closeFileLock(lock);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void test_0() throws IOException {

        FileLock lock1 = manager.openFileLock("test");
        lock1.acquire(false);

        FileLock lock2 = manager.openFileLock("test");
        lock2.acquire(false);

        lock1.release();
        lock1.close();

        lock2.release();
        lock2.close();

    }

    public void test_00() throws IOException {

        FileLock lock1 = manager.openFileLock("test");
        lock1.acquire(true);

        FileLock lock2 = manager.openFileLock("test");
        lock2.acquire(true);

        lock1.release();
        lock1.close();

        lock2.release();
        lock2.close();

    }

    public void test_1() throws IOException, InterruptedException {

        new Thread(){
            @Override
            public void run() {
                FileLock lock2 = manager.openFileLock("test");
                try {

                    lock2.acquire(true);

                    for (int i=0; i < 10; i++){
                        System.out.println("waiting --- " + i);
                        Thread.sleep(1000);
                    }

                    lock2.release();
                    lock2.close();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }.start();

        Thread.sleep(1000);

        FileLock lock1 = manager.openFileLock("test");
        lock1.acquire(false);
        System.out.println("got the lock");
        lock1.release();
        lock1.close();

    }

    public void test_2() throws IOException, InterruptedException {

        new Thread(){
            @Override
            public void run() {
                FileLock lock2 = manager.openFileLock("test");
                try {

                    lock2.acquire(false);

                    for (int i=0; i < 10; i++){
                        System.out.println("waiting --- " + i);
                        Thread.sleep(1000);
                    }

                    lock2.release();
                    lock2.close();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }.start();

        Thread.sleep(1000);

        FileLock lock1 = manager.openFileLock("test");
        lock1.acquire(false);
        System.out.println("got the lock");
        lock1.release();
        lock1.close();

    }

    public void test_3() throws IOException, InterruptedException {

        new Thread(){
            @Override
            public void run() {
                FileLock lock2 = manager.openFileLock("test");
                try {

                    lock2.acquire(false);

                    for (int i=0; i < 10; i++){
                        System.out.println("waiting --- " + i);
                        Thread.sleep(1000);
                    }

                    lock2.release();
                    lock2.close();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }.start();

        Thread.sleep(1000);

        FileLock lock1 = manager.openFileLock("test");
        lock1.acquire(true);
        System.out.println("got the lock");
        lock1.release();
        lock1.close();

    }

    public void Xtest_0() throws IOException {

        int keys = 1000;
        int rounds = 100;

        for (int t= 0; t < rounds; t++) {

            long startTime = System.nanoTime();

            for (int i = 0; i < keys; i++) {
                FileLock lock = manager.openFileLock(NUIDGenerator.unique());
                lock.acquire(false);
                lock.release();
                lock.close();
            }

            long endTime = System.nanoTime();

            System.out.printf("round %03d  --- elapsed: %s %d%n", t, TimeDistanceFormatter.nanoSecondsToString(endTime - startTime), manager.length());
        }

    }

    public void Xtest_z() throws IOException {

        int keys = 1000;
        int rounds = 100;

        for (int t= 0; t < rounds; t++) {

            long startTime = System.nanoTime();

            for (int i = 0; i < keys; i++) {
                FileLock lock = manager.openFileLock("key" + i);
                lock.acquire(false);
                lock.release();
                lock.close();
            }

            long endTime = System.nanoTime();

            System.out.printf("round %03d  --- elapsed: %s %d%n", t, TimeDistanceFormatter.nanoSecondsToString(endTime - startTime), manager.length());
        }

    }

}
