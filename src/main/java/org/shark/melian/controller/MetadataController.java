package org.shark.melian.controller;


import org.shark.melian.model.TableShortDto;
import org.shark.melian.service.MetadataService;
import org.shark.melian.model.DatabaseMetadataDto;
import org.shark.melian.model.McpMetadataDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.shark.melian.model.MongoDatabaseMetadataDto;

import java.util.List;

@RestController
@RequestMapping("/mcp/metadata")
public class MetadataController {

    private final MetadataService<DatabaseMetadataDto> sqlMetadataService;
    private final MetadataService<McpMetadataDto> restApiMetadataService;
    private final MetadataService<MongoDatabaseMetadataDto> mongoMetadataService;

    public MetadataController(
            @Qualifier("sqlMetadataService") MetadataService<DatabaseMetadataDto> sqlMetadataService,
            @Qualifier("restApiMetadataService") MetadataService<McpMetadataDto> restApiMetadataService,
            @Qualifier("mongoMetadataService") MetadataService<MongoDatabaseMetadataDto> mongoMetadataService
    ) {
        this.sqlMetadataService = sqlMetadataService;
        this.restApiMetadataService = restApiMetadataService;
        this.mongoMetadataService = mongoMetadataService;
    }

    @GetMapping("/short")
    public List<TableShortDto> getShortSummary(@RequestParam(name = "source", defaultValue = "sql") String source) {
        if ("rest".equalsIgnoreCase(source)) {
            return restApiMetadataService.extractShortSummary();
        }

        if ("mongodb".equalsIgnoreCase(source)) {
            return mongoMetadataService.extractShortSummary();
        }

        // Default to SQL
        return sqlMetadataService.extractShortSummary();
    }

    /**
     * Returns the complete metadata description for the selected source.
     * If {@code source=rest} a {@link McpMetadataDto} is returned, otherwise a
     * {@link DatabaseMetadataDto}.
     */
    @GetMapping
    public Object getMetadata(@RequestParam(name = "source", defaultValue = "sql") String source) {
        if ("rest".equalsIgnoreCase(source)) {
            return restApiMetadataService.extractMetadata();
        }

        if ("mongodb".equalsIgnoreCase(source)) {
            return mongoMetadataService.extractMetadata();
        }

        // Default to SQL
        return sqlMetadataService.extractMetadata();
    }
}
