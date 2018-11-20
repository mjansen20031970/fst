package com.mooo.amjansen.nio;

import com.mooo.amjansen.utils.StreamUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created with IntelliJ IDEA.
 * User: matthias
 * Date: 29.08.13
 * Time: 17:11
 * To change this template use File | Settings | File Templates.
 */
public class NIORandomAccessFile {

    private long position = 0;

    private ByteBuffer readBuffer = null;
    private ByteBuffer writeBuffer = null;

    private RandomAccessFile raf = null;

    private FileChannel fileChannel = null;

    public NIORandomAccessFile(File file) throws IOException {
        this(file, 8192);
    }

    public NIORandomAccessFile(File file, int bufferSize) throws IOException {

        if (!file.exists()) {
            boolean created = file.createNewFile();
            if (!created) {
                throw new IOException("Failed to create file: " + file);
            }
        }

        this.raf = new RandomAccessFile(file, "rw");
        this.fileChannel = raf.getChannel();

        this.readBuffer = ByteBuffer.allocate(bufferSize);
        this.writeBuffer = ByteBuffer.allocate(bufferSize);

    }

    public void sync(boolean metaData) throws IOException {
        fileChannel.force(metaData);
    }

    public long size() throws IOException {
        return fileChannel.size();
    }

    public long position() {
        return position;
    }

    public void position(long position) throws IOException {
        this.fileChannel.position(position);
        this.writeBuffer.position(0);
        this.readBuffer.position(0);
        this.position = position;
    }

    public int read() throws IOException {
        if ((readBuffer.hasRemaining() == false) && (sync() > 0))
            return -1;
        position++;
        return readBuffer.get();
    }

    public void write(int b) throws IOException {
        if (writeBuffer.remaining() <= 0)
            flush();
        position++;
        writeBuffer.put((byte) (b & 0xff));
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int reedBytes = 0;
        while (len > 0) {
            if ((readBuffer.hasRemaining() == false) && (sync() < 0))
                return (reedBytes > 0) ? reedBytes : -1;

            int remainingBytes = Math.min(len, readBuffer.remaining());
            readBuffer.get(b, off, remainingBytes);
            reedBytes += remainingBytes;
            position += remainingBytes;
            position += remainingBytes;
            off += remainingBytes;
            len -= remainingBytes;
        }
        return reedBytes;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            if (writeBuffer.remaining() <= 0)
                flush();

            int remainingBytes = Math.min(len, writeBuffer.remaining());
            writeBuffer.put(b, off, remainingBytes);
            off += remainingBytes;
            len -= remainingBytes;
        }
    }

    private int sync() throws IOException {
        readBuffer.flip();
        int reedBytes = fileChannel.read(readBuffer);
        readBuffer.flip();
        return reedBytes;
    }

    public void flush() throws IOException {
        long length = writeBuffer.position();
        if (length > 0) {
            writeBuffer.flip();
            fileChannel.write(writeBuffer);
            writeBuffer.flip();
            writeBuffer.rewind();
        }
    }

    public void close() throws IOException {

        flush();

        try {
            if (fileChannel != null)
                fileChannel.close();
            fileChannel = null;
        } catch (IOException e) { /* Punt !!! */ }

        try {
            if (raf != null)
                raf.close();
            raf = null;
        } catch (IOException e) { /* Punt !!! */ }

    }

    public OutputStream getOutputStream(final long writePosition) {
        return new OutputStream() {

            private long position = writePosition;
            private ByteBuffer buffer = ByteBuffer.allocate(8192);


            @Override
            public void write(int b) throws IOException {
                if (buffer.remaining() <= 0)
                    flush();
                buffer.put((byte) (b & 0xff));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                while (len > 0) {
                    if (buffer.remaining() <= 0)
                        flush();

                    int remainingBytes = Math.min(len, buffer.remaining());
                    buffer.put(b, off, remainingBytes);
                    off += remainingBytes;
                    len -= remainingBytes;
                }
            }

            @Override
            public void flush() throws IOException {
                long length = buffer.position();
                if (length > 0) {
                    buffer.flip();
                    position += fileChannel.write(buffer, position);
                    buffer.flip();
                    buffer.rewind();
                }
            }

            @Override
            public void close() throws IOException {
                flush();
            }
        };
    }

    public InputStream getInputStream(final long readPosition) {
        return new InputStream() {

            private long position = readPosition;

            private ByteBuffer buffer = ByteBuffer.allocate(8192);

            public int read() throws IOException {
                if ((buffer.hasRemaining() == false) && (sync() > 0))
                    return -1;

                return buffer.get();
            }

            private int sync() throws IOException {
                buffer.flip();
                int reedBytes = fileChannel.read(buffer, position);
                position += reedBytes;
                buffer.flip();
                return reedBytes;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int reedBytes = 0;
                while (len > 0) {
                    if ((buffer.hasRemaining() == false) && (sync() < 0))
                        return (reedBytes > 0) ? reedBytes : -1;

                    int remainingBytes = Math.min(len, buffer.remaining());
                    buffer.get(b, off, remainingBytes);
                    reedBytes += remainingBytes;
                    off += remainingBytes;
                    len -= remainingBytes;
                }
                return reedBytes;
            }
        };

    }

    public static void main(String[] args) throws IOException {
        NIORandomAccessFile f = new NIORandomAccessFile(new File("test.dat"));

        PrintStream printStream = new PrintStream(f.getOutputStream(0));
        for (int i = 0; i < 1024; i++) {
            printStream.println("0123456789ABCDEF");
        }
        printStream.close();


        InputStream inputStream = f.getInputStream(0);

        StreamUtils.copyStream(inputStream, System.out);

        System.out.flush();

        f.close();
    }

}
