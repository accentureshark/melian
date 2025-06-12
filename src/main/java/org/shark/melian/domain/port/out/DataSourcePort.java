package org.shark.melian.domain.port.out;



import java.util.List;

public interface DataSourcePort {
    List<String> fetchRawChunks(String queryOrConfig);
}
