package com.mooo.amjansen.process;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.17
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public class UnixProcess implements ProcessIfc {

    private static Logger logger = LoggerFactory.getLogger(UnixProcess.class);

    protected static LIBC libc = (LIBC) Native.loadLibrary("libc", LIBC.class);

    public static interface LIBC extends Library {


        public static final int EINVAL      = 22;
        public static final int EPERM       = 1;
        public static final int ESRCH       = 3;




        public static final int SIGKILL     = 9;

        public int kill(int pid, int signum);

        public int chmod(String path, int mode);

    }

    private int pid = -1;
    private Process process = null;

    public UnixProcess(int pid) throws IOException{
        if (libc.kill(pid, 0)!=0){

            switch (Native.getLastError()){
                case LIBC.EINVAL:
                    break;
                case LIBC.EPERM:
                    break;
                case LIBC.ESRCH:
                    throw new FileNotFoundException("process "+pid+" doesn't exist");

            }

        } else this.pid = pid;
    }

    public UnixProcess(File workingDirectory, String[] args, Map<String, String> env) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(args)
            .directory(workingDirectory)
            .redirectErrorStream(true);
        Map<String, String> parentEnvironment = builder.environment();

        if ((env!=null)&&(env.size()>0)){
            for (Map.Entry<String, String> entry : env.entrySet()){
                parentEnvironment.put(entry.getKey(), entry.getValue());
            }
        }
        process = builder.start();
    }

    public int getPid(){
        return pid;
    }

    @Override
    public int exitValue() {
        if (process!=null){
            return process.exitValue();

        } else if (pid<0){
            return 0;

        } else if (libc.kill(pid, 0)==0){
            return 0;

        } else throw new IllegalThreadStateException("process "+pid+" is still running");
    }

    @Override
    public void destroy() {
        if (process!=null){
            process.destroy();
            process = null;

        } else if (pid>=0){

            if (libc.kill(pid, LIBC.SIGKILL)==-1){

                logger.info("signal SIGKILL is send to pid: "+pid);

                switch (Native.getLastError()){
                    case LIBC.EINVAL:
                        throw new RuntimeException("illegal signal to kill "+pid);
                    case LIBC.EPERM:
                        throw new RuntimeException("permission denied to kill "+pid);
                    case LIBC.ESRCH:
                        logger.warn("process "+pid+" doesn't exist");
                        break;

                }

            } else pid = -1;
        }
    }

    @Override
    public InputStream getInputStream() {
        return (process==null) ? null : process.getInputStream();
    }
}
