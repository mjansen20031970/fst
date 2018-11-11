package com.mooo.amjansen.utils;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 26.09.18
 * Time: 12:30
 * <p>
 * To change this template use File | Settings | File Templates.
 */
public class StreamUtils {

    public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        for (int reed = inputStream.read(buffer);
             reed > 0; reed = inputStream.read(buffer)) {
            outputStream.write(buffer, 0, reed);
        }
    }

    public static void copyStream(Reader reader, Writer writer) throws IOException {
        char[] buffer = new char[8192];
        for (int reed = reader.read(buffer);
             reed > 0; reed = reader.read(buffer)) {
            writer.write(buffer, 0, reed);
        }
    }

    public static File copyFile(File fromFile, File toFile) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {

            StreamUtils.copyStream(
                    inputStream = new BufferedInputStream(new FileInputStream(fromFile)),
                    outputStream = new BufferedOutputStream(new FileOutputStream(toFile)));

            return toFile;

        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) { /* Punt !!! */ }
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) { /* Punt !!! */ }
        }
    }

}
