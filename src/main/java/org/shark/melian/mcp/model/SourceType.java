package org.shark.melian.mcp.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SourceType {
    SQL("sql"),
    TMDB("tmdb"),
    REST("rest");

    private final String value;

    public static SourceType from(String input) {
        if (input == null) return SQL; // Fallback default
        for (SourceType type : values()) {
            if (type.value.equalsIgnoreCase(input)) return type;
        }
        throw new IllegalArgumentException("Unsupported source: " + input);
    }
}

