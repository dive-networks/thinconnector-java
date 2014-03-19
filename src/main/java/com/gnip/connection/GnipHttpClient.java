package com.gnip.connection;

import com.gnip.Environment;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class GnipHttpClient {
    private final Logger logger = Logger.getLogger(getClass());
    private Environment environment;
    private UriStrategy uriStrategy;

    @Inject
    public GnipHttpClient(Environment environment,
                          UriStrategy uriStrategy) {
        this.environment = environment;
        this.uriStrategy = uriStrategy;
    }

    private HttpURLConnection getConnection(String urlStr, String method, boolean output) throws IOException {
        URL url = new URL(urlStr);

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
        String authToken = username + ":" + password;
        byte[] authTokenBytes = authToken.getBytes(Charsets.UTF_8);
        return "Basic " + new String(Base64.encodeBase64(authTokenBytes), Charsets.UTF_8);
    }

    public InputStream getStreaming() throws IOException {
        URI streamUri = uriStrategy.createStreamUri(environment.accountName(), environment.streamLabel());
        HttpURLConnection connection = getConnection(streamUri.toString(), "GET", true);
        int responseCode = connection.getResponseCode();
        if (responseCode <= 200 && responseCode >= 299) {
            handleNonSuccessResponse(connection);
        }
        return connection.getInputStream();
    }

    private void handleNonSuccessResponse(HttpURLConnection connection) throws IOException {
        logger.error(String.format("Error making %s request to %s Response code: %d, Reason: %s",
                connection.getRequestMethod(),
                connection.getURL().toString(),
                connection.getResponseCode(),
                connection.getResponseMessage()));
    }

}
