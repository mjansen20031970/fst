package com.mooo.amjansen.process;

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
public interface ProcessFactoryIfc {

    public void chmod(File file, int flags) throws IOException;

    public ProcessIfc openProcess(int pid) throws IOException;

    public ProcessIfc openProcess(File workingDirectory, String[] cmdLine, Map<String, String> env) throws IOException;
}
