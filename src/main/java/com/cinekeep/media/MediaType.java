package com.cinekeep.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MediaType {
    MOVIE("movie"),
    TV("tv");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MediaType fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (MediaType mediaType : values()) {
            if (mediaType.value.equalsIgnoreCase(value)) {
                return mediaType;
            }
        }

        throw new IllegalArgumentException("Unsupported media type: " + value);
    }
}
