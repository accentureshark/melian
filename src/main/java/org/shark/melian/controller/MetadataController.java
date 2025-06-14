package org.shark.melian.controller;


import org.shark.melian.model.DatabaseMetadataDto;
import org.shark.melian.model.TableShortDto;
import org.shark.melian.service.MetadataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mcp")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/metadata")
    public DatabaseMetadataDto getDatabaseMetadata() {
        return metadataService.extractMetadata();
    }

    @GetMapping("/metadata/short")
    public List<TableShortDto> getShortMetadataJson() {
        return metadataService.extractShortSummary();
    }

}
