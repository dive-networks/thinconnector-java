package com.gnip;

import com.gnip.rules.Rule;
import com.gnip.rules.Rules;
import com.gnip.stream.DefaultStreamHandler;
import com.gnip.stream.GnipStream;
import com.gnip.stream.StreamHandler;
import org.apache.log4j.Logger;

public class ThinConnector {
    private final static Logger logger = Logger.getLogger(ThinConnector.class);

    public static void main(String[] args) {
        ThinConnector thinConnector = new ThinConnector();
        try {
            thinConnector.start();
        } catch (Exception e) {
            logger.error("Unexpected error occured.", e);
        }
    }

    public void start() throws Exception {
        ClientConfig clientConfig = ClientConfig.getInstance();

        StreamHandler streamHandler = new DefaultStreamHandler();

        GnipStream gnipStream = new GnipStream(streamHandler, clientConfig);

        // Aggressively try to connect... for better or worse :)
        while (!gnipStream.connected()) {
            gnipStream.establishConnection();
        }

        Rules rules = gnipStream.listRules();

        logger.info(rules.toString());

        gnipStream.addRule(new Rule("wombat"));

        gnipStream.addRule(new Rule("honeybadger"));

        gnipStream.deleteRule(new Rule("wombat"));

        rules = gnipStream.listRules();

        logger.info(rules.toString());

        // Blocks
        gnipStream.stream();

        gnipStream.close();
    }
}
