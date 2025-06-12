package org.shark.melian.adapter.out.sql;



import org.shark.melian.domain.port.out.DataSourcePort;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SqlDataSourceAdapter implements DataSourcePort {
    @Override
    public List<String> fetchRawChunks(String queryOrConfig) {
        // Implementación real usando JDBC, etc.
        return List.of();
    }
}
