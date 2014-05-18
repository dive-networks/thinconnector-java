package com.gnip;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.gnip.stream.GnipStream;
import com.gnip.stream.MongoStreamHandler;
import com.gnip.stream.StreamHandler;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class IngesteratorApplication {
    private final static Logger logger = LogManager.getLogger(IngesteratorApplication.class);

    public static void main(String[] args) {
        IngesteratorApplication ingesteratorApplication = new IngesteratorApplication();

        try {
            ingesteratorApplication.start();
        } catch (Exception e) {
            logger.error("Unexpected error occured.", e);
        }
    }

    public void start() throws Exception {
        // Create Guice injector
        // Get GnipStream
        // Stream the datas
        Injector injector = Guice.createInjector(new IngesteratorModule());
        GnipStream gnipAPI = injector.getInstance(GnipStream.class);
        gnipAPI.stream();
        // Do Rule MGMT
        boolean closeStream = false;
        while(!closeStream){
            // Wait for CMD line input
            // Parse CMD & args
            // - addrule, deleterule, listrules, closeStream
            // gnipApi.command(args)
        }
    }

    public class IngesteratorModule extends AbstractModule {
        private final MetricRegistry metricRegistry;

        public IngesteratorModule() {
            metricRegistry = new MetricRegistry();
            final Slf4jReporter reporter = Slf4jReporter.forRegistry(metricRegistry)
                    .outputTo(LoggerFactory.getLogger("com.gnip.metrics"))
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();

            reporter.start(10, TimeUnit.SECONDS);
        }

        @Override
        protected void configure() {
            bind(StreamHandler.class).to(MongoStreamHandler.class);
            bind(MetricRegistry.class).toInstance(metricRegistry);
        }
    }
}
