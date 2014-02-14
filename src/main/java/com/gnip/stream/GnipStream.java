package com.gnip.stream;

import com.gnip.ClientConfig;
import com.gnip.rules.Rule;
import com.gnip.rules.Rules;
import com.oracle.javafx.jmx.json.JSONDocument;
import com.oracle.javafx.jmx.json.JSONReader;
import com.oracle.javafx.jmx.json.impl.JSONStreamReaderImpl;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
    private String ruleApi;
    private ExecutorService executorService;
    private InputStream inputStream = null;
    private StreamHandler streamHandler;
    private HttpURLConnection streamingConnection;
    private BufferedReader reader;
    private AtomicBoolean connected = new AtomicBoolean(false);

    public GnipStream(StreamHandler streamHandler,
                      ClientConfig clientConfig) {
        this.streamHandler = streamHandler;
        this.clientConfig = clientConfig;
        clientConfig.streamLabel();
        this.streamUrl = clientConfig.streamUrl();
        this.ruleApi = clientConfig.ruleUrl();
        executorService = Executors.newFixedThreadPool(4);
    }

    public boolean establishConnection() {
        try {
            streamingConnection = getConnection(streamUrl, "GET", false);
            inputStream = streamingConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(new StreamingGZIPInputStream(inputStream), StandardCharsets.UTF_8));
            connected.set(true);
        } catch (IOException e) {
            streamHandler.notifyConnectionError(this, e);
            return false;
        }
        streamHandler.notifyConnected(this);
        return true;
    }

    private HttpURLConnection getConnection(String urlStr, String method, boolean output) throws IOException {
        URL url = new URL(urlStr);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(clientConfig.streamReadTimeout());
        connection.setConnectTimeout(1000 * 10);
        connection.setRequestMethod(method);
        connection.setDoOutput(output);

        connection.setRequestProperty("Authorization", createAuthHeader());
        connection.setRequestProperty("Accept-Encoding", "gzip");

        return connection;
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
        if (inputStream != null) {
            inputStream.close();
        }
        if (streamingConnection != null) {
            streamingConnection.disconnect();
        }
        streamingConnection = null;
        inputStream = null;
    }

    public void addRule(Rule rule) {
        addRules(new Rules(rule));
    }

    public void addRules(Rules rules){
        HttpURLConnection uc = null;
        try {
            uc = getConnection(ruleApi, "POST", true);
            doWithRules(rules, uc);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to add rule: " + rules, e);
        } finally {
            if (uc != null) {
                uc.disconnect();
            }
        }
    }

    public void deleteRule(Rule rule) {
        deleteRules(new Rules(rule));
    }

    public void deleteRules(Rules rules) {
        HttpURLConnection connection = null;
        try {
            connection = getConnection(ruleApi, "DELETE", true);
            doWithRules(rules, connection);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to delete rule: " + rules, e);
        }
    }

    public Rules listRules() {
        Rules rules = new Rules();
        try {
            HttpURLConnection connection = getConnection(ruleApi, "GET", false);

            InputStream is = connection.getInputStream();
            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode <= 299) {

                JSONReader jsonReader = new JSONStreamReaderImpl(new InputStreamReader((is), StandardCharsets.UTF_8));
                List<Object> ruleList = jsonReader.build().getList("rules");

                for (Object rule : ruleList) {
                    StringReader sr = new StringReader(rule.toString());
                    JSONDocument aRule = new JSONStreamReaderImpl(sr).build();
                    rules.getRules().add(new Rule(aRule.getString("value"), aRule.getString("tag")));
                }

            } else {
                logger.severe("Bad response" + responseCode + connection.getResponseMessage());
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error listing rules", e);
        }

        return rules;
    }

    private void doWithRules(Rules rules, HttpURLConnection uc) throws IOException {
        OutputStream output = null;
        try {
            output = uc.getOutputStream();
            byte[] bytes = rules.build().getBytes(StandardCharsets.UTF_8.name());
            output.write(bytes);
        } finally {
            if (output != null) try {
                output.close();
            } catch (IOException logOrIgnore) {
            }
        }
        logResponse(uc);
    }

    private void logResponse(HttpURLConnection uc) {
        InputStream is = null;
        BufferedReader br;
        String message;
        try {
            int responseCode = uc.getResponseCode();
            String responseMessage = uc.getResponseMessage();

            if (responseCode >= 200 && responseCode <= 299) {
                is = uc.getInputStream();

                // Just print the first line of the response.
                br = new BufferedReader(new InputStreamReader(is));
                message = br.readLine();
                logger.info("Response Code: " + responseCode + " -- " + responseMessage);
                while (message != null) {
                    logger.info(message);
                    message = br.readLine();
                }
            } else {
                is = uc.getErrorStream();
                br = new BufferedReader(new InputStreamReader(is));
                message = br.readLine();
                while (message != null) {
                    logger.warning(message);
                    message = br.readLine();
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error handling response", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public boolean connected() {
        return connected.get();
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
