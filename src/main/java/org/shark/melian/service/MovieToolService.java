package org.shark.melian.service;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.FileResult;
import org.shark.melian.model.MovieResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

/**
 * MCP-compliant Movie Tool Service providing AI tools for movie search and storage.
 * Integrates with both SQL and MongoDB storage backends, plus filesystem scanning.
 */
@Service
public class MovieToolService {
    
    private static final Logger log = Logger.getLogger(MovieToolService.class.getName());
    
    private final TMDBService tmdbService;
    private final MovieChunkService sqlMovieChunkService;
    private final MovieChunkService mongoMovieChunkService;
    private final FilesystemMovieChunkService filesystemMovieChunkService;

    public MovieToolService(
            TMDBService tmdbService,
            @Qualifier("sqlMovieChunkService") MovieChunkService sqlMovieChunkService,
            @Qualifier("mongoMovieChunkService") MovieChunkService mongoMovieChunkService,
            @Qualifier("filesystemMovieChunkService") FilesystemMovieChunkService filesystemMovieChunkService
    ) {
        this.tmdbService = tmdbService;
        this.sqlMovieChunkService = sqlMovieChunkService;
        this.mongoMovieChunkService = mongoMovieChunkService;
        this.filesystemMovieChunkService = filesystemMovieChunkService;
        log.info("[MovieToolService] Initialized with MCP-compliant services including filesystem");
    }

    @Tool(name = "search_movies_by_mcp_server", description = "Busca películas por título usando el servidor MCP especificado (tmdb o mongo)")
    public List<MovieResult> searchMoviesByMcpServer(String mcpServer, String title, Integer limit) {
        int realLimit = limit != null ? limit : 3;
        switch (mcpServer.toLowerCase()) {
            case "tmdb":
                return tmdbService.search(title, realLimit);
            case "mongo":
                // Solo busca en Mongo, no almacena
                return mongoMovieChunkService.searchAndStore(title, realLimit, false);
            default:
                throw new IllegalArgumentException("Servidor MCP no soportado: " + mcpServer);
        }
    }

    @Tool(name = "search_movies_by_tmdb_api", description = "Busca peliculas por titulo usando la API de TMDB")
    public List<MovieResult> searchMovies(String title, Integer limit) {
        return tmdbService.search(title, limit != null ? limit : 3);
    }
    
    @Tool(name = "search_and_store_movies_sql", description = "Busca peliculas y las almacena en base de datos SQL")
    public List<MovieResult> searchAndStoreMoviesSQL(String title, Integer limit) {
        return sqlMovieChunkService.searchAndStore(title, limit != null ? limit : 3, true);
    }
    
    @Tool(name = "search_and_store_movies_mongo", description = "Busca peliculas y las almacena en MongoDB")
    public List<MovieResult> searchAndStoreMoviesMongo(String title, Integer limit) {
        return mongoMovieChunkService.searchAndStore(title, limit != null ? limit : 3, true);
    }
    
    @Tool(name = "get_stored_movies_sql", description = "Obtiene peliculas almacenadas desde SQL como chunks MCP")
    public List<ChunkDto> getStoredMoviesSQL(String filter, Integer limit, String afterId) {
        return sqlMovieChunkService.getMovieChunks("tmdb", limit != null ? limit : 10, afterId, filter, null, null);
    }
    
    @Tool(name = "get_stored_movies_mongo", description = "Obtiene peliculas almacenadas desde MongoDB como chunks MCP") 
    public List<ChunkDto> getStoredMoviesMongo(String filter, Integer limit, String afterId) {
        return mongoMovieChunkService.getMovieChunks("tmdb", limit != null ? limit : 10, afterId, filter, null, null);
    }
    
    // ===== FILESYSTEM TOOLS =====
    
    @Tool(name = "scan_filesystem_directory", description = "Escanea un directorio del filesystem y devuelve información de archivos")
    public List<MovieResult> scanFilesystemDirectory(String filenameFilter, Integer limit) {
        return filesystemMovieChunkService.searchAndStore(filenameFilter, limit != null ? limit : 10, false);
    }
    
    @Tool(name = "scan_and_store_filesystem_files", description = "Escanea un directorio del filesystem y almacena los archivos como chunks")
    public List<MovieResult> scanAndStoreFilesystemFiles(String filenameFilter, Integer limit) {
        return filesystemMovieChunkService.searchAndStore(filenameFilter, limit != null ? limit : 10, true);
    }
    
    @Tool(name = "scan_pdf_files", description = "Escanea un directorio buscando archivos PDF y extrae su contenido")
    public List<FileResult> scanPDFFiles(String filenameFilter, Integer limit) {
        return filesystemMovieChunkService.searchAndStorePDFs(filenameFilter, limit != null ? limit : 10, false);
    }
    
    @Tool(name = "scan_and_store_pdf_files", description = "Escanea un directorio buscando archivos PDF, extrae su contenido y los almacena como chunks")
    public List<FileResult> scanAndStorePDFFiles(String filenameFilter, Integer limit) {
        return filesystemMovieChunkService.searchAndStorePDFs(filenameFilter, limit != null ? limit : 10, true);
    }
    
    @Tool(name = "get_stored_filesystem_files", description = "Obtiene archivos del filesystem almacenados como chunks MCP")
    public List<ChunkDto> getStoredFilesystemFiles(String filter, Integer limit, String afterId) {
        return filesystemMovieChunkService.getMovieChunks("filesystem", limit != null ? limit : 10, afterId, filter, null, null);
    }
    
    @Tool(name = "get_stored_pdf_files", description = "Obtiene archivos PDF almacenados como chunks MCP")
    public List<ChunkDto> getStoredPDFFiles(String filter, Integer limit, String afterId) {
        return filesystemMovieChunkService.getMovieChunks("filesystem_pdf", limit != null ? limit : 10, afterId, filter, null, null);
    }
}
