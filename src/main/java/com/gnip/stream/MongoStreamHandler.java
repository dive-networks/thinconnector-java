package com.gnip.stream;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public class MongoStreamHandler implements StreamHandler {
    private final DBCollection messageTable;
    Logger logger = Logger.getLogger(StreamHandler.class);
    private MetricRegistry metricRegistry;

    @Inject
    public MongoStreamHandler(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;

        MongoClient mongo = null;
        try {
            mongo = new MongoClient("localhost", 27017);
        } catch (UnknownHostException e) {
            logger.error("Error connecting to DB", e);
        }
        DB db = mongo.getDB("test");

        messageTable = db.getCollection("messages");
    }

    @Override
    public void handleMessage(String message) {
        DBObject parsed = (DBObject) JSON.parse(message);
        messageTable.insert(parsed);
    }

    @Override
    public void notifyDisconnect(IOException e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyConnected(String streamName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyConnectionError(IOException e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
