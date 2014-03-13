package com.gnip.stream;

import org.apache.log4j.Logger;

import java.io.IOException;

public class DefaultStreamHandler implements StreamHandler {
    Logger logger = Logger.getLogger(StreamHandler.class);

    @Override
    public void handleMessage(String message) {
        // Write to DB, send to front-end, etc. Ensure thread safety.
        System.out.println(message);
    }

    @Override
    public void notifyDisconnect(GnipStream gnipStream, IOException e) {
        logger.warn("Disconnected from " + gnipStream.getName(), e);
        gnipStream.reconnect();
    }

    @Override
    public void notifyConnected(GnipStream gnipStream) {
        logger.info("Streaming connection made to " + gnipStream.getName());
    }

    @Override
    public void notifyConnectionError(GnipStream gnipStream, IOException e) {
        logger.warn("Error connecting to " + gnipStream.getName(), e);
    }

}
