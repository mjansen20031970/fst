package com.mooo.amjansen.process;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.17
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public class ProcessFactorySPI {

    private static ProcessFactoryIfc processFactory = null;

    public static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		// windows
		return (os.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		String os = System.getProperty("os.name").toLowerCase();
		// Mac
		return (os.indexOf("mac") >= 0);
	}

	public static boolean isUnix() {
		String os = System.getProperty("os.name").toLowerCase();
		// linux or unix
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
	}

	public static boolean isSolaris() {
		String os = System.getProperty("os.name").toLowerCase();
		// Solaris
		return (os.indexOf("sunos") >= 0);
	}

    public static ProcessFactoryIfc getProcessFactory(){
        if (processFactory!=null){
            return processFactory;
        }
        synchronized (ProcessFactorySPI.class){
            if (processFactory!=null){
                return processFactory;
            }

            if (isWindows()==true){
                try {
                    Class cls = Class.forName("de.dbh.controlcenter.Win32ProcessFactory");
                    return processFactory = (ProcessFactoryIfc)cls.newInstance();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (InstantiationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalAccessException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                return null;

            } else if (isUnix()==true){
                try {
                    Class cls = Class.forName("de.dbh.controlcenter.UnixProcessFactory");
                    return processFactory = (ProcessFactoryIfc)cls.newInstance();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (InstantiationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalAccessException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                return null;

            } else return null;

        }
    }

}
