package org.shark.melian.service;


import org.shark.melian.model.DatabaseMetadataDto;
import org.shark.melian.model.TableShortDto;

import java.util.List;

public interface MetadataService {
    DatabaseMetadataDto extractMetadata();

    List<TableShortDto> extractShortSummary();
}
