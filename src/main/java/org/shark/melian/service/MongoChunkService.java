package org.shark.melian.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import org.shark.melian.model.ChunkDto;

/*
 * Leer documentos desde una colección MongoDB, con paginación, filtros simples (igualdad o like), y devolver una lista de ChunkDto
 */

@Service("mongoChunkService")
public class MongoChunkService implements ChunkService {
	
	private static final Logger log = Logger.getLogger(MongoChunkService.class.getName());

	 //Hacer las consultas directamente sobre la base MongoDB
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ChunkDto> getChunks(String collection,
                                    String source,
                                    int limit,
                                    String afterId,
                                    String filter,
                                    List<String> tags,
                                    String sort) {
    	
        log.info("[MongoChunkService] Obteniendo chunks para colección: " + collection);

        Query query = new Query();
        
        //Filtro afterId (paginación)
        //Si se proporciona un afterId, se aplica un filtro para traer solo los documentos con _id mayor (útil para paginación eficiente)
        if (afterId != null && !afterId.isBlank()) {
        	
            query.addCriteria(Criteria.where("_id").gt(new ObjectId(afterId)));
            
        }

        // Filtro simple
        //Permite dos tipos de filtros simples: campo LIKE 'valor' -> usa regex y campo = 'valor' -> usa igualdad
        if (filter != null && !filter.isBlank()) {
        	
            if (filter.toLowerCase().contains(" like ")) {
            	
                String[] parts = filter.split("(?i)like", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim()); //cleanQuotes() se asegura de quitar las comillas simples o dobles del valor.
                query.addCriteria(Criteria.where(field).regex(val, "i"));
                
            } else if (filter.contains("=")) {
            	
                String[] parts = filter.split("=", 2);
                String field = parts[0].trim();
                String val = cleanQuotes(parts[1].trim());
                query.addCriteria(Criteria.where(field).is(val));
                
            }
            
        }

        // Orden (por defecto por _id)
        //Ordena los resultados por el campo sort (ascendente)
        if (sort != null && !sort.isBlank()) {
        	
            query.with(Sort.by(Sort.Order.asc(sort)));
            
        } else {
        	
        	//Si no se indica ninguno, usa _id
            query.with(Sort.by(Sort.Order.asc("_id")));
            
        }

        //Limita la cantidad de documentos devueltos
        query.limit(limit);

        //Ejecutamos la consulta y obtenemos una lista de documentos MongoDB
        List<Document> documents = mongoTemplate.find(query, Document.class, collection);

        //Transformamos cada documento en un ChunkDto con: el _id como identificador, el contenido en formato JSON como texto,
        //el documento original como metadatos
        List<ChunkDto> chunks = new ArrayList<>();
        
        for (Document doc : documents) {
        	
            ChunkDto chunk = new ChunkDto();
            chunk.setId(doc.getObjectId("_id").toHexString());
            chunk.setText(doc.toJson());
            chunk.setMetadata(doc);
            chunks.add(chunk);
            
        }

        return chunks; //Un chunk representa un fragmento de información de una colección de MongoDB, es decir, un documento Mongo transformado a un DTO estandarizado que puede usarse en el sistema
        
    }

    //Elimina comillas simples o dobles de los extremos del valor del filtro
    private String cleanQuotes(String val) {
    	
        val = val.trim();
        
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            return val.substring(1, val.length() - 1);
        }
        
        return val;
        
    }

}
