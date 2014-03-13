package com.gnip.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.gnip.ClientConfig;
import com.gnip.parsing.JSONUtils;
import com.gnip.rules.Rule;
import com.gnip.rules.Rules;
import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

public class GnipStream {
    private static final Logger logger = Logger.getLogger(GnipStream.class);
    private final ClientConfig clientConfig;
    private final String accountName;
    private final String streamLabel;
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
        accountName = clientConfig.accountName();
        streamLabel = clientConfig.streamLabel();
        executorService = Executors.newFixedThreadPool(4);
    }

    private String getStreamingUrl() {
        return String.format(
                "https://stream.gnip.com:443/accounts/%s/publishers/twitter/streams/track/%s.json",
                accountName,
                streamLabel);
    }

    private String getRulesUrl() {
        return String.format(
                "https://api.gnip.com:443/accounts/%s/publishers/twitter/streams/track/%s/rules.json",
                accountName,
                streamLabel);
    }

    private HttpURLConnection getConnection(String urlStr, String method, boolean output) throws IOException {
        URL url = new URL(urlStr);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(clientConfig.streamReadTimeout());
        connection.setConnectTimeout(1000 * 10);
        connection.setRequestMethod(method);
        connection.setDoOutput(output);

        connection.setRequestProperty("Authorization",
                createAuthHeader(clientConfig.userName(),
                        clientConfig.userPassword()));

        connection.setRequestProperty("Accept-Encoding", "gzip");

        return connection;
    }

    private String createAuthHeader(String username, String password) {
        String authToken = username + ":" + password;
        byte[] authTokenBytes = authToken.getBytes(Charsets.UTF_8);
        return "Basic " + new String(Base64.encodeBase64(authTokenBytes), Charsets.UTF_8);
    }

    public boolean establishConnection() {
        try {
            streamingConnection = getConnection(getStreamingUrl(), "GET", false);
            inputStream = streamingConnection.getInputStream();
            reader = new BufferedReader(
                    new InputStreamReader(
                            new StreamingGZIPInputStream(inputStream), StandardCharsets.UTF_8));

        } catch (IOException e) {
            streamHandler.notifyConnectionError(this, e);
            return false;
        }
        connected.set(true);
        streamHandler.notifyConnected(this);
        return connected.get();
    }

    public boolean reconnect() {
        boolean success;
        try {
            close();
        } catch (IOException e) {
            logger.error("Could not close stream to attempt re-connect, could be leaking resources", e);
        }
        success = establishConnection();
        return success;
    }

    public String getName() {
        return String.format("Account: %s Stream: %s", accountName, streamLabel);
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

    public void addRules(Rules rules) {
        HttpURLConnection uc = null;
        try {
            uc = getConnection(getRulesUrl(), "POST", true);
            doWithRules(rules, uc);
        } catch (IOException e) {
            logger.error("Unable to add rule: " + rules, e);
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
        HttpURLConnection connection;
        try {
            connection = getConnection(getRulesUrl(), "DELETE", true);
            doWithRules(rules, connection);
        } catch (IOException e) {
            logger.error("Unable to delete rule: " + rules, e);
        }
    }

    public Rules listRules() {
        Rules rules = new Rules();
        try {
            HttpURLConnection connection = getConnection(getRulesUrl(), "GET", false);
            InputStream is = connection.getInputStream();
            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode <= 299) {
                JsonNode ruleList = JSONUtils.getObjectMapper().readTree(is);

                for (JsonNode rule : ruleList.get("rules")) {
                    rules.getRules().add(
                            new Rule(rule.get("value").textValue(),
                                    rule.get("tag").textValue()));
                }
            } else {
                logger.error("Bad response" + responseCode + connection.getResponseMessage());
            }
        } catch (IOException e) {
            logger.error("Error listing rules", e);
        }

        return rules;
    }

    private void doWithRules(Rules rules, HttpURLConnection uc) throws IOException {
        OutputStream output = null;
        try {
            output = uc.getOutputStream();
            byte[] bytes = rules.build().getBytes(Charsets.UTF_8);
            output.write(bytes);
        } finally {
            if (output != null) try {
                output.close();
            } catch (IOException logOrIgnore) {
            }
        }
        logRulesResponse(uc, rules);
    }

    private void logRulesResponse(HttpURLConnection uc, Rules rules) {
        InputStream is = null;
        BufferedReader br;
        String message;
        try {
            int responseCode = uc.getResponseCode();
            String responseMessage = uc.getResponseMessage();

            if (responseCode >= 200 && responseCode <= 299) {
                is = uc.getInputStream();
            } else {
                is = uc.getErrorStream();
            }

            logger.info(MessageFormat.format("For method {0} Response Code: {1} -- {2} -- {3}",
                    uc.getRequestMethod(),
                    responseCode,
                    responseMessage,
                    rules.toString()));

            br = new BufferedReader(new InputStreamReader(is));
            message = br.readLine();
            while (message != null) {
                logger.info(message);
                message = br.readLine();
            }
        } catch (IOException e) {
            logger.warn("Error handling response", e);
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
