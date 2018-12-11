package com.mooo.amjansen.process;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.17
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public class Win32Process implements ProcessIfc {

    private static Logger logger = LoggerFactory.getLogger(Win32Process.class);

    private static final int STILL_ACTIVE               = 259;

    private static final int ERROR_FILE_NOT_FOUND       = 0x2;
    private static final int ERROR_INVALID_PARAMETER    = 0x57;

    private static final int WAIT_ABANDONED             = 0x00000080;
    private static final int WAIT_OBJECT_0              = 0x00000000;
    private static final int WAIT_TIMEOUT               = 0x00000102;
    private static final int WAIT_FAILED                = 0xFFFFFFFF;

    private static final int DELETE                     = 0x00010000;
    private static final int SYNCHRONIZE                = 0x00100000;
    private static final int PROCESS_TERMINATE          = 0x0001;
    private static final int PROCESS_QUERY_INFORMATION  = 0x0400;

    private WinNT.HANDLE handle = null;
    private int pid = 0;

    public interface ProcessKernel32 extends Kernel32 {

        final static Map<String, Object> WIN32API_OPTIONS = new HashMap<String, Object>() {

            private static final long serialVersionUID = 1L;

            {
                put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
                put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
            }
        };

        public ProcessKernel32 INSTANCE = (ProcessKernel32) Native.loadLibrary("Kernel32", ProcessKernel32.class, WIN32API_OPTIONS);

/*
    BOOL WINAPI CreateProcess(
            __in_opt     LPCTSTR lpApplicationName,
            __inout_opt  LPTSTR lpCommandLine,
            __in_opt     LPSECURITY_ATTRIBUTES lpProcessAttributes,
            __in_opt     LPSECURITY_ATTRIBUTES lpThreadAttributes,
            __in         BOOL bInheritHandles,
            __in         DWORD dwCreationFlags,
            __in_opt     LPVOID lpEnvironment,
            __in_opt     LPCTSTR lpCurrentDirectory,
            __in         LPSTARTUPINFO lpStartupInfo,
            __out        LPPROCESS_INFORMATION lpProcessInformation
            );
*/
        public boolean CreateProcess(
                String lpApplicationName,
                String lpCommandLine,
                WinBase.SECURITY_ATTRIBUTES lpProcessAttributes,
                WinBase.SECURITY_ATTRIBUTES lpThreadAttributes,
                boolean bInheritHandles,
                WinDef.DWORD dwCreationFlags,
                ByteBuffer lpEnvironment,
                String lpCurrentDirectory,
                WinBase.STARTUPINFO lpStartupInfo,
                WinBase.PROCESS_INFORMATION lpProcessInformation
        );

        public Pointer GetEnvironmentStrings(
        );

        public boolean FreeEnvironmentStrings(
                Pointer lpszEnvironmentBlock
        );

    }

    public Win32Process(int pid) throws FileNotFoundException {
        IntByReference result = new IntByReference();
        if ((handle = Kernel32.INSTANCE.OpenProcess(
            DELETE | SYNCHRONIZE | PROCESS_QUERY_INFORMATION | PROCESS_TERMINATE, false, pid))==null){
            throw new FileNotFoundException("process "+pid+" doesn't exist");
        } else if (Kernel32.INSTANCE.GetExitCodeProcess(handle, result)==false){
            Kernel32.INSTANCE.CloseHandle(handle);
            throw new RuntimeException("GetExitCodeProcess failed, retcode: "+Kernel32.INSTANCE.GetLastError());

        } else if (result.getValue()!=STILL_ACTIVE){
            Kernel32.INSTANCE.CloseHandle(handle);
            throw new FileNotFoundException("process "+pid+" exist, but is terminated");

        } else this.pid = pid;
    }

    public Win32Process(File workingDirectory, String[] args, Map<String, String> env) throws FileNotFoundException {

        args[0] = new File(args[0]).getPath();

        StringBuilder cmdbuf = new StringBuilder(80);
        for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    cmdbuf.append(' ');
                }
            String s = args[i];
            if (s.indexOf(' ') >= 0 || s.indexOf('\t') >= 0) {
                if (s.charAt(0) != '"') {
                cmdbuf.append('"');
                cmdbuf.append(s);
                if (s.endsWith("\\")) {
                cmdbuf.append("\\");
                }
                cmdbuf.append('"');
                    } else if (s.endsWith("\"")) {
                /* The argument has already been quoted. */
                cmdbuf.append(s);
            } else {
                /* Unmatched quote for the argument. */
                throw new IllegalArgumentException();
            }
            } else {
                cmdbuf.append(s);
            }
        }

        WinBase.PROCESS_INFORMATION processInformation = new WinBase.PROCESS_INFORMATION();
        WinBase.STARTUPINFO startupInfo = new WinBase.STARTUPINFO();
        startupInfo.cb = new WinDef.DWORD(processInformation.size());

        Map<String, String> parentEnvironment = getParentEnvironment();
        if ((env!=null)&&(env.size()>0)){
            for (Map.Entry<String, String> entry : env.entrySet()){
                parentEnvironment.put(entry.getKey(), entry.getValue());
            }
        }

        ByteBuffer buf = formulateEnvironmentVariableBlock(parentEnvironment);

        if (ProcessKernel32.INSTANCE.CreateProcess(
                null,
                cmdbuf.toString(),
                null,
                null,
                false,
                new WinDef.DWORD(0x00000020),
                buf,
                workingDirectory.getAbsolutePath(),
                startupInfo,
                processInformation) ==false){
            int errno = Kernel32.INSTANCE.GetLastError();
            switch (errno){
                case ERROR_FILE_NOT_FOUND:
                    throw new FileNotFoundException("can't find file '"+args[0]+"'");
                case ERROR_INVALID_PARAMETER:
                    throw new FileNotFoundException("The parameter is incorrect");
                default:
                    throw new RuntimeException("can't start process '"+args[0]+"', errno: "+errno);
            }

        } else {
            Kernel32.INSTANCE.CloseHandle(processInformation.hThread);
            handle = processInformation.hProcess;
        }
    }

    public int getPid(){
        return pid;
    }

    @Override
    public void destroy() {
        if ((handle!=null)&&(Kernel32.INSTANCE.TerminateProcess(handle, -1)==true)){
            for (int i=0; i<10; i++){
                if (waitFor(1000)==true){
                    break;
                } else logger.warn("wait for process termination takes longer than "+(i+1)+" secs");
            }
            Kernel32.INSTANCE.CloseHandle(handle);
            handle = null;
            pid = 0;
        }
    }

    public boolean waitFor(long msec){
        switch(Kernel32.INSTANCE.WaitForSingleObject(handle, (int) msec)){
            case WAIT_OBJECT_0 :
                return true;
            case WAIT_TIMEOUT :
                return false;
            case WAIT_ABANDONED:
                throw new RuntimeException("object is abandoned");
            default:
            case WAIT_FAILED:
                throw new RuntimeException("WaitForSingleObject failed, retcode: "+Kernel32.INSTANCE.GetLastError());
        }
    }

    @Override
    public int exitValue(){
        IntByReference result = new IntByReference();
        if (Kernel32.INSTANCE.GetExitCodeProcess(handle, result)==true){

            if (result.getValue()==STILL_ACTIVE){
                throw new IllegalThreadStateException("process is still active");

            } else return result.getValue();

        } else throw new RuntimeException("GetExitCodeProcess failed, retcode: "+Kernel32.INSTANCE.GetLastError());
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    private static ByteBuffer formulateEnvironmentVariableBlock(Map<String, String> env) {
        //final byte[] terminator = new byte[] { 0 }; // Win32Library.USE_UNICODE ? new byte[] { 0, 0 } : new byte[] { 0 };
        final byte[] terminator = new byte[]{0};
        final String charset_name = "ASCII"; // Win32Library.USE_UNICODE ? "UTF-8" : "ASCII";
//    final String charset_name = "UTF-8"; // Win32Library.USE_UNICODE ? "UTF-8" : "ASCII";
//    final String charset_name = "UTF-16LE"; // Win32Library.USE_UNICODE ? "UTF-8" : "ASCII";
        final Charset charset = Charset.forName(charset_name);
        final int size_per_char = (int) charset.newEncoder().maxBytesPerChar();

        int total_size = 0;

        for (Map.Entry<String, String> entry : env.entrySet()) {
            total_size += entry.getKey().length() * size_per_char;
            total_size += size_per_char; //=
            total_size += entry.getValue().length() * size_per_char;
            total_size += terminator.length * 2; //terminating null
        }

        total_size += terminator.length * 2; //ending terminating null
        //if ("ASCII".equals(charset_name) && total_size > MAX_ANSI_ENVIRONMENT_BLOCK_SIZE) {
        // throw new IllegalStateException("The total size of the entire environment variable block cannot exceed " + MAX_ANSI_ENVIRONMENT_BLOCK_SIZE + " bytes");
        //}

        ByteBuffer bb = ByteBuffer.allocate(total_size);// + 100);
        ByteBuffer var;
        for (Map.Entry<String, String> entry : env.entrySet()) {
            var = charset.encode(entry.getKey() + "=" + entry.getValue());
            bb.put(var);
            bb.put(terminator);
        }

        //bb.put(charset.encode("" + '\0'));
        bb.put(terminator);
        bb.put(terminator);
        //bb.putInt(-1);bb.putInt(-1);bb.putInt(-1);bb.putInt(-1);
        //bb.compact();
        bb.flip();


        return bb;
    }

    private Map<String, String> getParentEnvironment() {

        Map<String, String> env = new HashMap();
        Charset charset_utf16_le = Charset.forName("UTF-16LE");
        Pointer ptr = ProcessKernel32.INSTANCE.GetEnvironmentStrings();
        byte b1, b2;
        int offset = 0;
        int i = 0;

        while (true) {
            b1 = ptr.getByte(i++);
            b2 = ptr.getByte(i++);

            if (b1 == 0 && b2 == 0) {
                final ByteBuffer bb = ptr.getByteBuffer(offset, i - offset - 2);
                final String s = charset_utf16_le.decode(bb).toString();
                final int index_of_equals = s.indexOf('=');
                if (index_of_equals >= 0) {
                    final String name = s.substring(0, index_of_equals);
                    final String value = s.substring(index_of_equals + 1);
                    env.put(name, value);
                }
                offset = i;

                if (ptr.getByte(i + 1) == 0 && ptr.getByte(i + 2) == 0) {
                    break;
                }
            }
        }
        ProcessKernel32.INSTANCE.FreeEnvironmentStrings(ptr);
        return env;
    }
}
