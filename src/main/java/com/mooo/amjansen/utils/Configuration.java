package com.mooo.amjansen.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Configuration {

    private static Logger logger = LoggerFactory.getLogger(Configuration.class);

    private static Configuration singleton = null;

    private Map<String, String> parameters = null;

    private Configuration(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public static void initConfiguration(File file, String name) throws IOException {
        if (singleton != null)
            throw new IOException("config allready exists");

        synchronized (Configuration.class) {
            if (singleton != null)
                throw new IOException("config allready exists");

            if (file.exists()==false) {
                logger.info("config-file '" + file.getAbsolutePath() + "' doesn't exist");
                singleton = new Configuration(new HashMap<String, String>());
                return;
            } else if (file.isDirectory()==true){
                throw new IOException("config-file '" + file.getAbsolutePath() +"' is a directory");

            } else if (file.canRead() == false){
                throw new IOException("config-file '" + file.getAbsolutePath() +"' is unreadable");

            }

            FileInputStream inputStream = new FileInputStream(file);

            try {

                Properties properties = new Properties();

                properties.load(inputStream);

                Map<String, String> parameters = new HashMap<String, String>();

                Enumeration iter = properties.propertyNames();

                while (iter.hasMoreElements() == true) {
                    String valueName = (String) iter.nextElement();
                    
                    if ((name==null) || (name.length()==0)) {
                        parameters.put(valueName, properties.getProperty(valueName));

                    } else if (valueName.startsWith(name + ".") == true){
                        parameters.put(valueName.substring(name.length() + 1), properties.getProperty(valueName));
                    }
                }

                singleton = new Configuration(parameters);

            } finally {

                inputStream.close();

            }

        }
    }

    public static Configuration getInstance() throws IOException {
        if (singleton != null)
            return singleton;
        synchronized (Configuration.class) {
            if (singleton == null) {
                throw new IOException("config doesn't exist");
            }
        }
        return singleton;
    }

    public Collection<String> getValueNames(){
        return parameters.keySet();
    }

    public String getValue(String name) throws IOException {
        String value = parameters.get(name);
        if (value==null)
            throw new IOException("value '" + name + "' doesn't esist");
        return value;
    }

    public int getValue(String name, int defaultValue) throws IOException {
        String value = parameters.get(name);
        return ((value == null) || (value.length()==0)) ? defaultValue : Integer.parseInt(value);
    }

    public boolean getValue(String name, boolean defaultValue) throws IOException {
        String value = parameters.get(name);
        return ((value == null) || (value.length()==0)) ? defaultValue : Boolean.parseBoolean(value);
    }

    public String getValue(String name, String defaultValue) throws IOException {
        String value = parameters.get(name);
        return (value == null) ? defaultValue : value;
    }

    public Configuration getSubConfiguration(String prefix){

        Map<String, String> values = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : parameters.entrySet()){
            if (entry.getKey().startsWith(prefix)==true){
                values.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }

        return new Configuration(values);

    }
}
