package org.shark.melian.service;


import org.shark.melian.model.TableShortDto;

/**
 * Generic contract for extracting metadata information. Implementations may
 * return either a {@link org.shark.melian.model.DatabaseMetadataDto} or a
 * {@link org.shark.melian.model.McpMetadataDto} depending on the backend.
 */

import java.util.List;

public interface MetadataService<T> {
    /**
     * Extracts full metadata representation. Implementations decide the
     * concrete DTO returned.
     */
    T extractMetadata();

    List<TableShortDto> extractShortSummary();
}
