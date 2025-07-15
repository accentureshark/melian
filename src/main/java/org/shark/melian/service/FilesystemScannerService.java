package org.shark.melian.service;

import org.shark.melian.model.FileResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Service for scanning filesystem directories and retrieving file information.
 * Similar to TMDBService but for local files.
 */
@Service
public class FilesystemScannerService {
    
    private static final Logger log = Logger.getLogger(FilesystemScannerService.class.getName());
    
    @Value("${melian.filesystem.scan.directory:/tmp/melian-scan}")
    private String scanDirectory;
    
    private final PDFParsingService pdfParsingService;
    
    public FilesystemScannerService(PDFParsingService pdfParsingService) {
        this.pdfParsingService = pdfParsingService;
        log.info("[FilesystemScannerService] Initialized");
    }
    
    /**
     * Scan directory for files with optional filename filter
     */
    public List<FileResult> scanDirectory(String filenameFilter, int limit) {
        log.info("[FilesystemScannerService] Scanning directory: " + scanDirectory + " with filter: " + filenameFilter);
        
        Path scanPath = Paths.get(scanDirectory);
        if (!Files.exists(scanPath)) {
            log.warning("[FilesystemScannerService] Directory does not exist: " + scanDirectory);
            return List.of();
        }
        
        List<FileResult> results = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(scanPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> filenameFilter == null || 
                        path.getFileName().toString().toLowerCase().contains(filenameFilter.toLowerCase()))
                 .limit(limit)
                 .forEach(path -> {
                     try {
                         FileResult fileResult = processFile(path);
                         if (fileResult != null) {
                             results.add(fileResult);
                         }
                     } catch (Exception e) {
                         log.warning("[FilesystemScannerService] Error processing file: " + path + " - " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            log.severe("[FilesystemScannerService] Error scanning directory: " + e.getMessage());
        }
        
        log.info("[FilesystemScannerService] Found " + results.size() + " file(s)");
        return results;
    }
    
    /**
     * Scan directory for PDF files specifically
     */
    public List<FileResult> scanForPDFs(String filenameFilter, int limit) {
        log.info("[FilesystemScannerService] Scanning for PDF files with filter: " + filenameFilter);
        
        Path scanPath = Paths.get(scanDirectory);
        if (!Files.exists(scanPath)) {
            log.warning("[FilesystemScannerService] Directory does not exist: " + scanDirectory);
            return List.of();
        }
        
        List<FileResult> results = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(scanPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                 .filter(path -> filenameFilter == null || 
                        path.getFileName().toString().toLowerCase().contains(filenameFilter.toLowerCase()))
                 .limit(limit)
                 .forEach(path -> {
                     try {
                         FileResult fileResult = processPDFFile(path);
                         if (fileResult != null) {
                             results.add(fileResult);
                         }
                     } catch (Exception e) {
                         log.warning("[FilesystemScannerService] Error processing PDF file: " + path + " - " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            log.severe("[FilesystemScannerService] Error scanning directory for PDFs: " + e.getMessage());
        }
        
        log.info("[FilesystemScannerService] Found " + results.size() + " PDF file(s)");
        return results;
    }
    
    private FileResult processFile(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString();
        String filepath = filePath.toString();
        long size = Files.size(filePath);
        LocalDateTime lastModified = LocalDateTime.ofInstant(
            Files.getLastModifiedTime(filePath).toInstant(), 
            ZoneId.systemDefault()
        );
        
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        
        // For now, we'll just read basic file info, not content
        String content = "File: " + filename + ", Size: " + size + " bytes";
        
        log.info(String.format("[FilesystemScannerService] File: %s | Size: %d bytes | Modified: %s", 
                filename, size, lastModified));
        
        return new FileResult(filename, filepath, content, size, lastModified, mimeType);
    }
    
    private FileResult processPDFFile(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString();
        String filepath = filePath.toString();
        long size = Files.size(filePath);
        LocalDateTime lastModified = LocalDateTime.ofInstant(
            Files.getLastModifiedTime(filePath).toInstant(), 
            ZoneId.systemDefault()
        );
        
        // Extract text content from PDF
        String content = pdfParsingService.extractTextFromPDF(filePath);
        
        log.info(String.format("[FilesystemScannerService] PDF: %s | Size: %d bytes | Content length: %d", 
                filename, size, content.length()));
        
        return new FileResult(filename, filepath, content, size, lastModified, "application/pdf");
    }
}