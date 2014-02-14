package com.gnip.rules;

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
}
