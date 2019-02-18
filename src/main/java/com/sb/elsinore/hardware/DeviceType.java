package com.sb.elsinore.hardware;

public enum DeviceType {

    TEMPERATURE("28");

    private String prefix;

    DeviceType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return this.prefix;
    }
}
