package com.gnip.stream;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultStreamHandler implements StreamHandler {
    Logger logger = Logger.getLogger(StreamHandler.class.getName());

    @Override
    public void handleMessage(String message) {
        // Write to DB, send to front-end, etc. Ensure thread safety.
        System.out.println(message);
    }

    @Override
    public void notifyDisconnect(GnipStream gnipStream, IOException e) {
        logger.log(Level.WARNING, "Disconnected from " + gnipStream.getName(), e);
        gnipStream.reconnect();
    }

    @Override
    public void notifyConnected(GnipStream gnipStream) {
        logger.info("Streaming connection made to " + gnipStream.getName());
    }

    @Override
    public void notifyConnectionError(GnipStream gnipStream, IOException e) {
        logger.log(Level.WARNING, "Error connecting to " + gnipStream.getName(), e);
    }

}
