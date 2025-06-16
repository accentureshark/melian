package org.shark.melian.mcp.service;


import org.shark.melian.mcp.model.DatabaseMetadataDto;
import org.shark.melian.mcp.model.TableShortDto;

import java.util.List;

public interface MetadataService {
    DatabaseMetadataDto extractMetadata();

    List<TableShortDto> extractShortSummary();
}
