package org.shark.melian.mcp.provider;

import org.shark.melian.mcp.model.DatabaseMetadataDto;
import org.shark.melian.mcp.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SqlMetadataProvider {

    @Autowired
    @Qualifier("sqlMetadataService")
    private MetadataService sqlMetadataService;

    public DatabaseMetadataDto getSchema() {
        return sqlMetadataService.extractMetadata();
    }
}
