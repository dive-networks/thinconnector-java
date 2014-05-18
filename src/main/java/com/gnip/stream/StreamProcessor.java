package com.gnip.stream;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.gnip.connection.GnipHttpClient;
import com.gnip.utilities.TaskManager;
import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

public class StreamProcessor implements Runnable {
    private static final long MAX_RE_CONNECTION_WAIT_TIME = 2 * 60 * 1000; // 2 minutes
    private static final long INITIAL_RE_CONNECTION_WAIT_TIME = 250;
    private final Logger logger = LogManager.getLogger(getClass());
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

        // Set up metrics we want to use
        inboundActivityCountMetric = metricRegistry.meter("Inbound Activities Count");

        // Create streaming connection using httpClient
        makeStreamingConnectionWithClient();
    }

    private void makeStreamingConnectionWithClient() {
        // Get an input stream for the streaming connection and create a reader for it
        // Handle and log connection error or successful response
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
        //TODO: ensure that BufferedReader.readLine() does not break on newlines within activities
        // Use reader to iterate stream and hand off messages to handler on a separate thread.
        // Properly handle and log any I/O exceptions on the underlying resources.
        // Implement reconnect logic if a disconnect is detected
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
        // Close InputStream to free resources when finished with them
        // For instance, when shutting down or when reconnecting
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
        // Make a reconnection attempt
        // Possibly implement a back off strategy here as to not hammer with connection attempts when something is wrong
        // Log connection attempts and failures
        logger.info("Attempting reconnect");
        try {
            reConnectionAttempt.incrementAndGet();
            reConnectionWaitTime = (reConnectionWaitTime > MAX_RE_CONNECTION_WAIT_TIME)
                    ? MAX_RE_CONNECTION_WAIT_TIME : reConnectionWaitTime;
            try {
                Thread.sleep(reConnectionWaitTime);
            } catch (final InterruptedException e) {

            }
            reConnectionWaitTime = (reConnectionWaitTime * 2);
            makeStreamingConnectionWithClient();
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
