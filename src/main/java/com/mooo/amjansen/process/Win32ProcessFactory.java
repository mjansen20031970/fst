package com.mooo.amjansen.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.17
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public class Win32ProcessFactory implements ProcessFactoryIfc {

    public void chmod(File file, int flags) throws IOException {
    }

    @Override
    public ProcessIfc openProcess(int pid) throws IOException {
        return new Win32Process(pid);
    }

    @Override
    public ProcessIfc openProcess(File workingDirectory, String[] args, Map<String, String> env) throws FileNotFoundException {
        return new Win32Process(workingDirectory, args, env);
    }
}
