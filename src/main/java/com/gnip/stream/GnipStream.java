package com.gnip.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.gnip.Environment;
import com.gnip.Environment;
import com.gnip.connection.ConnectionStrategy;
import com.gnip.parsing.JSONUtils;
import com.gnip.rules.Rule;
import com.gnip.rules.Rules;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
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
    private final Logger logger = Logger.getLogger(getClass());

    @Inject
    public GnipStream(Environment environment,
                      StreamHandler streamHandler,
                      StreamProcessor processor) {

    }


}
