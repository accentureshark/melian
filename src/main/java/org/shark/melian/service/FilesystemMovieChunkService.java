package org.shark.melian.service;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.FileResult;
import org.shark.melian.model.MovieResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Filesystem-based implementation of MovieChunkService for handling file-based chunks.
 */
@Service("filesystemMovieChunkService")
public class FilesystemMovieChunkService implements MovieChunkService {
    
    private static final Logger log = Logger.getLogger(FilesystemMovieChunkService.class.getName());
    
    private final FilesystemScannerService filesystemScannerService;
    private final Map<String, List<ChunkDto>> chunkStorage = new HashMap<>();
    
    public FilesystemMovieChunkService(FilesystemScannerService filesystemScannerService) {
        this.filesystemScannerService = filesystemScannerService;
        log.info("[FilesystemMovieChunkService] Initialized");
    }
    
    @Override
    public void storeMovies(List<MovieResult> movies, String source) {
        log.info("[FilesystemMovieChunkService] Storing " + movies.size() + " movies from source: " + source);
        
        List<ChunkDto> chunks = new ArrayList<>();
        for (MovieResult movie : movies) {
            ChunkDto chunk = new ChunkDto();
            chunk.setId(source + "_" + movie.title().hashCode());
            chunk.setText(String.format("Title: %s\nOverview: %s\nRelease Date: %s\nRating: %.2f",
                    movie.title(), movie.overview(), movie.releaseDate(), movie.rating()));
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", movie.title());
            metadata.put("releaseDate", movie.releaseDate());
            metadata.put("rating", movie.rating());
            metadata.put("source", source);
            chunk.setMetadata(metadata);
            chunk.setSource(source);
            
            chunks.add(chunk);
        }
        
        chunkStorage.put(source, chunks);
        log.info("[FilesystemMovieChunkService] Stored " + chunks.size() + " chunks");
    }
    
    /**
     * Store file results as chunks
     */
    public void storeFiles(List<FileResult> files, String source) {
        log.info("[FilesystemMovieChunkService] Storing " + files.size() + " files from source: " + source);
        
        List<ChunkDto> chunks = new ArrayList<>();
        for (FileResult file : files) {
            ChunkDto chunk = new ChunkDto();
            chunk.setId(source + "_" + file.filename().hashCode());
            chunk.setText(file.content());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", file.filename());
            metadata.put("filepath", file.filepath());
            metadata.put("size", file.size());
            metadata.put("lastModified", file.lastModified().toString());
            metadata.put("mimeType", file.mimeType());
            metadata.put("source", source);
            chunk.setMetadata(metadata);
            chunk.setSource(source);
            
            chunks.add(chunk);
        }
        
        chunkStorage.put(source, chunks);
        log.info("[FilesystemMovieChunkService] Stored " + chunks.size() + " file chunks");
    }
    
    @Override
    public List<ChunkDto> getMovieChunks(String source, int limit, String afterId, String filter, List<String> tags, String sort) {
        log.info("[FilesystemMovieChunkService] Getting chunks for source: " + source + ", limit: " + limit + ", filter: " + filter);
        
        List<ChunkDto> chunks = chunkStorage.getOrDefault(source, new ArrayList<>());
        
        // Apply filter if provided
        if (filter != null && !filter.trim().isEmpty()) {
            chunks = chunks.stream()
                    .filter(chunk -> chunk.getText().toLowerCase().contains(filter.toLowerCase()) ||
                            (chunk.getMetadata() != null && chunk.getMetadata().toString().toLowerCase().contains(filter.toLowerCase())))
                    .collect(Collectors.toList());
        }
        
        // Apply afterId pagination if provided
        if (afterId != null && !afterId.trim().isEmpty()) {
            boolean foundAfter = false;
            List<ChunkDto> filteredChunks = new ArrayList<>();
            for (ChunkDto chunk : chunks) {
                if (foundAfter) {
                    filteredChunks.add(chunk);
                } else if (afterId.equals(chunk.getId())) {
                    foundAfter = true;
                }
            }
            chunks = filteredChunks;
        }
        
        // Apply limit
        if (limit > 0 && chunks.size() > limit) {
            chunks = chunks.subList(0, limit);
        }
        
        log.info("[FilesystemMovieChunkService] Returning " + chunks.size() + " chunks");
        return chunks;
    }
    
    @Override
    public List<MovieResult> searchAndStore(String title, int limit, boolean store) {
        log.info("[FilesystemMovieChunkService] Searching and storing files with title filter: " + title);
        
        // Search for files (treating title as filename filter)
        List<FileResult> files = filesystemScannerService.scanDirectory(title, limit);
        
        if (store) {
            storeFiles(files, "filesystem");
        }
        
        // Convert FileResult to MovieResult for compatibility
        List<MovieResult> results = files.stream()
                .map(file -> new MovieResult(
                        file.filename(),
                        file.content(),
                        file.lastModified().toString(),
                        file.size() // Using size as rating for now
                ))
                .collect(Collectors.toList());
        
        log.info("[FilesystemMovieChunkService] Returning " + results.size() + " file results as movies");
        return results;
    }
    
    /**
     * Search for PDF files specifically
     */
    public List<FileResult> searchAndStorePDFs(String filenameFilter, int limit, boolean store) {
        log.info("[FilesystemMovieChunkService] Searching and storing PDF files with filter: " + filenameFilter);
        
        List<FileResult> files = filesystemScannerService.scanForPDFs(filenameFilter, limit);
        
        if (store) {
            storeFiles(files, "filesystem_pdf");
        }
        
        return files;
    }
}