package com.gnip.connection;

import java.net.URI;

public class UriStrategy {
    public static final String BASE_GNIP_STREAM_URI = "https://stream.gnip.com:443/accounts/%s/publishers/twitter/streams/track/%s.json";
    public static final String BASE_GNIP_RULES_URI = "https://api.gnip.com:443/accounts/%s/publishers/twitter/streams/track/%s/rules.json";

    public URI createStreamUri(final String account, String streamName) {
        if (account == null || account.trim().isEmpty()) {
            throw new IllegalArgumentException("The account cannot be null or empty");
        }
        if (streamName == null || streamName.trim().isEmpty()) {
            throw new IllegalArgumentException("The streamName cannot be null or empty");
        }

        return URI.create(String.format(BASE_GNIP_STREAM_URI, account.trim(), streamName.trim()));
    }

    public URI createRulesUri(final String account, String streamName) {
        if (account == null || account.trim().isEmpty()) {
            throw new IllegalArgumentException("The account cannot be null or empty");
        }
        if (streamName == null || streamName.trim().isEmpty()) {
            throw new IllegalArgumentException("The streamName cannot be null or empty");
        }

        return URI.create(String.format(BASE_GNIP_RULES_URI, account.trim(), streamName.trim()));
    }
}
