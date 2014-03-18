package com.gnip.connection;

import com.gnip.Environment;
import org.apache.log4j.Logger;

public class StreamConnectionStrategy implements ConnectionStrategy {
    private final Logger logger = Logger.getLogger(getClass());
    private Environment environment;

    public StreamConnectionStrategy(Environment environment) {
        this.environment = environment;
    }

}
