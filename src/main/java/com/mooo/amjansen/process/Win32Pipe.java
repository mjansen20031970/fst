package com.mooo.amjansen.process;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.17
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public class Win32Pipe {

    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private WinNT.HANDLEByReference readHandle = new WinNT.HANDLEByReference();
    private WinNT.HANDLEByReference writeHandle = new WinNT.HANDLEByReference();


    public Win32Pipe(){

        WinBase.SECURITY_ATTRIBUTES saAttr = new WinBase.SECURITY_ATTRIBUTES();
        saAttr.dwLength = new WinDef.DWORD(saAttr.size());
        saAttr.bInheritHandle = true;
        saAttr.lpSecurityDescriptor = null;

        if (Kernel32.INSTANCE.CreatePipe(readHandle, writeHandle, saAttr, 0)==false){
            System.err.println(Kernel32.INSTANCE.GetLastError());
        }
    }

    public WinNT.HANDLEByReference getReadHandle() {
        return readHandle;
    }

    public WinNT.HANDLEByReference getWriteHandle() {
        return writeHandle;
    }

    public InputStream getInputStream() {
        if (inputStream!=null){
            return inputStream;
        } else return new InputStream(){
            IntByReference dwRead = new IntByReference();
            IntByReference dwWritten = new IntByReference();
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            Pointer data = Native.getDirectBufferPointer(buf);
            boolean bSuccess = true;

            {
                buf.flip();
            }

            @Override
            public int read() throws IOException {
                if ((buf.remaining()<=0)&&(sync()<0)){
                    return -1;
                }
                return buf.get();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int reedBytes = 0;
                while (len>0){
                    if ((buf.remaining()<=0)&&(sync()<0)){
                        return -1;
                    }
                    int bytes = Math.min(len, buf.remaining());
                    buf.get(b,off,bytes);
                    reedBytes+=bytes;
                    off+=bytes;
                    len-=bytes;
                }
                return reedBytes;
            }

            private int sync(){
                buf.flip();
                buf.clear();
                int r = buf.remaining();
                int p = buf.position();
                bSuccess = Kernel32.INSTANCE.ReadFile( readHandle.getValue(), buf.array(), 1024, dwRead, null);
                if( ( bSuccess==false ) || (dwRead.getValue() == 0 )) {
                    // 109 --- Broken Pipe
                    System.err.println(Kernel32.INSTANCE.GetLastError());
                    return -1;
                } else {
                    buf.flip();
                    return dwRead.getValue();
                }
            }

            @Override
            public void close() throws IOException {
                if ((readHandle!=null)&&(Kernel32.INSTANCE.CloseHandle(readHandle.getValue())==false)){
                    System.err.println(Kernel32.INSTANCE.GetLastError());
                } else readHandle = null;
            }
        };
    }

    public OutputStream getOutputStream() {
        if (outputStream!=null){
            return outputStream;
        } else return outputStream = new OutputStream() {

            boolean bSuccess = true;
            IntByReference dwRead = new IntByReference();
            IntByReference dwWritten = new IntByReference();
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            Pointer data = Native.getDirectBufferPointer(buf);

            @Override
            public void write(int b) throws IOException {
                buf.put((byte)(b & 0xff));
                if (buf.remaining()<=0){
                    flush();
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                while (len>0){
                    int bytesToWrite = Math.min(len, buf.remaining());
                    buf.put(b, off, bytesToWrite);
                    off+=bytesToWrite;
                    len-=bytesToWrite;
                    if (buf.remaining()<=0){
                        flush();
                    }
                }
            }

            public void flush(){
                byte[] buffer = data.getByteArray(0, buf.remaining());
                bSuccess = Kernel32.INSTANCE.WriteFile(writeHandle.getValue(), buffer, dwRead.getValue(), dwWritten, null);
                if ( bSuccess ==false) {
                    return;
                }

            }

            @Override
            public void close() throws IOException {
                if ((writeHandle!=null)&&(Kernel32.INSTANCE.CloseHandle(writeHandle.getValue())==false)){
                    System.err.println(Kernel32.INSTANCE.GetLastError());
                } else writeHandle = null;
            }
        };
    }

    /*
    static void WriteToPipe(){

        boolean bSuccess = true;
        IntByReference dwRead = new IntByReference();
        IntByReference dwWritten = new IntByReference();
        ByteBuffer buf = ByteBuffer.allocateDirect(BUFSIZE);
        Pointer data = Native.getDirectBufferPointer(buf);

        for (;;){
            bSuccess = Kernel32.INSTANCE.ReadFile(inputFile, buf, BUFSIZE, dwRead, null);
            if ( ! bSuccess || dwRead.getValue() == 0 ) {
                break;
            }

            bSuccess = Kernel32.INSTANCE.WriteFile(childStdInWrite.getValue(), data.getByteArray(0, BUFSIZE), dwRead.getValue(), dwWritten, null);
            if ( ! bSuccess ) {
                break;
            }
        }

        // Close the pipe handle so the child process stops reading.

        if (!com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(childStdInWrite.getValue())){
            System.err.println(Kernel32.INSTANCE.GetLastError());
        }
    }

    // Read output from the child process's pipe for STDOUT
    // and write to the parent process's pipe for STDOUT.
    // Stop when there is no more data.
    static void ReadFromPipe(){

        IntByReference dwRead = new IntByReference();
        IntByReference dwWritten = new IntByReference();
        ByteBuffer buf = ByteBuffer.allocateDirect(BUFSIZE);
        Pointer data = Native.getDirectBufferPointer(buf);
        boolean bSuccess = true;
        WinNT.HANDLE hParentStdOut = Kernel32.INSTANCE.GetStdHandle(STD_OUTPUT_HANDLE);

        // Close the write end of the pipe before reading from the
        // read end of the pipe, to control child process execution.
        // The pipe is assumed to have enough buffer space to hold the
        // data the child process has already written to it.

        if (!Kernel32.INSTANCE.CloseHandle(childStdOutWrite.getValue())){
            System.err.println(Kernel32.INSTANCE.GetLastError());
        }

        for (;;){
            bSuccess = Kernel32.INSTANCE.ReadFile( childStdOutRead.getValue(), buf, BUFSIZE, dwRead, null);
            if( ! bSuccess || dwRead.getValue() == 0 ) {
                break;
            }

            bSuccess = Kernel32.INSTANCE.WriteFile(hParentStdOut, data.getByteArray(0, BUFSIZE), dwRead.getValue(), dwWritten, null);
            if (! bSuccess ) {
                break;
            }
        }
    }
    */
}
