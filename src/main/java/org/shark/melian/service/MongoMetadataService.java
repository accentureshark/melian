package org.shark.melian.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import org.shark.melian.model.ColumnMetadataDto;
import org.shark.melian.model.MongoDatabaseMetadataDto;
import org.shark.melian.model.TableMetadataDto;
import org.shark.melian.model.TableShortDto;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

/*
 * Extraer metadatos de colecciones MongoDB: nombres de colecciones, campos comunes, y tipos básicos si se puede inferir.
 * Se encarga de leer las colecciones existentes, inspeccionar los documentos y generar una estructura con información 
 * sobre las tablas (colecciones) y sus campos (columnas).
 */
@Service("mongoMetadataService")
public class MongoMetadataService implements MetadataService<MongoDatabaseMetadataDto> {
	
	@Autowired
    private MongoTemplate mongoTemplate;

	//Recorremos todas las colecciones de la bbdd, analizamos una muestra de documentos y deducimos los nombres de campos y tipos de datos
  @Override
  public MongoDatabaseMetadataDto extractMetadata() {
    	
        List<TableMetadataDto> tables = new ArrayList<>();

        // Iteramos sobre cada coleccción (simil tablas en sql)
        for (String collectionName : mongoTemplate.getCollectionNames()) {
        	
            MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
            FindIterable<Document> docs = collection.find().limit(10000);

            //Deducimos los tipos de datos de los campos
            Map<String, String> fieldTypes = new HashMap<>();
            
            for (Document doc : docs) {
                for (Map.Entry<String, Object> entry : doc.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    if (!fieldTypes.containsKey(key)) {
                        fieldTypes.put(key, val != null ? val.getClass().getSimpleName() : "Unknown");
                    }
                }
            }

            //Crea una lista de columnas (campos) para la tabla
            List<ColumnMetadataDto> columns = fieldTypes.entrySet().stream()
                    .map(e -> ColumnMetadataDto.builder()
                            .name(e.getKey())
                            .type(e.getValue())
                            .primaryKey(e.getKey().equals("_id")) // _id clave primaria de Mongo
                            .foreignKey(false) // Mongo no usa claves foráneas explícitamente
                            .description(null)
                            .build())
                    .collect(Collectors.toList());

            //Agregamos esta colección como una "tabla" con columnas
            tables.add(new TableMetadataDto(collectionName, columns, Collections.emptyList()));
        }

        //Devuelve los metadatos de toda la base
        return new MongoDatabaseMetadataDto(tables);
        
    }

    //Método que devuelve una lista con los nombres de las colecciones como un resumen, sin inspeccionar documentos
    @Override
    public List<TableShortDto> extractShortSummary() {
    	
        return mongoTemplate.getCollectionNames().stream()
                .map(name -> new TableShortDto(name, Collections.emptyList(), Collections.emptyList()))
                .collect(Collectors.toList());
        
    }

}
