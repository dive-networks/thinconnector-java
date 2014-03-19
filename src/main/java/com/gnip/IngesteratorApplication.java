package com.gnip;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.gnip.stream.GnipStream;
import com.gnip.stream.MongoStreamHandler;
import com.gnip.stream.StreamHandler;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class IngesteratorApplication {
    private final static Logger logger = Logger.getLogger(IngesteratorApplication.class);

    public static void main(String[] args) {
        IngesteratorApplication trainingApplication = new IngesteratorApplication();
        try {
            trainingApplication.start();
        } catch (Exception e) {
            logger.error("Unexpected error occured.", e);
        }
    }

    public void start() throws Exception {
        Injector injector = Guice.createInjector(new IngesteratorModule());
        GnipStream gnipStream = injector.getInstance(GnipStream.class);
        gnipStream.stream();
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
