package com.gnip.stream;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public class DefaultStreamHandler implements StreamHandler {
    private final DBCollection messageTable;
    Logger logger = Logger.getLogger(StreamHandler.class);

    public DefaultStreamHandler() {
        MongoClient mongo = null;
        try {
            mongo = new MongoClient("localhost", 27017);
        } catch (UnknownHostException e) {
            logger.error("Error connecting to DB", e);
        }
        DB db = mongo.getDB("test");
        List<String> dbs = mongo.getDatabaseNames();

        messageTable = db.getCollection("messages");
    }

    @Override
    public void handleMessage(String message) {
        DBObject parsed = (DBObject) JSON.parse(message);
        messageTable.insert(parsed);
    }

    @Override
    public void notifyDisconnect(GnipStream gnipStream, IOException e) {

    }

    @Override
    public void notifyConnected(GnipStream gnipStream) {

    }

    @Override
    public void notifyConnectionError(GnipStream gnipStream, IOException e) {

    }

}
