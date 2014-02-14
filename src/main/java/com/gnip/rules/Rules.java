package com.gnip.rules;

import com.oracle.javafx.jmx.json.JSONDocument;

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

    public String build() {
        JSONDocument array = JSONDocument.createArray(1);
        Rule rule;
        for (int i = 0; i < rules.size(); i++) {
            rule = rules.get(i);
            JSONDocument jsonRule = new JSONDocument(JSONDocument.Type.OBJECT);
            if (rule.getTag() != null) {
                jsonRule.setString("tag", rule.getTag());
            }
            jsonRule.setString("value", rule.getValue());
            array.set(i, jsonRule);
        }
        JSONDocument rules = JSONDocument.createObject();
        rules.set("rules", array);
        return rules.toJSON();
    }

    @Override
    public Iterator<Rule> iterator() {
        return rules.iterator();
    }
}
