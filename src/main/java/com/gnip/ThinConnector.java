package com.gnip;

import com.gnip.rules.Rule;
import com.gnip.rules.Rules;
import com.gnip.stream.DefaultStreamHandler;
import com.gnip.stream.GnipStream;
import com.gnip.stream.StreamHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ThinConnector {
    private final static Logger logger = Logger.getLogger(ThinConnector.class.getName());

    public static void main(String[] args) {
        ThinConnector thinConnector = new ThinConnector();
        try {
            thinConnector.go();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error occured.", e);
        }
    }

    public void go() throws Exception {
        ClientConfig clientConfig = ClientConfig.getInstance();

        StreamHandler streamHandler = new DefaultStreamHandler();

        GnipStream gnipStream = new GnipStream(streamHandler, clientConfig);

        boolean connected = false;

        // Aggressively try to connect... for better or worse :)
        while (!gnipStream.connected()) {
            connected = gnipStream.establishConnection();
        }

        Rules rules = gnipStream.listRules();

        for (Rule rule : rules.getRules()) {
            logger.info(rule.toString());
        }

        gnipStream.addRule(new Rule("wombat"));

        gnipStream.addRule(new Rule("honeybadger"));

        gnipStream.deleteRule(new Rule("wombat"));

        rules = gnipStream.listRules();

        for (Rule rule : rules.getRules()) {
            logger.info(rule.toString());
        }

        // Blocks
        gnipStream.stream();

        gnipStream.close();
    }
}
