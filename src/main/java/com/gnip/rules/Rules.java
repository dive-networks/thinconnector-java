package com.gnip.rules;

import java.util.ArrayList;
import java.util.List;

public class Rules {
    List<Rule> rules = new ArrayList<>();

    public Rules() {
    }

    public Rules(List<Rule> rules) {
        this.rules = rules;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

}
