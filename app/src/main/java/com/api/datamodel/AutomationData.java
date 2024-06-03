package com.api.datamodel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AutomationData {
    @SerializedName("description")
    private String description;
    @SerializedName("mode")
    private String mode;
    @SerializedName("trigger")
    private List<Trigger> triggers;
    @SerializedName("condition")
    private List<Object> conditions; // Assuming no specific structure for conditions
    @SerializedName("action")
    private List<Action> actions;
    @SerializedName("alias")
    private String alias;

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setConditions(List<Object> conditions) {
        this.conditions = conditions;
    }

    public List<Object> getConditions() {
        return conditions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public class Trigger {
        @SerializedName("platform")
        private String platform;
        @SerializedName("event")
        private String event;
        @SerializedName("offset")
        private String offset;

        // Getters and Setters
        public void setEvent(String event) {
            this.event = event;
        }

        public String getEvent() {
            return event;
        }

        public void setOffset(String offset) {
            this.offset = offset;
        }

        public String getOffset() {
            return offset;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getPlatform() {
            return platform;
        }
    }
    public class Action {
        public class Target {
            @SerializedName("entity_id")
            private String entity_id;

            public String getEntity_id() {
                return entity_id;
            }

            public void setEntity_id(String entity_id) {
                this.entity_id = entity_id;
            }
        }

        @SerializedName("service")
        private String service;
        @SerializedName("data")
        private Target data; // Assuming data is an object with no specific structure

        // Getters and Setters
        public void setService(String service) {
            this.service = service;
        }

        public String getService() {
            return service;
        }

        public void setData(Target data) {
            this.data = data;
        }

        public Target getData() {
            return data;
        }
    }
}
