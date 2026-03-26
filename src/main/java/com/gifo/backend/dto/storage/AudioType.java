package com.gifo.backend.dto.storage;

public enum AudioType {
    BGM("bgm/");

    private final String prefix;

    AudioType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
