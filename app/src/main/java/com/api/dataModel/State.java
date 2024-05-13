package com.api.dataModel;

import java.util.Map;

public class State {
    private String entity_id;
    private Map<String, Object> attributes;

    // Getters and setters
    public String getEntity_id() {
        return entity_id;
    }

    public void setEntity_id(String entity_id) {
        this.entity_id = entity_id;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
