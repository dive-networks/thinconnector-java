package com.gnip.stream;

import com.gnip.Environment;
import com.gnip.rules.Rule;
import com.gnip.rules.Rules;
import com.gnip.utilities.TaskManager;
import com.google.inject.Inject;
import org.apache.log4j.Logger;

public class GnipStream {
    private final Logger logger = Logger.getLogger(getClass());
    private final Environment environment;
    private final StreamProcessor processor;
    private TaskManager taskManager;

    @Inject
    public GnipStream(Environment environment,
                      StreamProcessor processor,
                      TaskManager taskManager) {
        this.environment = environment;
        this.processor = processor;
        this.taskManager = taskManager;
    }

    public void stream() {
        logger.info("Starting streaming for " + environment.streamLabel());
        taskManager.submit(processor);
    }

    public boolean addRule(Rule rule) {
        return true;
    }

    public boolean deleteRule(Rule rule) {
        return true;
    }

    public Rules listRules() {
        return new Rules();
    }

}
