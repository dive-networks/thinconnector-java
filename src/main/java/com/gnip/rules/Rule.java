package com.gnip.rules;

import com.oracle.javafx.jmx.json.JSONDocument;

public class Rule {
    private String value;
    private String tag = null;

    public Rule(String value) {
        this.value = value;
    }

    public Rule(String value, String tag) {
        this.value = value;
        this.tag = tag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRule() {
        JSONDocument array = JSONDocument.createArray(1);
        JSONDocument rule = new JSONDocument(JSONDocument.Type.OBJECT);
        if (tag != null) {
            rule.setString("tag", tag);
        }
        rule.setString("value", value);

        array.set(0, rule);

        JSONDocument rules = JSONDocument.createObject();
        rules.set("rules", array);
        return rules.toJSON();
    }

    @Override
    public String toString() {
        return "Rule{" +
                "value='" + value + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }
}
