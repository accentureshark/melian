package org.shark.melian.model;

import java.time.LocalDateTime;

public record FileResult(
        String filename,
        String filepath,
        String content,
        long size,
        LocalDateTime lastModified,
        String mimeType
) {
}