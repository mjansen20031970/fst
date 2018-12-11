package com.mooo.amjansen.process;

import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: matthias
 * Date: 29.03.17
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
public interface ProcessIfc {

    public int getPid();

    public int exitValue();

    public void destroy();

    public InputStream getInputStream();
}
