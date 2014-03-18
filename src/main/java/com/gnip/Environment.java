package com.gnip;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Singleton
public class Environment {
    private static final Logger logger = Logger.getLogger(Environment.class);
    private static Environment instance = null;
    private final Properties props;

    public Environment() {
        props = new Properties();
        InputStream properties = Environment.class.getClassLoader().getResourceAsStream("config.properties");
        try {
            props.load(properties);
        } catch (IOException e) {
            logger.error("Could not load properties, streams cannot be configured");
            throw new RuntimeException("Could not load properties");
        }
    }

    public String userName() {
        return String.valueOf(props.get("user.name"));
    }

    public String userPassword() {
        return String.valueOf(props.get("user.password"));
    }

    public int streamReadTimeout() {
        return Integer.parseInt(props.getProperty("stream.read.timeout", "600000"));
    }

    public String streamLabel() {
        return String.valueOf(props.get("stream.label"));
    }

    public String accountName() {
        return String.valueOf(props.get("account.name"));
    }
}
