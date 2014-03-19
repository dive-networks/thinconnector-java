package com.gnip.stream;

import java.io.IOException;

public interface StreamHandler {
    public void handleMessage(String message);

    public void notifyDisconnect(IOException e);

    public void notifyConnected(String streamName);

    public void notifyConnectionError(IOException e);
}
