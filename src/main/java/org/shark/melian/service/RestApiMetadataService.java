package org.shark.melian.service;


import org.shark.melian.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("restApiMetadataService")
public class RestApiMetadataService implements MetadataService {

    // Tablas virtuales con metadata descriptiva
    private List<TableMetadataDto> tmdbVirtualTables() {
        List<TableMetadataDto> tables = new ArrayList<>();

        tables.add(new TableMetadataDto("film", List.of(
                ColumnMetadataDto.builder().name("film_id").type("string").primaryKey(true).description("ID interno de la película en TMDB").build(),
                ColumnMetadataDto.builder().name("title").type("string").description("Título de la película").build(),
                ColumnMetadataDto.builder().name("description").type("string").description("Resumen o sinopsis de la película").build(),
                ColumnMetadataDto.builder().name("release_year").type("string").description("Año de estreno (YYYY)").build(),
                ColumnMetadataDto.builder().name("imdb_rating").type("double").description("Puntaje promedio en IMDb o TMDB (0-10)").build(),
                ColumnMetadataDto.builder().name("director").type("string").description("Nombre del director si se puede obtener (placeholder)").build()
        ), List.of()));

        return tables;
    }


    @Override
    public DatabaseMetadataDto extractMetadata() {
        DatabaseMetadataDto result = new DatabaseMetadataDto();
        result.setTables(tmdbVirtualTables());
        return result;
    }

    @Override
    public List<TableShortDto> extractShortSummary() {
        List<TableShortDto> summary = new ArrayList<>();
        for (TableMetadataDto table : tmdbVirtualTables()) {
            List<ColumnShortDto> colShorts = table.getColumns().stream()
                    .map(col -> new ColumnShortDto(col.getName(), col.getType()))
                    .toList();
            summary.add(new TableShortDto(table.getName(), colShorts, List.of()));
        }
        return summary;
    }
}
