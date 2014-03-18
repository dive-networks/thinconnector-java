package com.gnip;

import org.apache.log4j.Logger;

public class ThinConnector {
    private final static Logger logger = Logger.getLogger(ThinConnector.class);

    public static void main(String[] args) {
        ThinConnector thinConnector = new ThinConnector();
        try {
            thinConnector.start();
        } catch (Exception e) {
            logger.error("Unexpected error occured.", e);
        }
    }

    public void start() throws Exception {

    }
}
