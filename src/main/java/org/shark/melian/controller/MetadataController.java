package org.shark.melian.controller;


import org.shark.melian.model.TableShortDto;
import org.shark.melian.service.MetadataService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mcp/metadata")
public class MetadataController {

    private final MetadataService sqlMetadataService;
    private final MetadataService restApiMetadataService;

    public MetadataController(
            @Qualifier("sqlMetadataService") MetadataService sqlMetadataService,
            @Qualifier("restApiMetadataService") MetadataService restApiMetadataService
    ) {
        this.sqlMetadataService = sqlMetadataService;
        this.restApiMetadataService = restApiMetadataService;
    }

    @GetMapping("/short")
    public List<TableShortDto> getShortSummary(@RequestParam(name = "source", defaultValue = "sql") String source) {
        if ("rest".equalsIgnoreCase(source)) {
            return restApiMetadataService.extractShortSummary();
        }
        return sqlMetadataService.extractShortSummary();
    }
}
