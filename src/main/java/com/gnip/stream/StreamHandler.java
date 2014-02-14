package com.gnip.stream;

import java.io.IOException;

public interface StreamHandler {
    public void handleMessage(String message);

    public void notifyDisconnect(GnipStream gnipStream, IOException e);

    public void notifyConnected(GnipStream gnipStream);

    public void notifyConnectionError(GnipStream gnipStream, IOException e);
}
