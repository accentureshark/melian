package org.shark.melian.test;

import org.shark.melian.model.ChunkDto;
import org.shark.melian.model.FileResult;
import org.shark.melian.model.MovieResult;
import org.shark.melian.service.MovieToolService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication(scanBasePackages = "org.shark.melian")
public class MovieToolTestApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MovieToolTestApp.class, args);
        
        MovieToolService movieToolService = context.getBean(MovieToolService.class);
        
        System.out.println("=== Testing filesystem directory scanning ===");
        List<MovieResult> files = movieToolService.scanFilesystemDirectory(null, 10);
        System.out.println("Found " + files.size() + " files:");
        files.forEach(file -> System.out.println("  - " + file.title() + " (" + file.rating() + " bytes)"));
        
        System.out.println("\n=== Testing PDF file scanning ===");
        List<FileResult> pdfFiles = movieToolService.scanPDFFiles(null, 10);
        System.out.println("Found " + pdfFiles.size() + " PDF files:");
        pdfFiles.forEach(file -> {
            System.out.println("  - " + file.filename() + " (" + file.size() + " bytes)");
            System.out.println("    Content preview: " + file.content().substring(0, Math.min(100, file.content().length())) + "...");
        });
        
        System.out.println("\n=== Testing PDF file scanning with filtering ===");
        List<FileResult> matrixFiles = movieToolService.scanPDFFiles("matrix", 10);
        System.out.println("Found " + matrixFiles.size() + " files matching 'matrix':");
        matrixFiles.forEach(file -> System.out.println("  - " + file.filename()));
        
        System.out.println("\n=== Testing scan and store functionality ===");
        List<FileResult> storedFiles = movieToolService.scanAndStorePDFFiles(null, 10);
        System.out.println("Stored " + storedFiles.size() + " PDF files as chunks");
        
        System.out.println("\n=== Testing stored file retrieval ===");
        List<ChunkDto> chunks = movieToolService.getStoredPDFFiles(null, 10, null);
        System.out.println("Retrieved " + chunks.size() + " chunks:");
        chunks.forEach(chunk -> {
            System.out.println("  - " + chunk.getId());
            System.out.println("    Text preview: " + chunk.getText().substring(0, Math.min(100, chunk.getText().length())) + "...");
        });
        
        context.close();
    }
}