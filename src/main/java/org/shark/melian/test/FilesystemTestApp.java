package org.shark.melian.test;

import org.shark.melian.model.FileResult;
import org.shark.melian.service.FilesystemScannerService;
import org.shark.melian.service.PDFParsingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication(scanBasePackages = "org.shark.melian")
public class FilesystemTestApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(FilesystemTestApp.class, args);
        
        FilesystemScannerService scanner = context.getBean(FilesystemScannerService.class);
        
        System.out.println("=== Testing filesystem scanning ===");
        List<FileResult> allFiles = scanner.scanDirectory(null, 10);
        System.out.println("Found " + allFiles.size() + " files:");
        allFiles.forEach(file -> System.out.println("  - " + file.filename() + " (" + file.size() + " bytes)"));
        
        System.out.println("\n=== Testing PDF scanning ===");
        List<FileResult> pdfFiles = scanner.scanForPDFs(null, 10);
        System.out.println("Found " + pdfFiles.size() + " PDF files:");
        pdfFiles.forEach(file -> {
            System.out.println("  - " + file.filename() + " (" + file.size() + " bytes)");
            System.out.println("    Content preview: " + file.content().substring(0, Math.min(100, file.content().length())) + "...");
        });
        
        System.out.println("\n=== Testing PDF filtering ===");
        List<FileResult> matrixFiles = scanner.scanForPDFs("matrix", 10);
        System.out.println("Found " + matrixFiles.size() + " files matching 'matrix':");
        matrixFiles.forEach(file -> System.out.println("  - " + file.filename()));
        
        context.close();
    }
}