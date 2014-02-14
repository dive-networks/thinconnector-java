package com.gnip.stream;

import com.gnip.ClientConfig;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class GnipStream {
    private static final Logger logger = Logger.getLogger(GnipStream.class.getName());
    private final ClientConfig clientConfig;
    private final String streamUrl;
    private ExecutorService executorService;
    private InputStream inputStream = null;
    private StreamHandler streamHandler;
    private HttpURLConnection connection;
    private BufferedReader reader;
    private AtomicBoolean connected = new AtomicBoolean(false);

    public GnipStream(StreamHandler streamHandler,
                      ClientConfig clientConfig) {
        this.streamHandler = streamHandler;
        this.clientConfig = clientConfig;
        clientConfig.streamLabel();
        this.streamUrl = clientConfig.streamUrl();
        executorService = Executors.newFixedThreadPool(4);
    }

    public boolean establishConnection() {
        try {
            URL url = new URL(streamUrl);

            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(clientConfig.streamReadTimeout());
            connection.setConnectTimeout(1000 * 10);
            connection.setRequestMethod("GET");

            connection.setRequestProperty("Authorization", createAuthHeader());
            connection.setRequestProperty("Accept-Encoding", "gzip");
            inputStream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(new StreamingGZIPInputStream(inputStream), StandardCharsets.UTF_8));
            connected.set(true);
        } catch (IOException e) {
            streamHandler.notifyConnectionError(this, e);
            return false;
        }
        streamHandler.notifyConnected(this);
        return true;
    }

    private String createAuthHeader() throws UnsupportedEncodingException {
        BASE64Encoder encoder = new BASE64Encoder();
        String authToken = clientConfig.userName() + ":" + clientConfig.userPassword();
        return "Basic " + encoder.encode(authToken.getBytes());
    }

    public boolean reconnect() {
        boolean success;
        try {
            close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not close stream to attempt re-connect, could be leaking resources", e);
        }
        success = establishConnection();
        return success;
    }

    public String getName() {
        return streamUrl;
    }

    public void stream() throws IOException {
        while (connected.get()) {
            try {
                final String line = reader.readLine();
                if (line == null) {
                    throw new TransportException("Read null line from resource");
                }
                // Make sure implementation of handleMessage is thread-safe
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        streamHandler.handleMessage(line);
                    }
                });
            } catch (SocketTimeoutException | TransportException e) {
                streamHandler.notifyDisconnect(this, e);
            }
        }
    }

    public void close() throws IOException {
        connected.set(false);
        if (connection != null) {
            connection.disconnect();
        }
        if (inputStream != null) {
            inputStream.close();
        }
        connection = null;
        inputStream = null;
    }

    private class StreamingGZIPInputStream extends GZIPInputStream {
        private final InputStream wrapped;

        public StreamingGZIPInputStream(InputStream is) throws IOException {
            super(is);
            wrapped = is;
        }

        /**
         * Overrides behavior of GZIPInputStream which assumes we have all the data available
         * which is not true for streaming. We instead rely on the underlying stream to tell us
         * how much data is available.
         * <p/>
         * Programs should not count on this method to return the actual number
         * of bytes that could be read without blocking.
         *
         * @return - whatever the wrapped InputStream returns
         * @throws IOException if an I/O error occurs.
         */

        public int available() throws IOException {
            return wrapped.available();
        }
    }

    public class TransportException extends IOException {
        public TransportException(String message) {
            super(message);
        }
    }
}
