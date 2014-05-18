package com.gnip.connection;

import com.gnip.Environment;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class GnipHttpClient {
    private final Logger logger = LogManager.getLogger(getClass());
    private Environment environment;
    private UriStrategy uriStrategy;

    @Inject
    public GnipHttpClient(Environment environment,
                          UriStrategy uriStrategy) {
        this.environment = environment;
        this.uriStrategy = uriStrategy;
    }

    public InputStream getStreaming() throws IOException {
        // Get HttpUrlConnection
        // Handle and log response
        // Return the InputStream for the connection
        URI streamUri = uriStrategy.createStreamUri(environment.accountName(), environment.streamLabel());
        HttpURLConnection connection = getConnection(new URL(streamUri.toString()), "GET", true);
        int responseCode = connection.getResponseCode();
        if (responseCode <= 200 && responseCode >= 299) {
            handleNonSuccessResponse(connection);
        }
        return connection.getInputStream();
    }

    private HttpURLConnection getConnection(URL url, String method, boolean output) throws IOException {
        // Open connection with URL
        // Set options:
        // - Read timeout from ENV
        // - Connection timeout e.g. 10000ms
        // - Request method
        // - DoOutput
        // Create basic auth header and setRequestProperty

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(environment.streamReadTimeout());
        connection.setConnectTimeout(1000 * 10);
        connection.setRequestMethod(method);
        connection.setDoOutput(output);

        connection.setRequestProperty("Authorization",
                createAuthHeader(environment.userName(),
                        environment.userPassword()));

        connection.setRequestProperty("Accept-Encoding", "gzip");

        return connection;
    }

    private String createAuthHeader(String username, String password) {
        // Create basic auth header Base64.encodeBase64 username:password
        String authToken = username + ":" + password;
        byte[] authTokenBytes = authToken.getBytes(Charsets.UTF_8);
        return "Basic " + new String(Base64.encodeBase64(authTokenBytes), Charsets.UTF_8);
    }

    private void handleNonSuccessResponse(HttpURLConnection connection) throws IOException {
        // Log the bad response, make sure to leave enough information here to debug any issues
        logger.error(String.format("Error making %s request to %s Response code: %d, Reason: %s",
                connection.getRequestMethod(),
                connection.getURL().toString(),
                connection.getResponseCode(),
                connection.getResponseMessage()));
    }

}
