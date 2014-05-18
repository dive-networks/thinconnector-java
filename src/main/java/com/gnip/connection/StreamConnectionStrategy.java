package com.gnip.connection;

import com.gnip.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StreamConnectionStrategy implements ConnectionStrategy {
    private final Logger logger = LogManager.getLogger(getClass());
    private Environment environment;

    public StreamConnectionStrategy(Environment environment) {
        this.environment = environment;
    }

}
