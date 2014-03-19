package com.gnip.stream;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.gnip.connection.GnipHttpClient;
import com.gnip.utilities.TaskManager;
import com.google.inject.Inject;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

public class StreamProcessor implements Runnable {
    private static final long MAX_RE_CONNECTION_WAIT_TIME = 5 * 60 * 1000; // 5 minutes
    private static final long INITIAL_RE_CONNECTION_WAIT_TIME = 250;
    private final Logger logger = Logger.getLogger(getClass());
    private final AtomicInteger reConnectionAttempt = new AtomicInteger();
    private final StreamHandler handler;
    private final Meter inboundActivityCountMetric;
    private long reConnectionWaitTime = INITIAL_RE_CONNECTION_WAIT_TIME;
    private TaskManager taskManager;
    private GnipHttpClient client;
    private AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private InputStream inputStream;
    private BufferedReader reader;

    @Inject
    public StreamProcessor(GnipHttpClient client,
                           StreamHandler handler,
                           TaskManager taskManager,
                           MetricRegistry metricRegistry) {
        this.client = client;
        this.handler = handler;
        this.taskManager = taskManager;

        inboundActivityCountMetric = metricRegistry.meter("Inbound Activities Count");

        makeConnectionWithClient();
    }

    private void makeConnectionWithClient() {
        try {
            inputStream = client.getStreaming();
            reader = new BufferedReader(
                    new InputStreamReader(
                            new StreamingGZIPInputStream(inputStream), StandardCharsets.UTF_8));
        } catch (IOException e) {
            handler.notifyConnectionError(e);
        }

        logger.info("Succesfully made streaming connection to");
    }

    @Override
    public void run() {
        while (!shuttingDown.get() && !Thread.interrupted()) {
            if (inputStream == null) {
                reconnect();
            }
            try {
                while (inputStream != null && reader != null) {
                    final String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        inboundActivityCountMetric.mark();
                        taskManager.submit(new Runnable() {
                            @Override
                            public void run() {
                                handler.handleMessage(line);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                // I/O Error on stream channel
            } catch (final Throwable e) {
                // Unexpected error processing channel
            } finally {
                closeInputStream();
            }

        }
    }

    private void closeInputStream() {
        logger.info("Closing input stream");
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ignore) {
            }
            inputStream = null;
        }
    }

    private void reconnect() {
        logger.info("Attempting reconnect");
        try {
            reConnectionAttempt.incrementAndGet();
            reConnectionWaitTime = (reConnectionWaitTime * 2);
            reConnectionWaitTime = (reConnectionWaitTime > MAX_RE_CONNECTION_WAIT_TIME)
                    ? MAX_RE_CONNECTION_WAIT_TIME : reConnectionWaitTime;
            try {
                Thread.sleep(reConnectionWaitTime);
            } catch (final InterruptedException e) {

            }
            makeConnectionWithClient();
            reConnectionAttempt.set(0);
            reConnectionWaitTime = INITIAL_RE_CONNECTION_WAIT_TIME;
        } catch (final Throwable e) {
            logger.error("Error attempting reconnect");
        }
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
}
