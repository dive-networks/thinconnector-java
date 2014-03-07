package com.gnip.rules;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gnip.parsing.JSONUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Rules implements Iterable<Rule> {
    List<Rule> rules = new ArrayList<>();

    public Rules() {
    }

    public Rules(Rule rule) {
        this.rules.add(rule);
    }

    public List<Rule> getRules() {
        return rules;
    }

    public String build() throws IOException {
        ArrayNode arrayNode = JSONUtils.getObjectMapper().createArrayNode();
        for (Rule rule : rules) {
            arrayNode.add(JSONUtils.parseTree(rule.toString()));
        }
        return arrayNode.toString();
    }

    @Override
    public Iterator<Rule> iterator() {
        return rules.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Rule rule : rules) {
            sb.append(rule);
        }
        return sb.toString();
    }
}
