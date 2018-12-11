package com.mooo.amjansen.process;

import com.sun.jna.Native;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.17
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public class UnixProcessFactory implements ProcessFactoryIfc {

    public void chmod(File file, int mode) throws IOException {
        if (UnixProcess.libc.chmod(file.getAbsolutePath(), mode)==-1){
            throw new IOException("can't chmod file '"+file.getAbsolutePath()+"', errno: "+ Native.getLastError());
        }
    }

    @Override
    public ProcessIfc openProcess(int pid) throws IOException {
        return new UnixProcess(pid);
    }

    @Override
    public ProcessIfc openProcess(File workingDirectory, String[] args, Map<String, String> env) throws IOException {
        return new UnixProcess(workingDirectory, args, env);
    }

}
