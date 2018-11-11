package com.mooo.amjansen.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 *
 * To change this template use File | Settings | File Templates.
 *
 */
public class NUIDGenerator {

    private static Logger logger = LoggerFactory.getLogger(NUIDGenerator.class);

    public static final int UNIQUE_LENGTH = 48;


    private static int pid = 0;
    private static int counter = 0;
    private static char[] pattern = null;
    private static char[] patternRLE = null;
    private static byte[] addrBytes = null;

    private static char[] table = new char[]{
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F'
    };

    /**
     * Liefert die aktuelle Prozess-Id
     *
     * @return pid
     */
    private static int getPID() {

        if (NUIDGenerator.pid > 0)
            return NUIDGenerator.pid;

        synchronized (NUIDGenerator.class) {

            if (NUIDGenerator.pid > 0)
                return NUIDGenerator.pid;

            String name = ManagementFactory.getRuntimeMXBean().getName();

            if ((name == null) || (name.length() == 0)) {
                logger.error(" unexpectedly get no process-name");
                return 0;
            }

            int index = name.indexOf('@');
            if (index < 0) {
                logger.error("unexpected process-name: '" + name + "'");
                return 0;
            }

            return NUIDGenerator.pid = Integer.parseInt(name.substring(0, index));

        }

    }

    /**
     * Liefert die Netzwerk-Adresse. Unabhängig davon
     * ob IPv4 oder IPv6 verwendet wird, ist das gelieferte
     * Ergebnis immer ein Array der Länge 16.
     *
     * @return
     */
    private static byte[] getAddrBytes() {

        if (addrBytes != null)
            return addrBytes;

        synchronized (NUIDGenerator.class) {
            if (addrBytes != null)
                return addrBytes;

            try {
                byte[] temp = new byte[16];
                byte[] addr = InetAddress.
                        getLocalHost().getAddress();

                for (int i = addr.length - 1; i >= 0; i--) {
                    temp[i] = addr[i];
                }

                return addrBytes = temp;

            } catch (UnknownHostException e) {
                logger.error("can't get host-address", e);
                return addrBytes = new byte[16];
            }
        }
    }

    /**
     * Liefert den hinteren statischen Teil des Schlüssels
     *
     * @return
     */
    private static char[] getPattern() {
        if (pattern != null)
            return pattern.clone();

        synchronized (NUIDGenerator.class) {

            if (pattern != null)
                return pattern.clone();

            char buf[] = new char[48];

            /**
             * 4 Zeichen
             *
             * 12 - 15
             *
             */
            int pid = getPID();
            for (int i = 0; i < 4; i++) {
                buf[i + 12] = table[(int) (pid & 0x0F)];
                pid >>= 4;
            }

            /**
             * 32 Zeichen
             *
             * 16 - 47
             */
            byte[] addrBytes = getAddrBytes();
            for (int t = 0; t < addrBytes.length; t++) {
                buf[16 + (t * 2)] = table[(int) (addrBytes[t] & 0x0F)];
                buf[16 + (t * 2) + 1] = table[(int) ((addrBytes[t] >> 4) & 0x0F)];
            }

            return (pattern = buf).clone();

        }

    }

    /**
     * Führt eine RLE auf die übergebene Zeichenkette durch.
     * Die Zeichenkette darf nur folgende Zeichen enthalten (0-9 A-F).
     * Prinzip bedingt ist die resultierende Länge unbestimmt.
     *
     * @param str Zeichenkette, die komprimiert werden soll
     * @param off Offset in die Zeichenkette
     * @param len Länge der Zeichenkette, die komprimiert werden sollen
     * @return Die komprimierte Zeichenkette. Alle Zeichen vor dem Offset
     * werden kopiert.
     */
    public static char[] encode(char[] str, int off, int len) {

        int count = 0;
        int pptr = off;
        char[] encoding = new char[str.length];

        for (int i = off; i < len; i++) {

            count = 1;
            while (i + 1 < len && str[i] == str[i + 1]) {
                count++;
                i++;
            }

            if (count > 2) {
                while (count > 20) {
                    encoding[pptr++] = 'F' + 20;
                    count -= 20;
                }

                encoding[pptr++] = (char) ('F' + count);
                encoding[pptr++] = str[i];

            } else {

                while (count > 0) {
                    encoding[pptr++] = str[i];
                    count--;
                }

            }
        }

        char[] tmp = new char[pptr];
        System.arraycopy(encoding, 0, tmp, 0, tmp.length);
        return tmp;
    }

    /**
     * Liefert den hinteren statischen Teil des Schlüssels,
     * wobei der relevante Teil durch RLE komprimiert wird.
     *
     * @return Zeichenkette, die als Schablone für den resultierenden
     * eindeutigen fungiert-
     */
    private static char[] getPatternRLE() {
        if (patternRLE != null)
            return patternRLE.clone();

        synchronized (NUIDGenerator.class) {

            if (patternRLE != null)
                return patternRLE.clone();

            char buf[] = new char[48];

            /**
             * 4 Zeichen
             *
             * 12 - 15
             *
             */
            int pid = getPID();
            for (int i = 0; i < 4; i++) {
                buf[i + 12] = table[(int) (pid & 0x0F)];
                pid >>= 4;
            }

            /**
             * 32 Zeichen
             *
             * 16 - 47
             */
            byte[] addrBytes = getAddrBytes();
            for (int t = 0; t < addrBytes.length; t++) {
                buf[16 + (t * 2)] = table[(int) (addrBytes[t] & 0x0F)];
                buf[16 + (t * 2) + 1] = table[(int) ((addrBytes[t] >> 4) & 0x0F)];
            }

            buf = encode(buf, 12, buf.length);

            return (patternRLE = buf).clone();

        }

    }

    /**
     * Füllt den übergebenen Puffer mit den restlichen Werten
     * des Schlüssels
     *
     * @param buf
     * @param counter
     * @param time
     * @return
     */
    public static String unique(char buf[], int counter, long time) {

        /**
         * 4 Zeichen
         * 0 - 3
         */
        for (int i = 0; i < 4; i++) {
            buf[i] = table[(int) (counter & 0x0F)];
            counter >>= 4;
        }

        /**
         * 8 Zeichen
         *
         * 4 - 11
         */
        for (int i = 0; i < 8; i++) {
            buf[i + 4] = table[(int) (time & 0x0F)];
            time >>= 4;
        }

        return new String(buf);

    }

    /**
     * Erzeugt eine netzwerkweite eindeutige Zeichenkette
     * wobei die am höchsten relevanten Daten am Anfang stehen,
     * um eine Verwendung als Index zu verbessern.
     * Es werden nur folgende Zeichen verwendet (0-9 A-F),
     * damit keine Komplikationen wegen einer Case-Sensitivität
     * entstehen kann. Die Länge ist konstant UNIQUE_LENGTH.
     *
     * @return
     */
    public static String unique() {

        synchronized (NUIDGenerator.class) {
            int c = counter;
            counter++;
            if (counter >= 65536)
                counter = 0;
        }

        return unique(getPattern(), counter, System.currentTimeMillis());
    }

    /**
     * Erzeugt eine netzwerkweite eindeutige Zeichenkette
     * wobei die am höchsten relevanten Daten am Anfang stehen,
     * um eine Verwendung als Index zu verbessern.
     * Es werden nur folgende Zeichen verwendet (0-9 A-Z),
     * damit keine Komplikationen wegen einer Case-Sensitivität
     * entstehen kann. Die Länge ist nicht definiert aber kürzer
     * als UNIQUE_LENGTH.
     *
     * @return
     */
    public static String uniqueRLE() {

        synchronized (NUIDGenerator.class) {
            int c = counter;
            counter++;
            if (counter >= 65536)
                counter = 0;
        }

        return unique(getPatternRLE(), counter, System.currentTimeMillis());
    }

}