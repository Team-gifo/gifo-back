package com.gifo.backend.dto.storage;

public enum ImageType {
    MEMORY("memories/"),
    GIFT("gifts/"),
    QUIZ("quizzes/");

    private final String prefix;

    ImageType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
